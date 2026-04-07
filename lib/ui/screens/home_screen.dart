import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

import '../../data/chat_dao.dart';
import '../../data/message_dao.dart';
import '../../models/message.dart';

/// Home screen shown after onboarding.
/// Displays welcome message with the user's display name and ID.
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

          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // Welcome message
                Text(
                  'Welcome, $displayName',
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 16),

                // User ID display
                Text('ID: $displayId', style: const TextStyle(fontSize: 14)),
                const SizedBox(height: 48),

                // Placeholder text
                const Text(
                  'Phases coming soon...',
                  style: TextStyle(fontSize: 16, color: Colors.grey),
                ),
                const SizedBox(height: 32),

                // TEST 2: Deterministic Chat ID test button
                ElevatedButton(
                  onPressed: () async {
                    final chatDao = ChatDao();

                    // First call: alice → bob
                    final chat1 = await chatDao.getOrCreateDirectChat(
                      'alice_id_12345',
                      'bob_id_67890',
                    );
                    debugPrint('Chat 1 ID: ${chat1.id}');

                    // Second call: bob → alice (reversed order)
                    final chat2 = await chatDao.getOrCreateDirectChat(
                      'bob_id_67890',
                      'alice_id_12345',
                    );
                    debugPrint('Chat 2 ID: ${chat2.id}');

                    // Check if IDs match
                    if (chat1.id == chat2.id) {
                      debugPrint('✅ DETERMINISTIC ID TEST PASSED');
                    } else {
                      debugPrint('❌ DETERMINISTIC ID TEST FAILED');
                    }
                  },
                  child: const Text('Test Deterministic Chat ID'),
                ),
                const SizedBox(height: 16),

                // TEST 3: Message Index & Query Performance test button
                ElevatedButton(
                  onPressed: () async {
                    final chatDao = ChatDao();
                    final msgDao = MessageDao();
                    const uuid = Uuid();

                    // Create a test chat
                    final chat = await chatDao.getOrCreateDirectChat(
                      'me',
                      'friend',
                    );

                    // Insert 100 messages
                    final sw = Stopwatch()..start();
                    final baseTime = DateTime.now().millisecondsSinceEpoch;
                    for (int i = 0; i < 100; i++) {
                      await msgDao.insertMessage(
                        Message(
                          id: uuid.v4(),
                          chatId: chat.id,
                          senderId: 'me',
                          content: 'Test message $i',
                          status: 'sent',
                          createdAt: baseTime + i,
                        ),
                      );
                    }
                    sw.stop();
                    debugPrint(
                      'Inserted 100 messages in ${sw.elapsedMilliseconds}ms',
                    );

                    // Query them back (with limit: 100 to fetch all inserted messages)
                    final sw2 = Stopwatch()..start();
                    final messages = await msgDao.getMessagesForChat(
                      chat.id,
                      limit: 100,
                    );
                    sw2.stop();
                    debugPrint(
                      'Queried ${messages.length} messages in ${sw2.elapsedMilliseconds}ms',
                    );

                    if (messages.length == 100 &&
                        sw2.elapsedMilliseconds < 100) {
                      debugPrint('✅ MESSAGE INDEX TEST PASSED');
                    } else {
                      debugPrint('❌ MESSAGE INDEX TEST FAILED');
                    }
                  },
                  child: const Text('Test Message Index'),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
