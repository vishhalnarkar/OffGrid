import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

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
              ],
            ),
          );
        },
      ),
    );
  }
}
