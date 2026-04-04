import 'dart:convert';

/// Represents a single chat message.
/// Fields: id, chatId, senderId, content, status (sending/sent/delivered/read), createdAt (unix ms)
class Message {
  final String id;
  final String chatId;
  final String senderId;
  final String content;
  final String status; // sending, sent, delivered, read
  final int createdAt; // Unix timestamp in milliseconds

  /// Constructor
  Message({
    required this.id,
    required this.chatId,
    required this.senderId,
    required this.content,
    required this.status,
    required this.createdAt,
  });

  /// Convert Message to Map for JSON serialization or database storage
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'chatId': chatId,
      'senderId': senderId,
      'content': content,
      'status': status,
      'createdAt': createdAt,
    };
  }

  /// Create Message from Map (database or JSON)
  factory Message.fromMap(Map<String, dynamic> map) {
    return Message(
      id: map['id'] as String,
      chatId: map['chatId'] as String,
      senderId: map['senderId'] as String,
      content: map['content'] as String,
      status: map['status'] as String,
      createdAt: map['createdAt'] as int,
    );
  }

  /// JSON serialization
  String toJson() => jsonEncode(toMap());

  /// JSON deserialization
  factory Message.fromJson(String json) {
    return Message.fromMap(jsonDecode(json) as Map<String, dynamic>);
  }

  /// Create a copy with optionally replaced fields
  Message copyWith({
    String? id,
    String? chatId,
    String? senderId,
    String? content,
    String? status,
    int? createdAt,
  }) {
    return Message(
      id: id ?? this.id,
      chatId: chatId ?? this.chatId,
      senderId: senderId ?? this.senderId,
      content: content ?? this.content,
      status: status ?? this.status,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  String toString() =>
      'Message(id: $id, chatId: $chatId, from: $senderId, status: $status)';
}
