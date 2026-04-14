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

  /// Initialize BLE: request permissions, turn on Bluetooth, and start scanning
  Future<void> start() async {
    try {
      debugPrint('[BLE] Initializing BLE Transport...');
      
      // Check current Bluetooth state
      final adapterState = await FlutterBluePlus.adapterState.first;
      debugPrint('[BLE] Current Bluetooth state: $adapterState');
      
      // Request all required BLE permissions
      final permissions = [
        Permission.bluetoothScan,
        Permission.bluetoothConnect,
        Permission.bluetoothAdvertise,
        Permission.locationWhenInUse,
      ];

      debugPrint('[BLE] Requesting ${permissions.length} permissions...');
      for (final perm in permissions) {
        final status = await perm.request();
        debugPrint('[BLE] Permission $perm: ${status.isGranted ? '✓' : '✗'}');
        if (!status.isGranted) {
          debugPrint('[BLE] WARNING: Permission denied: $perm');
        }
      }

      // CRITICAL: Turn on Bluetooth
      debugPrint('[BLE] Turning on Bluetooth...');
      await FlutterBluePlus.turnOn();
      debugPrint('[BLE] ✓ Bluetooth enabled');
      
      // Verify Bluetooth is now on
      final newState = await FlutterBluePlus.adapterState.first;
      debugPrint('[BLE] Bluetooth state after turnOn: $newState');

      // Begin initial scan
      await _startScan();

      // Re-scan every 25 seconds to discover new devices and detect disconnections
      _scanTimer = Timer.periodic(const Duration(seconds: 25), (_) {
        debugPrint('[BLE] Periodic re-scan (every 25s)');
        _startScan();
      });

      debugPrint('[BLE] BLE Transport started successfully');
    } catch (e) {
      debugPrint('[BLE] ERROR on start: $e');
    }
  }

  /// Scan for BLE devices (no service filter - services aren't advertised in scan)
  Future<void> _startScan() async {
    try {
      debugPrint('[BLE] Starting BLE device scan...');
      // Stop any existing scan
      await FlutterBluePlus.stopScan();

      // IMPORTANT: Do NOT filter by service UUID in scan!
      // Services are only discoverable AFTER connection, not in advertisements.
      // We'll verify the service exists after connecting.
      await FlutterBluePlus.startScan(
        timeout: const Duration(seconds: 20),
      );

      // Listen to scan results and auto-connect
      FlutterBluePlus.scanResults.listen((results) {
        if (results.isNotEmpty) {
          debugPrint('[BLE] Scan found ${results.length} device(s)');
          for (final result in results) {
            final rssi = result.rssi;
            debugPrint(
              '[BLE]   - Found: ${result.device.remoteId} | "${result.device.platformName}" | RSSI: $rssi',
            );
            _connectIfNeeded(result.device);
          }
        }
      });

      debugPrint('[BLE] ✓ Scan initiated (20s timeout)');
    } catch (e) {
      debugPrint('[BLE] ERROR during scan: $e');
    }
  }

  /// Connect to a device if not already connected
  Future<void> _connectIfNeeded(BluetoothDevice device) async {
    try {
      final deviceId = device.remoteId.str;
      debugPrint('[BLE] Checking device: $deviceId');

      // Check if already connected
      if (_peers.containsKey(deviceId)) {
        debugPrint('[BLE]   Already connected to $deviceId');
        return;
      }

      // Attempt connection
      debugPrint('[BLE]   Connecting to $deviceId...');
      await device.connect(timeout: const Duration(seconds: 8));
      _peers[deviceId] = device;
      debugPrint(
        '[BLE] ✓ Connected to $deviceId (${_peers.length} peer(s) total)',
      );

      // Listen for disconnections
      device.connectionState.listen((state) {
        debugPrint('[BLE] Connection state change: $deviceId → $state');
        if (state == BluetoothConnectionState.disconnected) {
          _peers.remove(deviceId);
          _chunkBuffers.remove(deviceId);
          debugPrint(
            '[BLE] ✗ Disconnected from $deviceId (${_peers.length} peer(s) remaining)',
          );
        }
      });

      // Subscribe to incoming data
      await _subscribe(device);
    } catch (e) {
      debugPrint('[BLE] ERROR connecting to ${device.remoteId}: $e');
    }
  }

  /// Subscribe to characteristic notifications on a device
  Future<void> _subscribe(BluetoothDevice device) async {
    try {
      final deviceId = device.remoteId.str;
      debugPrint('[BLE] Discovering services on $deviceId...');
      // Discover services
      final services = await device.discoverServices();
      debugPrint('[BLE] Found ${services.length} service(s)');

      // Find our service
      BluetoothService? targetService;
      for (final service in services) {
        debugPrint('[BLE]   Service: ${service.uuid}');
        if (service.uuid.str.toLowerCase() == kServiceUuid.toLowerCase()) {
          targetService = service;
          debugPrint('[BLE]   ✓ Found our service!');
          break;
        }
      }

      if (targetService == null) {
        debugPrint('[BLE] ERROR: Service $kServiceUuid not found on $deviceId');
        return;
      }

      // Find our characteristic
      BluetoothCharacteristic? targetChar;
      for (final char in targetService.characteristics) {
        debugPrint('[BLE]   Char: ${char.uuid}');
        if (char.uuid.str.toLowerCase() == kCharUuid.toLowerCase()) {
          targetChar = char;
          debugPrint('[BLE]   ✓ Found our characteristic!');
          break;
        }
      }

      if (targetChar == null) {
        debugPrint('[BLE] ERROR: Characteristic $kCharUuid not found');
        return;
      }

      // Enable notifications
      debugPrint('[BLE] Enabling notifications...');
      await targetChar.setNotifyValue(true);
      debugPrint('[BLE] ✓ Notifications enabled on $deviceId');

      // Listen for incoming data
      targetChar.onValueReceived.listen(
        (bytes) => _handleBytes(deviceId, bytes),
        onError: (e) => debugPrint('[BLE] ERROR listening on $deviceId: $e'),
      );

      debugPrint('[BLE] ✓ Successfully subscribed to $deviceId');
    } catch (e) {
      debugPrint('[BLE] ERROR during subscribe: $e');
    }
  }

  /// Handle incoming bytes from a peer
  /// Reconstructs multi-chunk packets and emits complete packets
  void _handleBytes(String deviceId, List<int> bytes) {
    try {
      debugPrint('[BLE] Received ${bytes.length} bytes from $deviceId');
      // Packets must have at least chunk header (2 bytes)
      if (bytes.length < 2) {
        debugPrint('[BLE] ERROR: Packet too short from $deviceId');
        return;
      }

      final chunkIndex = bytes[0];
      final totalChunks = bytes[1];
      final data = bytes.sublist(2);
      debugPrint(
        '[BLE] Chunk $chunkIndex of $totalChunks, data: ${data.length} bytes',
      );

      // Single-chunk packet: emit immediately
      if (totalChunks == 1) {
        try {
          final packet = Packet.fromBytes(data);
          _controller.add(packet);
          debugPrint('[BLE] ✓ Emitted single-chunk ${packet.type} packet');
        } catch (e) {
          debugPrint('[BLE] ERROR parsing single-chunk packet: $e');
        }
        return;
      }

      // Multi-chunk packet: buffer until all chunks received
      _chunkBuffers[deviceId] ??= List.filled(totalChunks, []);
      final buffer = _chunkBuffers[deviceId]!;

      // Validate chunk index
      if (chunkIndex >= totalChunks) {
        debugPrint(
          '[BLE] ERROR: Invalid chunk index $chunkIndex of $totalChunks',
        );
        return;
      }

      // Store chunk
      buffer[chunkIndex] = data;
      debugPrint('[BLE] Buffered chunk $chunkIndex/$totalChunks');

      // Check if all chunks received
      bool allReceived = true;
      int receivedCount = 0;
      for (final chunk in buffer) {
        if (chunk.isNotEmpty) receivedCount++;
        if (chunk.isEmpty) {
          allReceived = false;
        }
      }
      debugPrint('[BLE] Progress: $receivedCount/$totalChunks chunks received');

      if (!allReceived) {
        return; // Wait for more chunks
      }

      // Reassemble and emit
      try {
        debugPrint('[BLE] All chunks received, reassembling...');
        final fullData = buffer.expand((chunk) => chunk).toList();
        final packet = Packet.fromBytes(fullData);
        _controller.add(packet);
        debugPrint(
          '[BLE] ✓ Emitted multi-chunk ${packet.type} packet (${fullData.length} bytes total)',
        );

        // Clear buffer
        _chunkBuffers.remove(deviceId);
      } catch (e) {
        debugPrint('[BLE] ERROR parsing reassembled packet: $e');
        _chunkBuffers.remove(deviceId);
      }
    } catch (e) {
      debugPrint('[BLE] ERROR handling bytes: $e');
    }
  }

  /// Broadcast a packet to all connected peers
  /// Automatically chunks large packets
  /// Excludes the optional originating peer to avoid loops
  Future<void> broadcast(Packet packet, {String? excludeId}) async {
    try {
      final data = packet.toBytes();
      final chunks = _toChunks(data);
      debugPrint(
        '[BLE] Broadcasting ${packet.type} packet (${data.length} bytes, ${chunks.length} chunk(s)) to ${_peers.length} peer(s)',
      );

      for (final entry in _peers.entries) {
        final deviceId = entry.key;
        final device = entry.value;

        // Don't broadcast back to sender
        if (deviceId == excludeId) {
          debugPrint('[BLE]   Skipping excluded peer: $deviceId');
          continue;
        }

        try {
          debugPrint('[BLE]   Sending to $deviceId...');
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
            debugPrint('[BLE]   ERROR: Service not found on $deviceId');
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
            debugPrint('[BLE]   ERROR: Characteristic not found on $deviceId');
            continue;
          }

          // Send each chunk
          int chunkNum = 0;
          for (final chunk in chunks) {
            await targetChar.write(chunk, withoutResponse: false);
            debugPrint(
              '[BLE]   ✓ Sent chunk ${chunkNum + 1}/${chunks.length} to $deviceId',
            );
            chunkNum++;
          }

          debugPrint('[BLE] ✓ Broadcast successful to $deviceId');
        } catch (e) {
          debugPrint('[BLE] ERROR broadcasting to $deviceId: $e');
        }
      }
    } catch (e) {
      debugPrint('[BLE] ERROR during broadcast: $e');
    }
  }

  /// Split a packet into MTU-sized chunks with headers
  /// Each chunk: [chunkIndex, totalChunks, ...data]
  List<List<int>> _toChunks(List<int> data) {
    // Single chunk fits in MTU
    if (data.length <= kMtu) {
      debugPrint(
        '[BLE] Data fits in single chunk (${data.length} <= $kMtu bytes)',
      );
      return [
        [0, 1, ...data],
      ];
    }

    // Multiple chunks needed
    final totalChunks = (data.length / kMtu).ceil();
    debugPrint(
      '[BLE] Chunking ${data.length} bytes into $totalChunks chunks (MTU=$kMtu)',
    );
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
    debugPrint('[BLE] Disposing BLE Transport...');
    _scanTimer?.cancel();
    _controller.close();
    FlutterBluePlus.stopScan();
    debugPrint('[BLE] ✓ BLE Transport disposed');
  }
}
