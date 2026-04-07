import 'package:sqflite/sqflite.dart';

import '../models/peer.dart';
import 'db_helper.dart';

/// Data Access Object for Peer operations.
/// Provides methods to insert, query, and update peers in the SQLite database.
class PeerDao {
  final String _tableName = 'peers';

  /// Insert or update a peer in the database.
  /// Uses ConflictAlgorithm.replace to overwrite existing peers with the same ID.
  /// This is how we update peer information (e.g., last_seen_at) when we see them again.
  Future<void> upsertPeer(Peer peer) async {
    final db = await DbHelper.instance.database;
    await db.insert(
      _tableName,
      peer.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  /// Retrieve a single peer by ID.
  /// Returns null if the peer doesn't exist.
  Future<Peer?> getPeerById(String id) async {
    final db = await DbHelper.instance.database;
    final maps = await db.query(_tableName, where: 'id = ?', whereArgs: [id]);

    if (maps.isEmpty) return null;
    return Peer.fromMap(maps.first);
  }

  /// Retrieve all peers ordered by last_seen_at (most recently seen first).
  /// Peers with no last_seen_at appear at the end.
  Future<List<Peer>> getAllPeers() async {
    final db = await DbHelper.instance.database;
    final maps = await db.query(
      _tableName,
      orderBy: 'last_seen_at DESC NULLS LAST',
    );

    return List.generate(maps.length, (i) => Peer.fromMap(maps[i]));
  }

  /// Update the last_seen_at timestamp and optionally the RSSI for a peer.
  /// Call this when you discover a peer via BLE to keep the discovery list fresh.
  Future<void> updateLastSeen(String id, int timestamp, {int? rssi}) async {
    final db = await DbHelper.instance.database;
    final updateMap = {
      'last_seen_at': timestamp,
      if (rssi != null) 'rssi': rssi,
    };

    await db.update(_tableName, updateMap, where: 'id = ?', whereArgs: [id]);
  }

  /// Delete a peer from the database.
  /// Use with caution — this removes all history of a peer.
  Future<void> deletePeer(String id) async {
    final db = await DbHelper.instance.database;
    await db.delete(_tableName, where: 'id = ?', whereArgs: [id]);
  }

  /// Delete all peers from the database.
  /// Use this for data wipe scenarios.
  Future<void> deleteAllPeers() async {
    final db = await DbHelper.instance.database;
    await db.delete(_tableName);
  }
}
