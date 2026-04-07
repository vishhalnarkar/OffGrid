import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:permission_handler/permission_handler.dart';

import '../mesh/packet.dart';

/// BLE Transport Layer for OffGrid mesh communication
/// Handles:
/// - Scanning for nearby BLE devices
/// - Establishing connections
/// - Sending and receiving packets with automatic chunking
/// - Reassembling multi-chunk packets
class BleTransport {
  // BLE Service and Characteristic UUIDs (Nordic UART Service)
  static const String kServiceUuid = '6E400001-B5A3-F393-E0A9-E50E24DCCA9E';
  static const String kCharUuid = '6E400002-B5A3-F393-E0A9-E50E24DCCA9E';
  static const int kMtu = 180; // Maximum usable bytes per BLE packet

  // Internal state
  final Map<String, BluetoothDevice> _peers = {};
  final Map<String, List<List<int>>> _chunkBuffers = {};
  final StreamController<Packet> _controller = StreamController.broadcast();
  Timer? _scanTimer;

  /// Public API: Stream of incoming packets
  Stream<Packet> get incomingPackets => _controller.stream;

  /// Public API: List of connected peer device IDs
  List<String> get connectedPeerIds => _peers.keys.toList();

  /// Initialize BLE: request permissions and start scanning
  Future<void> start() async {
    try {
      // Request all required BLE permissions
      final permissions = [
        Permission.bluetoothScan,
        Permission.bluetoothConnect,
        Permission.bluetoothAdvertise,
        Permission.locationWhenInUse,
      ];

      for (final perm in permissions) {
        final status = await perm.request();
        if (!status.isGranted) {
          debugPrint('Permission denied: $perm');
        }
      }

      // Begin initial scan
      await _startScan();

      // Re-scan every 25 seconds to discover new devices and detect disconnections
      _scanTimer = Timer.periodic(
        const Duration(seconds: 25),
        (_) => _startScan(),
      );

      debugPrint('BLE Transport started successfully');
    } catch (e) {
      debugPrint('BLE start error: $e');
    }
  }

  /// Scan for BLE devices with our service UUID
  Future<void> _startScan() async {
    try {
      // Stop any existing scan
      await FlutterBluePlus.stopScan();

      // Start new scan for our service
      await FlutterBluePlus.startScan(
        withServices: [Guid(kServiceUuid)],
        timeout: const Duration(seconds: 20),
      );

      // Listen to scan results and auto-connect
      FlutterBluePlus.scanResults.listen((results) {
        for (final result in results) {
          _connectIfNeeded(result.device);
        }
      });

      debugPrint('BLE scan started');
    } catch (e) {
      debugPrint('BLE scan error: $e');
    }
  }

  /// Connect to a device if not already connected
  Future<void> _connectIfNeeded(BluetoothDevice device) async {
    try {
      final deviceId = device.remoteId.str;

      // Check if already connected
      if (_peers.containsKey(deviceId)) {
        return;
      }

      // Attempt connection
      await device.connect(timeout: const Duration(seconds: 8));
      _peers[deviceId] = device;

      debugPrint('Connected to $deviceId');

      // Listen for disconnections
      device.connectionState.listen((state) {
        if (state == BluetoothConnectionState.disconnected) {
          _peers.remove(deviceId);
          _chunkBuffers.remove(deviceId);
          debugPrint('Disconnected from $deviceId');
        }
      });

      // Subscribe to incoming data
      await _subscribe(device);
    } catch (e) {
      debugPrint('BLE connect failed for ${device.remoteId}: $e');
    }
  }

  /// Subscribe to characteristic notifications on a device
  Future<void> _subscribe(BluetoothDevice device) async {
    try {
      // Discover services
      final services = await device.discoverServices();

      // Find our service
      BluetoothService? targetService;
      for (final service in services) {
        if (service.uuid.str.toLowerCase() == kServiceUuid.toLowerCase()) {
          targetService = service;
          break;
        }
      }

      if (targetService == null) {
        debugPrint('Service $kServiceUuid not found on ${device.remoteId}');
        return;
      }

      // Find our characteristic
      BluetoothCharacteristic? targetChar;
      for (final char in targetService.characteristics) {
        if (char.uuid.str.toLowerCase() == kCharUuid.toLowerCase()) {
          targetChar = char;
          break;
        }
      }

      if (targetChar == null) {
        debugPrint('Characteristic $kCharUuid not found');
        return;
      }

      // Enable notifications
      await targetChar.setNotifyValue(true);

      // Listen for incoming data
      targetChar.onValueReceived.listen(
        (bytes) => _handleBytes(device.remoteId.str, bytes),
        onError: (e) => debugPrint('Characteristic listen error: $e'),
      );

      debugPrint('Subscribed to ${device.remoteId}');
    } catch (e) {
      debugPrint('BLE subscribe error: $e');
    }
  }

