import 'package:sqflite/sqflite.dart';

import '../models/chat.dart';
import 'db_helper.dart';

/// Data Access Object for Chat operations.
/// Provides methods to create, query, and update chats in the SQLite database.
class ChatDao {
  final String _tableName = 'chats';

  /// Insert or update a chat in the database.
  /// Uses ConflictAlgorithm.replace to overwrite existing chats with the same ID.
  Future<void> upsertChat(Chat chat) async {
    final db = await DbHelper.instance.database;
    await db.insert(
      _tableName,
      chat.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  /// Retrieve a single chat by ID.
  /// Returns null if the chat doesn't exist.
  Future<Chat?> getChatById(String id) async {
    final db = await DbHelper.instance.database;
    final maps = await db.query(_tableName, where: 'id = ?', whereArgs: [id]);

    if (maps.isEmpty) return null;
    return Chat.fromMap(maps.first);
  }

  /// Retrieve all chats ordered by last_message_at (most recent first).
  /// Chats with no messages appear at the end.
  Future<List<Chat>> getAllChats() async {
    final db = await DbHelper.instance.database;
    final maps = await db.query(
      _tableName,
      orderBy: 'last_message_at DESC NULLS LAST',
    );

    return List.generate(maps.length, (i) => Chat.fromMap(maps[i]));
  }

  /// Update the last_message_at timestamp for a chat.
  /// Call this whenever a new message arrives in this chat.
  Future<void> updateLastMessage(String chatId, int timestamp) async {
    final db = await DbHelper.instance.database;
    await db.update(
      _tableName,
      {'last_message_at': timestamp},
      where: 'id = ?',
      whereArgs: [chatId],
    );
  }

  /// Get or create a direct chat with deterministic ID.
  /// For a direct chat between Alice and Bob, the chat ID is always the same
  /// regardless of who initiates. This allows peers to independently compute
  /// the same chat ID without coordinating with a server.
  ///
  /// The deterministic ID is computed as: sort([myId, peerId]) → join with '_direct_'
  /// Example: if myId='alice' and peerId='bob', chatId becomes 'alice_direct_bob'
  ///
  /// This method:
  /// 1. Computes the deterministic chatId from the two peer IDs
  /// 2. Checks if a chat with this ID already exists
  /// 3. If it exists, returns it
  /// 4. If not, creates a new direct chat and returns it
  Future<Chat> getOrCreateDirectChat(String myId, String peerId) async {
    // Sort the IDs alphabetically to ensure deterministic result
    final ids = [myId, peerId]..sort();
    final chatId = '${ids[0]}_direct_${ids[1]}';

    // Check if chat already exists
    final existingChat = await getChatById(chatId);
    if (existingChat != null) {
      return existingChat;
    }

    // Create new direct chat
    final newChat = Chat(
      id: chatId,
      type: 'direct',
      name: null, // Direct chats have no name (just use peer's displayName)
      memberIds: [myId, peerId],
      lastMessageAt: null,
    );

    // Insert and return
    await upsertChat(newChat);
    return newChat;
  }

  /// Delete a chat from the database.
  /// Note: This does NOT delete associated messages — handle that separately if needed.
  Future<void> deleteChat(String id) async {
    final db = await DbHelper.instance.database;
    await db.delete(_tableName, where: 'id = ?', whereArgs: [id]);
  }

  /// Delete all chats from the database.
  /// Use this for data wipe scenarios.
  Future<void> deleteAllChats() async {
    final db = await DbHelper.instance.database;
    await db.delete(_tableName);
  }
}
