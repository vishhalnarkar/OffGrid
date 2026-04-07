import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

/// Singleton database helper for OffGrid.
/// Manages SQLite database initialization and provides access to the database instance.
class DbHelper {
  static final DbHelper _instance = DbHelper._internal();

  static DbHelper get instance => _instance;

  Database? _database;

  DbHelper._internal();

  /// Lazy getter for the database instance.
  /// Opens the database on first access, subsequent accesses return the cached instance.
  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  /// Initialize the SQLite database.
  /// Creates the database file if it doesn't exist, and runs onCreate on first creation.
  Future<Database> _initDatabase() async {
    final databasePath = await getDatabasesPath();
    final path = join(databasePath, 'offgrid.db');

    return await openDatabase(path, version: 1, onCreate: _onCreate);
  }

  /// Create all database tables and indices on first initialization.
  /// This is called once when the database is first created.
  Future<void> _onCreate(Database db, int version) async {
    // Create peers table
    // Stores discovered remote peers with their last seen timestamp and signal strength
    await db.execute('''
      CREATE TABLE peers (
        id TEXT PRIMARY KEY,
        display_name TEXT NOT NULL,
        last_seen_at INTEGER,
        rssi INTEGER
      );
    ''');

    // Create chats table
    // Stores conversations (both direct 1-to-1 and group chats)
    // member_ids is stored as JSON string for SQLite compatibility
    await db.execute('''
      CREATE TABLE chats (
        id TEXT PRIMARY KEY,
        type TEXT NOT NULL,
        name TEXT,
        member_ids TEXT NOT NULL,
        last_message_at INTEGER
      );
    ''');

    // Create messages table
    // Stores all messages with status tracking (sending, sent, delivered, read)
    // Foreign key constraint ensures messages only exist for valid chats
    await db.execute('''
      CREATE TABLE messages (
        id TEXT PRIMARY KEY,
        chat_id TEXT NOT NULL,
        sender_id TEXT NOT NULL,
        content TEXT NOT NULL,
        status TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        FOREIGN KEY (chat_id) REFERENCES chats(id)
      );
    ''');

    // Create index for efficient message queries by chat and time
    // This speeds up loading conversation history
    await db.execute('''
      CREATE INDEX idx_messages_chat ON messages(chat_id, created_at);
    ''');
  }

  /// Close the database connection.
  /// Call this during app shutdown to ensure proper cleanup.
  Future<void> close() async {
    if (_database != null) {
      await _database!.close();
      _database = null;
    }
  }
}
