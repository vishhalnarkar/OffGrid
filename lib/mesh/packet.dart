import 'dart:convert';
import 'package:uuid/uuid.dart';

/// Represents a single packet in the OffGrid mesh network.
/// A packet is the fundamental unit of communication, containing:
/// - id: unique identifier for deduplication
/// - type: 'chat', 'ack', or 'discovery'
/// - from: sender's ID
/// - to: recipient's ID or 'BROADCAST'
/// - ttl: Time To Live (decremented on each relay, dropped at 0)
/// - timestamp: Unix milliseconds when packet was created
/// - payload: type-specific data (Map<String, dynamic>)
class Packet {
  final String id;
  final String type;
  final String from;
  final String to;
  final int ttl;
  final int timestamp;
  final Map<String, dynamic> payload;

  /// Constructor for custom packets
  Packet({
    required this.id,
    required this.type,
    required this.from,
    required this.to,
    required this.ttl,
    required this.timestamp,
    required this.payload,
  });

  /// Factory constructor for chat packets
  /// Sent from one peer to another with message content
  factory Packet.chat({
    required String from,
    required String to,
    required String chatId,
    required String text,
  }) {
    return Packet(
      id: const Uuid().v4(),
      type: 'chat',
      from: from,
      to: to,
      ttl: 5,
      timestamp: DateTime.now().millisecondsSinceEpoch,
      payload: {'chatId': chatId, 'text': text},
    );
  }

  /// Factory constructor for ACK (acknowledgement) packets
  /// Sent to confirm receipt of a chat packet
  factory Packet.ack({
    required String from,
    required String to,
    required String originalId,
  }) {
    return Packet(
      id: const Uuid().v4(),
      type: 'ack',
      from: from,
      to: to,
      ttl: 5,
      timestamp: DateTime.now().millisecondsSinceEpoch,
      payload: {'originalId': originalId},
    );
  }

  /// Factory constructor for discovery packets
  /// Broadcast to announce presence and display name to nearby peers
  factory Packet.discovery({
    required String from,
    required String displayName,
  }) {
    return Packet(
      id: const Uuid().v4(),
      type: 'discovery',
      from: from,
      to: 'BROADCAST',
      ttl: 3,
      timestamp: DateTime.now().millisecondsSinceEpoch,
      payload: {'displayName': displayName},
    );
  }

  /// Create a copy of this packet with TTL decremented by 1
  /// Used when relaying the packet to the next hop
  Packet withDecrementedTtl() {
    return Packet(
      id: id,
      type: type,
      from: from,
      to: to,
      ttl: ttl - 1,
      timestamp: timestamp,
      payload: payload,
    );
  }

  /// Convert packet to bytes for transmission over BLE
  /// Format: UTF-8 encoded JSON
  List<int> toBytes() {
    final map = {
      'id': id,
      'type': type,
      'from': from,
      'to': to,
      'ttl': ttl,
      'timestamp': timestamp,
      'payload': payload,
    };
    return utf8.encode(jsonEncode(map));
  }

  /// Reconstruct a packet from bytes received over BLE
  /// Throws exceptions if the data is malformed
  factory Packet.fromBytes(List<int> bytes) {
    final jsonString = utf8.decode(bytes);
    final map = jsonDecode(jsonString) as Map<String, dynamic>;

    return Packet(
      id: map['id'] as String,
      type: map['type'] as String,
      from: map['from'] as String,
      to: map['to'] as String,
      ttl: map['ttl'] as int,
      timestamp: map['timestamp'] as int,
      payload: map['payload'] as Map<String, dynamic>,
    );
  }

  @override
  String toString() =>
      'Packet(id=$id, type=$type, from=$from, to=$to, ttl=$ttl)';
}
