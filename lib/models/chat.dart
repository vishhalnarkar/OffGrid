import 'dart:convert';
import 'package:flutter/foundation.dart';

/// Represents a chat conversation (direct or group).
/// Fields: id, type (direct/group), name (optional), memberIds (List stored as JSON), lastMessageAt (unix ms?)
class Chat {
  final String id;
  final String type; // 'direct' or 'group'
  final String? name; // Optional, for group chats
  final List<String> memberIds; // List of peer IDs
  final int? lastMessageAt; // Unix timestamp in milliseconds, nullable

  /// Constructor
  Chat({
    required this.id,
    required this.type,
    this.name,
    required this.memberIds,
    this.lastMessageAt,
  });

  /// Convert Chat to Map for JSON serialization or database storage
  /// Uses snake_case keys to match SQLite schema
  /// memberIds is encoded as JSON string
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'type': type,
      'name': name,
      'member_ids': jsonEncode(memberIds), // Encode list as JSON string
      'last_message_at': lastMessageAt,
    };
  }

  /// Create Chat from Map (database or JSON)
  /// Converts from snake_case database keys to camelCase model fields
  /// memberIds is decoded from JSON string with safe error handling
  factory Chat.fromMap(Map<String, dynamic> map) {
    List<String> memberIds = [];
    try {
      final memberIdsJson = map['member_ids'] as String;
      memberIds = List<String>.from(
        jsonDecode(memberIdsJson) as List<dynamic>,
      );
    } catch (e) {
      debugPrint('[Chat] ERROR parsing member_ids: $e, using empty list');
      memberIds = [];
    }

    return Chat(
      id: map['id'] as String,
      type: map['type'] as String,
      name: map['name'] as String?,
      memberIds: memberIds,
      lastMessageAt: map['last_message_at'] as int?,
    );
  }

  /// JSON serialization
  String toJson() => jsonEncode(toMap());

  /// JSON deserialization
  factory Chat.fromJson(String json) {
    return Chat.fromMap(jsonDecode(json) as Map<String, dynamic>);
  }

  /// Create a copy with optionally replaced fields
  Chat copyWith({
    String? id,
    String? type,
    String? name,
    List<String>? memberIds,
    int? lastMessageAt,
  }) {
    return Chat(
      id: id ?? this.id,
      type: type ?? this.type,
      name: name ?? this.name,
      memberIds: memberIds ?? this.memberIds,
      lastMessageAt: lastMessageAt ?? this.lastMessageAt,
    );
  }

  @override
  String toString() =>
      'Chat(id: $id, type: $type, members: ${memberIds.length})';
}
