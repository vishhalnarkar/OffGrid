import 'package:sqflite/sqflite.dart';

import '../models/message.dart';
import 'db_helper.dart';

/// Data Access Object for Message operations.
/// Provides methods to insert, query, and update messages in the SQLite database.
class MessageDao {
  final String _tableName = 'messages';

  /// Insert a message into the database.
  /// Uses ConflictAlgorithm.ignore to prevent duplicates.
  /// If a message with the same ID already exists, it is silently ignored.
  /// This is important in a mesh network where the same message might arrive
  /// via multiple paths and need to be deduplicated.
  Future<void> insertMessage(Message message) async {
    final db = await DbHelper.instance.database;
    await db.insert(
      _tableName,
      message.toMap(),
      conflictAlgorithm: ConflictAlgorithm.ignore,
    );
  }

  /// Retrieve all messages for a specific chat, ordered by creation time (oldest first).
  /// Optionally limit the number of returned messages (default 50 for memory efficiency).
  /// This is used to load conversation history.
  Future<List<Message>> getMessagesForChat(
    String chatId, {
    int limit = 50,
  }) async {
    final db = await DbHelper.instance.database;
    final maps = await db.query(
      _tableName,
      where: 'chat_id = ?',
      whereArgs: [chatId],
      orderBy: 'created_at ASC',
      limit: limit,
    );

    return List.generate(maps.length, (i) => Message.fromMap(maps[i]));
  }

  /// Update the status of a message.
  /// Status values: 'sending', 'sent', 'delivered', 'read'
  /// Call this when you receive an ACK from another peer or when you know
  /// a message has been successfully transmitted.
  Future<void> updateMessageStatus(String id, String status) async {
    final db = await DbHelper.instance.database;
    await db.update(
      _tableName,
      {'status': status},
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  /// Retrieve a single message by ID.
  /// Returns null if the message doesn't exist.
  Future<Message?> getMessageById(String id) async {
    final db = await DbHelper.instance.database;
    final maps = await db.query(_tableName, where: 'id = ?', whereArgs: [id]);

    if (maps.isEmpty) return null;
    return Message.fromMap(maps.first);
  }

  /// Get the count of messages in a chat.
  /// Useful for pagination or checking if a chat is empty.
  Future<int> getMessageCount(String chatId) async {
    final db = await DbHelper.instance.database;
    final result = await db.rawQuery(
      'SELECT COUNT(*) as count FROM $_tableName WHERE chat_id = ?',
      [chatId],
    );
    return Sqflite.firstIntValue(result) ?? 0;
  }

  /// Delete a message from the database.
  /// This removes a single message but keeps the chat intact.
  Future<void> deleteMessage(String id) async {
    final db = await DbHelper.instance.database;
    await db.delete(_tableName, where: 'id = ?', whereArgs: [id]);
  }

  /// Delete all messages for a specific chat.
  /// Use this when clearing a conversation.
  Future<void> deleteMessagesForChat(String chatId) async {
    final db = await DbHelper.instance.database;
    await db.delete(_tableName, where: 'chat_id = ?', whereArgs: [chatId]);
  }

  /// Delete all messages from the database.
  /// Use this for data wipe scenarios.
  Future<void> deleteAllMessages() async {
    final db = await DbHelper.instance.database;
    await db.delete(_tableName);
  }
}