  /// Handle incoming bytes from a peer
  /// Reconstructs multi-chunk packets and emits complete packets
  void _handleBytes(String deviceId, List<int> bytes) {
    try {
      // Packets must have at least chunk header (2 bytes)
      if (bytes.length < 2) {
        debugPrint('Packet too short from $deviceId');
        return;
      }

      final chunkIndex = bytes[0];
      final totalChunks = bytes[1];
      final data = bytes.sublist(2);

      // Single-chunk packet: emit immediately
      if (totalChunks == 1) {
        try {
          final packet = Packet.fromBytes(data);
          _controller.add(packet);
          debugPrint('Received packet: ${packet.type} from ${packet.from}');
        } catch (e) {
          debugPrint('Failed to parse single-chunk packet: $e');
        }
        return;
      }

      // Multi-chunk packet: buffer until all chunks received
      _chunkBuffers[deviceId] ??= List.filled(totalChunks, []);
      final buffer = _chunkBuffers[deviceId]!;

      // Validate chunk index
      if (chunkIndex >= totalChunks) {
        debugPrint('Invalid chunk index $chunkIndex of $totalChunks');
        return;
      }

      // Store chunk
      buffer[chunkIndex] = data;

      // Check if all chunks received
      bool allReceived = true;
      for (final chunk in buffer) {
        if (chunk.isEmpty) {
          allReceived = false;
          break;
        }
      }

      if (!allReceived) {
        return; // Wait for more chunks
      }

      // Reassemble and emit
      try {
        final fullData = buffer.expand((chunk) => chunk).toList();
        final packet = Packet.fromBytes(fullData);
        _controller.add(packet);
        debugPrint(
          'Received reassembled packet: ${packet.type} from ${packet.from}',
        );

        // Clear buffer
        _chunkBuffers.remove(deviceId);
      } catch (e) {
        debugPrint('Failed to parse reassembled packet: $e');
        _chunkBuffers.remove(deviceId);
      }
    } catch (e) {
      debugPrint('BLE handle bytes error: $e');
    }
  }

  /// Broadcast a packet to all connected peers
  /// Automatically chunks large packets
  /// Excludes the optional originating peer to avoid loops
  Future<void> broadcast(Packet packet, {String? excludeId}) async {
    try {
      final data = packet.toBytes();
      final chunks = _toChunks(data);

      for (final entry in _peers.entries) {
        final deviceId = entry.key;
        final device = entry.value;

        // Don't broadcast back to sender
        if (deviceId == excludeId) {
          continue;
        }

        try {
          // Discover services on this device
          final services = await device.discoverServices();

          // Find service and characteristic
          BluetoothService? targetService;
          for (final service in services) {
            if (service.uuid.str.toLowerCase() == kServiceUuid.toLowerCase()) {
              targetService = service;
              break;
            }
          }

          if (targetService == null) {
            continue;
          }

          BluetoothCharacteristic? targetChar;
          for (final char in targetService.characteristics) {
            if (char.uuid.str.toLowerCase() == kCharUuid.toLowerCase()) {
              targetChar = char;
              break;
            }
          }

          if (targetChar == null) {
            continue;
          }

          // Send each chunk
          for (final chunk in chunks) {
            await targetChar.write(chunk, withoutResponse: false);
          }

          debugPrint('Broadcasted ${chunks.length} chunk(s) to $deviceId');
        } catch (e) {
          debugPrint('Broadcast to $deviceId failed: $e');
        }
      }
    } catch (e) {
      debugPrint('Broadcast error: $e');
    }
  }

  /// Split a packet into MTU-sized chunks with headers
  /// Each chunk: [chunkIndex, totalChunks, ...data]
  List<List<int>> _toChunks(List<int> data) {
    // Single chunk fits in MTU
    if (data.length <= kMtu) {
      return [
        [0, 1, ...data],
      ];
    }

    // Multiple chunks needed
    final totalChunks = (data.length / kMtu).ceil();
    final chunks = <List<int>>[];

    for (int i = 0; i < totalChunks; i++) {
      final start = i * kMtu;
      final end = (start + kMtu).clamp(0, data.length);
      final chunkData = data.sublist(start, end);
      chunks.add([i, totalChunks, ...chunkData]);
    }

    return chunks;
  }

  /// Cleanup and shutdown BLE
  void dispose() {
    _scanTimer?.cancel();
    _controller.close();
    FlutterBluePlus.stopScan();
    debugPrint('BLE Transport disposed');
  }
}
