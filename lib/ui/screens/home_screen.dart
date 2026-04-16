import 'dart:async';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../ble/ble_transport.dart';
import '../../mesh/packet.dart';

/// Home screen shown after onboarding.
/// Displays welcome message with user identity and Phase 3 BLE testing UI.
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('OffGrid'), centerTitle: true),
      body: FutureBuilder<SharedPreferences>(
        future: SharedPreferences.getInstance(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snapshot.hasError) {
            return Center(child: Text('Error: ${snapshot.error}'));
          }

          final prefs = snapshot.data;
          if (prefs == null) {
            return const Center(child: Text('Failed to load preferences'));
          }

          final displayName = prefs.getString('displayName') ?? 'Unknown';
          final myId = prefs.getString('myId') ?? '';

          // DEBUG: Print myId info every time HomeScreen opens
          debugPrint('=== OffGrid HomeScreen Opened ===');
          debugPrint('Full myId: $myId');
          debugPrint('myId length: ${myId.length}');

          // Show first 8 characters of myId, followed by ellipsis
          final displayId = myId.length > 8
              ? '${myId.substring(0, 8)}...'
              : myId;

          return SingleChildScrollView(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  // Welcome message (static)
                  Text(
                    'Welcome, $displayName',
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),

                  // User ID display (static)
                  Text('ID: $displayId', style: const TextStyle(fontSize: 14)),
                  const SizedBox(height: 48),

                  // === PHASE 3: BLE TESTING UI (separate widget for efficient updates) ===
                  const _BleTestWidget(),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

/// BLE testing widget - separate StatefulWidget so only this rebuilds during updates
/// This avoids rebuilding the entire screen when packet log or peer count changes
class _BleTestWidget extends StatefulWidget {
  const _BleTestWidget();

  @override
  State<_BleTestWidget> createState() => _BleTestWidgetState();
}

class _BleTestWidgetState extends State<_BleTestWidget> {
  late BleTransport _ble;
  final List<String> _packetLog = [];
  int _connectedPeersCount = 0;
  List<String> _peersList = [];
  String? _selectedPeerId;
  bool _bleStarted = false;
  StreamSubscription? _packetSubscription;
  Timer? _statsTimer;
  String _myId = ''; // Mesh ID (SHA-256 hash for internal use)
  String _deviceName =
      ''; // Bluetooth device name (user's display name from onboarding)
  bool _bleInitialized = false; // Track when BleTransport is ready

  @override
  void initState() {
    super.initState();
    // CRITICAL: Do NOT create BleTransport here - do it ONLY in _initializeBle()
    // after myId is loaded. Creating it twice breaks the stream controller.
    _initializeBle();
  }

  Future<void> _initializeBle() async {
    final prefs = await SharedPreferences.getInstance();
    final myId = prefs.getString('myId') ?? '';
    final deviceName = prefs.getString('displayName') ?? 'Unknown Device';
    if (!mounted) return; // Safety check: widget might be disposed

    setState(() {
      _myId = myId;
      _deviceName =
          deviceName; // Use user's display name as Bluetooth device name
      // Create BleTransport ONLY ONCE with the real myId and device name
      // CRITICAL: Do NOT recreate it - that breaks the stream controller listener!
      if (!_bleInitialized) {
        _ble = BleTransport(myId: _myId, deviceName: _deviceName);
        _bleInitialized = true;
      }
    });
  }

  @override
  void dispose() {
    _packetSubscription?.cancel();
    _statsTimer?.cancel();
    _ble.dispose();
    super.dispose();
  }

  Future<void> _startBle() async {
    if (_bleStarted) return;

    setState(() => _bleStarted = true);

    // Start BLE transport
    await _ble.start();

    // Listen to incoming packets
    _packetSubscription = _ble.incomingPackets.listen((packet) {
      setState(() {
        _packetLog.insert(
          0,
          '${DateTime.now().toIso8601String().split('T')[1].split('.')[0]} | '
          '${packet.type} from ${packet.from.substring(0, 8)}...',
        );
        // Keep log size manageable
        if (_packetLog.length > 20) {
          _packetLog.removeLast();
        }
      });
      debugPrint('Got packet: ${packet.type} from ${packet.from}');
    });

    // Update connected peers list every 3 seconds
    _statsTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      setState(() {
        _peersList = _ble.connectedPeerIds;
        _connectedPeersCount = _peersList.length;
        // If selected peer is no longer connected, deselect it
        if (_selectedPeerId != null && !_peersList.contains(_selectedPeerId)) {
          _selectedPeerId = null;
        }
      });
      debugPrint('Connected peers: $_connectedPeersCount');
    });

    debugPrint('BLE started');
  }

  Future<void> _sendTestPacket() async {
    if (_selectedPeerId == null) {
      // No peer selected: emit loopback test packet
      debugPrint('No peer selected - using loopback test');
      _ble.emitTestPacket('My Device (loopback)');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Sent loopback test packet (single device)'),
          ),
        );
      }
      return;
    }

    final packet = Packet.discovery(from: _myId, displayName: 'Test Device');

    debugPrint('Sending discovery packet to peer: $_selectedPeerId');
    await _ble.broadcast(packet, excludeId: null);
    // Note: broadcast sends to all peers. In Phase 4 we'll add direct messaging.

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Sent packet to $_selectedPeerId')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    // Show loading state until BleTransport is initialized
    if (!_bleInitialized) {
      return const Center(child: CircularProgressIndicator());
    }

    return Column(
      children: [
        const Text(
          'Phase 3: BLE Transport',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 16),

        // Status display
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'BLE Status: ${_bleStarted ? '🟢 Running' : '🔴 Stopped'}',
                style: const TextStyle(fontSize: 14),
              ),
              const SizedBox(height: 8),
              Text(
                'Your Device: $_deviceName',
                style: const TextStyle(fontSize: 14),
              ),
              const SizedBox(height: 8),
              Text(
                'Connected Peers: $_connectedPeersCount',
                style: const TextStyle(fontSize: 14),
              ),
              const SizedBox(height: 8),
              Text(
                'Packets Received: ${_packetLog.length}',
                style: const TextStyle(fontSize: 14),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // Connected Peers List
        const Text(
          'Connected Peers',
          style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Container(
          height: 120,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey),
            borderRadius: BorderRadius.circular(8),
          ),
          child: _peersList.isEmpty
              ? const Center(
                  child: Text(
                    'No peers connected',
                    style: TextStyle(color: Colors.grey, fontSize: 12),
                  ),
                )
              : ListView.builder(
                  itemCount: _peersList.length,
                  itemBuilder: (context, index) {
                    final peerId = _peersList[index];
                    final isSelected = peerId == _selectedPeerId;
                    // Get the peer's display name from BleTransport
                    final peerName = _ble.getPeerName(peerId);

                    return GestureDetector(
                      onTap: () {
                        setState(() {
                          _selectedPeerId = isSelected ? null : peerId;
                        });
                      },
                      child: Container(
                        color: isSelected
                            ? Colors.blue.withValues(alpha: 0.3)
                            : null,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12.0,
                          vertical: 8.0,
                        ),
                        child: Row(
                          children: [
                            Text(
                              isSelected ? '✓' : '○',
                              style: TextStyle(
                                fontSize: 16,
                                color: isSelected ? Colors.blue : Colors.grey,
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Text(
                                    peerName,
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w500,
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                  Text(
                                    peerId.substring(0, 12),
                                    style: const TextStyle(
                                      fontSize: 10,
                                      color: Colors.grey,
                                      fontFamily: 'monospace',
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
        ),
        const SizedBox(height: 16),

        // Selected Peer Display
        if (_selectedPeerId != null)
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.blue.withValues(alpha: 0.1),
              border: Border.all(color: Colors.blue),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Selected Peer:',
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
                const SizedBox(height: 4),
                Text(
                  _ble.getPeerName(_selectedPeerId!),
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Text(
                  'ID: ${_selectedPeerId!}',
                  style: const TextStyle(fontSize: 11, fontFamily: 'monospace'),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          )
        else
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.grey.withValues(alpha: 0.1),
              border: Border.all(color: Colors.grey),
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Text(
              'No peer selected — tap a peer above to select',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ),
        const SizedBox(height: 16),

        // Control buttons
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            ElevatedButton(
              onPressed: _bleStarted ? null : _startBle,
              child: const Text('Start BLE'),
            ),
            ElevatedButton(
              onPressed: _bleStarted ? _sendTestPacket : null,
              child: const Text('Send Packet'),
            ),
          ],
        ),
        const SizedBox(height: 24),

        // Packet log
        const Text(
          'Incoming Packets Log',
          style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Container(
          height: 200,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey),
            borderRadius: BorderRadius.circular(8),
          ),
          child: _packetLog.isEmpty
              ? const Center(
                  child: Text(
                    'No packets received yet',
                    style: TextStyle(color: Colors.grey),
                  ),
                )
              : ListView.builder(
                  itemCount: _packetLog.length,
                  itemBuilder: (context, index) {
                    return Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8.0,
                        vertical: 4.0,
                      ),
                      child: Text(
                        _packetLog[index],
                        style: const TextStyle(fontSize: 12),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    );
                  },
                ),
        ),
        const SizedBox(height: 48),

        // Placeholder text for future phases
        const Text(
          'More phases coming soon...',
          style: TextStyle(fontSize: 14, color: Colors.grey),
        ),
      ],
    );
  }
}
