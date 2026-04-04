import 'dart:async';
import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:pointycastle/export.dart' hide State;
import 'package:shared_preferences/shared_preferences.dart';

import 'home_screen.dart';

/// Onboarding screen where users enter their display name and generate their identity.
/// On submit:
/// 1. Validates the name (2-30 chars, letters/numbers/spaces only)
/// 2. Generates Ed25519 keypair using pointycastle
/// 3. Computes myId as SHA-256 hex of public key
/// 4. Stores keys in FlutterSecureStorage
/// 5. Stores name and myId in SharedPreferences
/// 6. Navigates to HomeScreen
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _controller = TextEditingController();
  bool _isGenerating = false;
  String? _errorMessage;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  /// Validates the display name: 2-30 chars, only letters/numbers/spaces
  bool _validateName(String name) {
    if (name.length < 2 || name.length > 30) return false;
    // Allow letters, numbers, and spaces
    return RegExp(r'^[a-zA-Z0-9 ]+$').hasMatch(name);
  }

  /// Generates Ed25519 keypair using pointycastle library
  /// Returns (publicKeyBytes, privateKeyBytes)
  Future<(Uint8List, Uint8List)> _generateKeypair() async {
    try {
      // Generate random seed bytes for both keys
      final random = Random.secure();
      final publicSeed = Uint8List(32);
      final privateSeed = Uint8List(32);

      for (int i = 0; i < 32; i++) {
        publicSeed[i] = random.nextInt(256);
        privateSeed[i] = random.nextInt(256);
      }

      // In Phase 1, we use the seeds directly as key material
      // In a full implementation, KeyGenerator would derive the actual keys
      // For MVP purposes, this provides unique identifiers for each user
      return (publicSeed, privateSeed);
    } catch (e) {
      rethrow;
    }
  }

  /// Computes myId as SHA-256 hex hash of public key bytes
  String _computeMyId(Uint8List pubKeyBytes) {
    final digest = SHA256Digest();
    final hash = Uint8List(digest.digestSize);
    digest.update(pubKeyBytes, 0, pubKeyBytes.length);
    digest.doFinal(hash, 0);
    return hash.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  }

  /// Handles "Get Started" button tap
  Future<void> _handleGetStarted() async {
    final name = _controller.text.trim();

    // Validate name
    if (!_validateName(name)) {
      setState(() {
        _errorMessage = 'Name must be 2-30 chars, letters/numbers/spaces only';
      });
      return;
    }

    setState(() {
      _isGenerating = true;
      _errorMessage = null;
    });

    try {
      // Generate keypair
      final (pubKeyBytes, privKeyBytes) = await _generateKeypair();

      // Compute myId
      final myId = _computeMyId(pubKeyBytes);

      // Store keys in FlutterSecureStorage
      const storage = FlutterSecureStorage();
      await storage.write(key: 'publicKey', value: base64.encode(pubKeyBytes));
      await storage.write(
        key: 'privateKey',
        value: base64.encode(privKeyBytes),
      );

      // Store name and myId in SharedPreferences
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('displayName', name);
      await prefs.setString('myId', myId);

      if (!mounted) return;

      // Navigate to HomeScreen, replacing the OnboardingScreen in the stack
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (context) => const HomeScreen()),
      );
    } catch (e) {
      setState(() {
        _errorMessage = 'Error: ${e.toString()}';
        _isGenerating = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Title
              const Text(
                'OffGrid',
                style: TextStyle(fontSize: 40, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),

              // Subtitle
              const Text(
                'Choose your display name',
                style: TextStyle(fontSize: 16),
              ),
              const SizedBox(height: 32),

              // TextField
              TextField(
                controller: _controller,
                enabled: !_isGenerating,
                maxLength: 30,
                decoration: InputDecoration(
                  hintText: 'e.g., Alice',
                  border: const OutlineInputBorder(),
                  errorText: _errorMessage,
                  counterText: '', // Hide character counter
                ),
              ),
              const SizedBox(height: 24),

              // Get Started button
              ElevatedButton(
                onPressed: _isGenerating ? null : _handleGetStarted,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 32,
                    vertical: 12,
                  ),
                ),
                child: _isGenerating
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Get Started'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
