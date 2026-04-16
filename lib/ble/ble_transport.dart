import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
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
  static const int kMaxConnectionRetries =
      3; // Max reconnect attempts per device
  static const String kPlatformChannel = 'com.example.offgrid/ble_advertising';

  // Internal state
  final String myId; // Our own mesh ID
  final String? deviceName; // Device display name for advertising
  final Map<String, BluetoothDevice> _peers = {};
  final Map<String, String> _peerNames = {}; // Map device ID → display name
  final Map<String, StreamSubscription> _connectionStateSubscriptions =
      {}; // Track connection state listeners
  final Map<String, StreamSubscription> _characteristicSubscriptions =
      {}; // Track characteristic value subscriptions to prevent leaks
  final Map<String, List<List<int>>> _chunkBuffers = {};
  final Set<String> _pendingConnections =
      {}; // Devices currently connecting (race condition protection)
  final Map<String, int> _connectionRetries = {}; // Retry count per device
  final StreamController<Packet> _controller = StreamController.broadcast();
  Timer? _scanTimer;
  StreamSubscription? _scanResultsSubscription; // Track scan results listener
  bool _permissionsGranted = false; // Cache permission status
  late final MethodChannel _platformChannel;

  /// Constructor - requires this device's mesh ID and optional display name
  BleTransport({required this.myId, this.deviceName}) {
    _platformChannel = const MethodChannel(kPlatformChannel);
  }

  /// Public API: Stream of incoming packets
  Stream<Packet> get incomingPackets => _controller.stream;

  /// Public API: List of connected peer device IDs
  List<String> get connectedPeerIds => _peers.keys.toList();

  /// Public API: Get the display name of a peer by its device ID
  /// Returns the peer's display name, or a truncated device ID if name unavailable
  String getPeerName(String deviceId) {
    return _peerNames[deviceId] ?? deviceId.substring(0, 12);
  }

  /// Public API: Local device's BLE address (same as shown in other users' peer lists)
  /// Returns the mesh ID which uniquely identifies this device
  String get localDeviceAddress => myId;

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
      _permissionsGranted = true;
      for (final perm in permissions) {
        final status = await perm.request();
        debugPrint('[BLE] Permission $perm: ${status.isGranted ? '✓' : '✗'}');
        if (!status.isGranted) {
          debugPrint('[BLE] CRITICAL: Permission denied: $perm');
          _permissionsGranted = false;
        }
      }

      if (!_permissionsGranted) {
        debugPrint(
          '[BLE] ERROR: Not all permissions granted. BLE cannot start. Please enable BLE permissions in Settings.',
        );
        return; // Return gracefully instead of throwing — don't crash the app
      }

      // CRITICAL: Turn on Bluetooth
      debugPrint('[BLE] Turning on Bluetooth...');
      await FlutterBluePlus.turnOn();
      debugPrint('[BLE] ✓ Bluetooth enabled');

      // Verify Bluetooth is now on
      final newState = await FlutterBluePlus.adapterState.first;
      debugPrint('[BLE] Bluetooth state after turnOn: $newState');
      debugPrint('[BLE] Local device ID: $myId');

      // Start BLE advertising with device name so other devices can see this device
      if (deviceName != null && deviceName!.isNotEmpty) {
        await _startAdvertising(deviceName!);
      } else {
        debugPrint('[BLE] WARNING: No device name provided for advertising');
      }

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
      // Stop any existing scan and cancel previous listener
      await FlutterBluePlus.stopScan();
      await _scanResultsSubscription?.cancel();

      // IMPORTANT: Do NOT filter by service UUID in scan!
      // Services are only discoverable AFTER connection, not in advertisements.
      // We'll verify the service exists after connecting.
      await FlutterBluePlus.startScan(timeout: const Duration(seconds: 20));

      // Listen to scan results and auto-connect
      // IMPORTANT: Store subscription so we can cancel it later
      _scanResultsSubscription = FlutterBluePlus.scanResults.listen((results) {
        if (results.isNotEmpty) {
          debugPrint('[BLE] Scan found ${results.length} device(s)');
          for (final result in results) {
            final rssi = result.rssi;
            final deviceName = result.device.platformName;
            final hasName = deviceName.isNotEmpty;

            // Enhanced logging: show clearly if device has a name or not
            if (hasName) {
              debugPrint(
                '[BLE]   ✓ ADVERTISING: ${result.device.remoteId} | "$deviceName" | RSSI: $rssi',
              );
            } else {
              debugPrint(
                '[BLE]   ○ NO NAME: ${result.device.remoteId} | (empty) | RSSI: $rssi',
              );
            }

            // Try to connect to ALL devices discovered.
            // We validate that they have our service UUID after connecting.
            // This is more robust than filtering by name (which may not be advertised).
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
  /// Uses exponential backoff for retries and prevents race conditions
  Future<void> _connectIfNeeded(BluetoothDevice device) async {
    try {
      final deviceId = device.remoteId.str;
      debugPrint('[BLE] Checking device: $deviceId');

      // Check if already connected
      if (_peers.containsKey(deviceId)) {
        debugPrint('[BLE]   Already connected to $deviceId');
        return;
      }

      // CRITICAL: Prevent race condition — don't connect if already connecting
      if (_pendingConnections.contains(deviceId)) {
        debugPrint('[BLE]   Already connecting to $deviceId (pending)...');
        return;
      }

      // Check retry count with exponential backoff
      final retries = _connectionRetries[deviceId] ?? 0;
      if (retries >= kMaxConnectionRetries) {
        debugPrint('[BLE]   Max retries reached for $deviceId');
        return;
      }

      // Mark as pending
      _pendingConnections.add(deviceId);

      // Capture device name from BLE advertisement
      final deviceName = device.platformName.isNotEmpty
          ? device.platformName
          : 'Device ${deviceId.substring(0, 8)}';
      _peerNames[deviceId] = deviceName;

      // Attempt connection
      debugPrint(
        '[BLE]   Connecting to $deviceId "$deviceName" (attempt ${retries + 1}/$kMaxConnectionRetries)...',
      );
      await device.connect(timeout: const Duration(seconds: 8));

      // Success: remove from pending and reset retries
      _pendingConnections.remove(deviceId);
      _connectionRetries.remove(deviceId);
      _peers[deviceId] = device;
      debugPrint(
        '[BLE] ✓ Connected to $deviceId (${_peers.length} peer(s) total)',
      );

      // Listen for disconnections and store subscription for cleanup
      // CRITICAL: Cancel any existing subscription first to prevent memory leaks
      _connectionStateSubscriptions[deviceId]?.cancel();

      final connectionSub = device.connectionState.listen((state) {
        debugPrint('[BLE] Connection state change: $deviceId → $state');
        if (state == BluetoothConnectionState.disconnected) {
          _peers.remove(deviceId);
          _chunkBuffers.remove(deviceId);
          _peerNames.remove(deviceId);
          // Cancel and remove the connection state subscription
          _connectionStateSubscriptions[deviceId]?.cancel();
          _connectionStateSubscriptions.remove(deviceId);
          debugPrint(
            '[BLE] ✗ Disconnected from $deviceId (${_peers.length} peer(s) remaining)',
          );
        }
      });
      _connectionStateSubscriptions[deviceId] = connectionSub;

      // Subscribe to incoming data
      await _subscribe(device);
    } catch (e) {
      final deviceId = device.remoteId.str;
      _pendingConnections.remove(deviceId);
      // Track retry count
      _connectionRetries[deviceId] = (_connectionRetries[deviceId] ?? 0) + 1;
      debugPrint('[BLE] ERROR connecting to $deviceId: $e');
    }
  }

  /// Subscribe to characteristic notifications on a device
  Future<void> _subscribe(BluetoothDevice device) async {
    try {
      final deviceId = device.remoteId.str;
      final deviceName = device.platformName.isNotEmpty
          ? device.platformName
          : 'Unknown';
      debugPrint('[BLE] Discovering services on $deviceId ("$deviceName")...');
      // Discover services
      final services = await device.discoverServices();
      debugPrint('[BLE] Found ${services.length} service(s)');

      // Find our service
      BluetoothService? targetService;
      for (final service in services) {
        debugPrint('[BLE]   Service: ${service.uuid}');
        if (service.uuid.str.toLowerCase() == kServiceUuid.toLowerCase()) {
          targetService = service;
          debugPrint('[BLE]   ✓✓✓ FOUND OFFGRID SERVICE ✓✓✓');
          break;
        }
      }

      if (targetService == null) {
        debugPrint('[BLE] ✗ Service NOT found on $deviceId ("$deviceName")');
        debugPrint(
          '[BLE] → This device is not running OffGrid (or service not exposed)',
        );
        // Disconnect immediately from devices that don't have our service
        await device.disconnect();
        _peers.remove(deviceId);
        _chunkBuffers.remove(deviceId);
        _peerNames.remove(deviceId);
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
        debugPrint(
          '[BLE] DISCONNECTING: $deviceId missing required characteristic',
        );
        // Disconnect if characteristic not found
        await device.disconnect();
        _peers.remove(deviceId);
        _chunkBuffers.remove(deviceId);
        _peerNames.remove(deviceId);
        return;
      }

      // Enable notifications
      debugPrint('[BLE] Enabling notifications...');
      await targetChar.setNotifyValue(true);
      debugPrint('[BLE] ✓ Notifications enabled on $deviceId');

      // CRITICAL: Store subscription to prevent memory leaks
      // Cancel old subscription if it exists (shouldn't, but safety first)
      _characteristicSubscriptions[deviceId]?.cancel();

      // Listen for incoming data
      final subscription = targetChar.onValueReceived.listen(
        (bytes) => _handleBytes(deviceId, bytes),
        onError: (e) => debugPrint('[BLE] ERROR listening on $deviceId: $e'),
      );
      _characteristicSubscriptions[deviceId] = subscription;

      debugPrint('[BLE] ═══════════════════════════════════════════');
      debugPrint('[BLE] ✓✓✓ CONNECTED TO OFFGRID PEER ✓✓✓');
      debugPrint('[BLE] Peer: ${_peerNames[deviceId] ?? "Unknown"}');
      debugPrint('[BLE] ID: $deviceId');
      debugPrint('[BLE] Total peers: ${_peers.length}');
      debugPrint('[BLE] ═══════════════════════════════════════════');
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
  /// CRITICAL: Always excludes self (myId) to prevent packet loops
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

        // CRITICAL: Never broadcast to self — prevents infinite loops
        if (deviceId == myId) {
          debugPrint('[BLE]   Skipping self (myId): $deviceId');
          continue;
        }

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
  /// Each chunk format: [chunkIndex, totalChunks, ...data]
  /// CRITICAL: Header is 2 bytes, so max data per chunk is kMtu - 2
  List<List<int>> _toChunks(List<int> data) {
    const maxDataPerChunk = kMtu - 2; // Account for chunk header

    // Single chunk fits in MTU
    if (data.length <= maxDataPerChunk) {
      debugPrint(
        '[BLE] Data fits in single chunk (${data.length} <= $maxDataPerChunk bytes)',
      );
      return [
        [0, 1, ...data],
      ];
    }

    // Multiple chunks needed
    final totalChunks = (data.length / maxDataPerChunk).ceil();
    debugPrint(
      '[BLE] Chunking ${data.length} bytes into $totalChunks chunks (MTU=$kMtu, header=2)',
    );
    final chunks = <List<int>>[];

    for (int i = 0; i < totalChunks; i++) {
      final start = i * maxDataPerChunk;
      final end = (start + maxDataPerChunk).clamp(0, data.length);
      final chunkData = data.sublist(start, end);
      chunks.add([i, totalChunks, ...chunkData]);
    }

    return chunks;
  }

  /// Try to get the local device's BLE address
  /// Returns the hardware MAC address if available through BLE adapter
  /// Emit a test packet directly (for single-device loopback testing)
  /// This simulates receiving your own packet so you can verify packet serialization
  void emitTestPacket(String displayName) {
    try {
      final packet = Packet.discovery(from: myId, displayName: displayName);
      _controller.add(packet);
      debugPrint('[BLE] ✓ Emitted loopback test packet');
    } catch (e) {
      debugPrint('[BLE] ERROR emitting test packet: $e');
    }
  }

  /// Cleanup and shutdown BLE
  void dispose() {
    debugPrint('[BLE] Disposing BLE Transport...');
    // Cancel all connection state subscriptions
    for (final sub in _connectionStateSubscriptions.values) {
      sub.cancel();
    }
    _connectionStateSubscriptions.clear();
    // Cancel all characteristic subscriptions (fixes memory leaks)
    for (final sub in _characteristicSubscriptions.values) {
      sub.cancel();
    }
    _characteristicSubscriptions.clear();
    // Cancel scan results subscription
    _scanResultsSubscription?.cancel();
    // Cancel periodic scan timer
    _scanTimer?.cancel();
    // Close packet stream
    _controller.close();
    // Stop BLE scanning
    FlutterBluePlus.stopScan();
    debugPrint('[BLE] ✓ BLE Transport disposed');
  }

  /// Start BLE advertising on native side with the device name
  /// This makes this device visible to other devices with the user's display name
  Future<void> _startAdvertising(String name) async {
    try {
      debugPrint('[BLE] ═══════════════════════════════════════════');
      debugPrint('[BLE] Starting BLE advertising with name: "$name"');
      debugPrint('[BLE] ═══════════════════════════════════════════');

      final result = await _platformChannel.invokeMethod<String>(
        'startAdvertising',
        {'deviceName': name},
      );

      debugPrint('[BLE] ✓✓✓ ADVERTISING ACTIVE ✓✓✓');
      debugPrint('[BLE] Result: $result');
      debugPrint('[BLE] Device is now visible as: "$name"');
      debugPrint('[BLE] Other devices should see this name in scan results');
      debugPrint('[BLE] ═══════════════════════════════════════════');
    } catch (e) {
      debugPrint('[BLE] ✗✗✗ ADVERTISING FAILED ✗✗✗');
      debugPrint('[BLE] Error: $e');
      debugPrint('[BLE] Device name will NOT be advertised');
      debugPrint('[BLE] ═══════════════════════════════════════════');
    }
  }
}
