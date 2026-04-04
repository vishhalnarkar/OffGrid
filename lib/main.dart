import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'ui/screens/home_screen.dart';
import 'ui/screens/onboarding_screen.dart';

void main() async {
  // Ensure Flutter bindings are initialized
  WidgetsFlutterBinding.ensureInitialized();

  // Check if user has already completed onboarding
  final prefs = await SharedPreferences.getInstance();
  final myId = prefs.getString('myId');
  final hasCompleted = myId != null && myId.isNotEmpty;

  runApp(MainApp(hasCompleted: hasCompleted));
}

class MainApp extends StatelessWidget {
  final bool hasCompleted;

  const MainApp({super.key, required this.hasCompleted});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'OffGrid',
      theme: ThemeData.dark(useMaterial3: true),
      // If onboarding is complete, show HomeScreen; otherwise OnboardingScreen
      home: hasCompleted ? const HomeScreen() : const OnboardingScreen(),
    );
  }
}
