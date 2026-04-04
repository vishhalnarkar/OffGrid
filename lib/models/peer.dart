import 'dart:convert';

/// Represents a discovered remote peer.
/// Fields: id, displayName, lastSeenAt (unix ms?), rssi (RSSI signal strength)
class Peer {
  final String id;
  final String displayName;
  final int? lastSeenAt; // Unix timestamp in milliseconds, nullable
  final int? rssi; // Received Signal Strength Indicator, nullable

  /// Constructor
  Peer({
    required this.id,
    required this.displayName,
    this.lastSeenAt,
    this.rssi,
  });

  /// Convert Peer to Map for JSON serialization or database storage
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'displayName': displayName,
      'lastSeenAt': lastSeenAt,
      'rssi': rssi,
    };
  }

  /// Create Peer from Map (database or JSON)
  factory Peer.fromMap(Map<String, dynamic> map) {
    return Peer(
      id: map['id'] as String,
      displayName: map['displayName'] as String,
      lastSeenAt: map['lastSeenAt'] as int?,
      rssi: map['rssi'] as int?,
    );
  }

  /// JSON serialization
  String toJson() => jsonEncode(toMap());

  /// JSON deserialization
  factory Peer.fromJson(String json) {
    return Peer.fromMap(jsonDecode(json) as Map<String, dynamic>);
  }

  /// Create a copy with optionally replaced fields
  Peer copyWith({String? id, String? displayName, int? lastSeenAt, int? rssi}) {
    return Peer(
      id: id ?? this.id,
      displayName: displayName ?? this.displayName,
      lastSeenAt: lastSeenAt ?? this.lastSeenAt,
      rssi: rssi ?? this.rssi,
    );
  }

  @override
  String toString() => 'Peer(id: $id, displayName: $displayName)';
}
