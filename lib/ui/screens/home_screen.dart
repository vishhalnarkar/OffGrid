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
  final BleTransport _ble = BleTransport();
  final List<String> _packetLog = [];
  int _connectedPeersCount = 0;
  bool _bleStarted = false;
  StreamSubscription? _packetSubscription;
  Timer? _statsTimer;

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

    // Update connected peers count every 3 seconds
    _statsTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      setState(() {
        _connectedPeersCount = _ble.connectedPeerIds.length;
      });
      debugPrint('Connected peers: $_connectedPeersCount');
    });

    debugPrint('BLE started');
  }

  Future<void> _sendTestPacket() async {
    final packet = Packet.discovery(
      from: 'test-device-${DateTime.now().millisecondsSinceEpoch % 1000}',
      displayName: 'Test Device',
    );
    await _ble.broadcast(packet);
    debugPrint('Sent test discovery packet: ${packet.id}');
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const Text(
          'Phase 3: BLE Transport',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 16),

        // Status display (only this rebuilds on updates)
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
        const SizedBox(height: 24),

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
              child: const Text('Send Test Packet'),
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
