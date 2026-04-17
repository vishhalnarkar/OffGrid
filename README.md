# OffGrid for Android

A secure, decentralized, peer-to-peer messaging app that works over Bluetooth mesh networks. No internet required for mesh chats, no servers, no phone numbers - just pure encrypted communication. OffGrid also supports Nostr integration and geohash-based location channels for connecting with others in your geographic area.

This is the **Android port** of the original [OffGrid iOS app](https://github.com/jackjackbits/OffGrid), maintaining protocol compatibility for cross-platform communication.

## Install OffGrid

You can download the latest version of OffGrid for Android from the [GitHub Releases page](https://github.com/permissionlesstech/OffGrid-android/releases).

**Current Version:** 1.7.2

**Instructions:**

1.  **Download the APK:** On your Android device, navigate to the link above and download the latest `.apk` file. Open it.
2.  **Allow Unknown Sources:** On some devices, before you can install the APK, you may need to enable "Install from unknown sources" in your device's settings. This is typically found under **Settings > Security** or **Settings > Apps & notifications > Special app access**.
3.  **Install:** Open the downloaded `.apk` file to begin the installation.

## License

This project is released into the public domain. See the [LICENSE](LICENSE.md) file for details.

## Features

### Core Messaging
- **✅ Cross-Platform Compatible**: Protocol compatibility with iOS OffGrid
- **✅ Decentralized Mesh Network**: Automatic peer discovery and multi-hop message relay over Bluetooth LE
- **✅ End-to-End Encryption**: X25519 key exchange + AES-256-GCM for private messages
- **✅ Channel-Based Chats**: Topic-based group messaging with optional password protection
- **✅ Store & Forward**: Messages cached for offline peers and delivered when they reconnect
- **✅ Privacy First**: No accounts, no phone numbers, no persistent identifiers

### User Experience
- **✅ Modern UI**: Hike-inspired chat interface with futuristic neon aesthetics
- **✅ Material Design 3**: Clean, intuitive interface with Jetpack Compose
- **✅ Dark Theme**: Terminal-inspired dark theme optimized for readability
- **✅ Connection Status**: Visual indicator showing mesh network connectivity (green = connected, grey = disconnected)
- **✅ User Settings**: Easy username customization in settings
- **✅ Message Retention**: Optional channel-wide message saving controlled by channel owners
- **✅ Emergency Wipe**: Triple-tap logo to instantly clear all data

### Advanced Features
- **✅ IRC-Style Commands**: Familiar `/join`, `/msg`, `/who` style interface
- **✅ Nostr Integration**: Connect to Nostr relays for extended reach
- **✅ Geohash Channels**: Location-based channels for connecting with nearby users
- **✅ Voice Messages**: Send and receive voice notes over the mesh
- **✅ File Sharing**: Share images and files with peers
- **✅ @ Mentions**: Mention users with autocomplete support
- **✅ Battery Optimization**: Adaptive scanning and power management
- **✅ Proof of Work**: Optional PoW for spam prevention on Nostr channels

## Android Setup

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or newer
- **Android SDK**: API level 26 (Android 8.0) or higher
- **Kotlin**: 2.2.0
- **Gradle**: 8.0 or newer
- **JDK**: 17 or newer

### Build Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/permissionlesstech/OffGrid-android.git
   cd OffGrid-android
   ```

2. **Open in Android Studio:**
   ```bash
   # Open Android Studio and select "Open an Existing Project"
   # Navigate to the OffGrid-android directory
   ```

3. **Build the project:**
   ```bash
   ./gradlew build
   ```

4. **Install on device:**
   ```bash
   ./gradlew installDebug
   ```

### Development Build

For development builds with debugging enabled:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

For production releases:

```bash
./gradlew assembleRelease
```

The build system automatically creates optimized APK splits for different architectures (arm64-v8a, x86_64, armeabi-v7a, x86).

## Android-Specific Requirements

### Permissions

The app requires the following permissions (automatically requested during onboarding):

- **Bluetooth**: Core BLE functionality for mesh networking
- **Location**: Required for BLE scanning on Android (system requirement)
- **Notifications**: Message alerts and background updates
- **Camera**: For QR code scanning (verification features)
- **Microphone**: For voice message recording
- **Storage**: For file and image sharing

### Hardware Requirements

- **Bluetooth LE (BLE)**: Required for mesh networking
- **Android 8.0+**: API level 26 minimum
- **RAM**: 2GB recommended for optimal performance
- **Storage**: 100MB minimum for app and cached messages

## Usage

### Getting Started

1. **Install the app** on your Android device (requires Android 8.0+)
2. **Complete onboarding** - grant required permissions (Bluetooth, Location, etc.)
3. **Set your username** in settings or use the auto-generated one
4. **Launch OffGrid** - it will auto-start mesh networking
5. **Connect automatically** to nearby iOS and Android OffGrid users
6. **Join a channel** with `/j #general` or start chatting in public
7. **Messages relay** through the mesh network to reach distant peers

### User Interface

- **Connection Indicator**: Green dot = connected to mesh, Grey dot = disconnected
- **Peer Count**: Shows number of connected peers in the app bar
- **Settings**: Tap the settings icon to customize your username and preferences
- **Mesh Visualization**: View your mesh network topology in the About/Settings screen

### Basic Commands

- `/j #channel` - Join or create a channel
- `/m @name message` - Send a private message
- `/w` - List online users
- `/channels` - Show all discovered channels
- `/block @name` - Block a peer from messaging you
- `/block` - List all blocked peers
- `/unblock @name` - Unblock a peer
- `/clear` - Clear chat messages
- `/pass [password]` - Set/change channel password (owner only)
- `/transfer @name` - Transfer channel ownership
- `/save` - Toggle message retention for channel (owner only)

### Media Features

- **Voice Messages**: Tap and hold the microphone button to record
- **Image Sharing**: Tap the image button to select and send photos
- **File Sharing**: Share documents and files with peers
- **Voice Visualizer**: Real-time waveform display during recording

### Channel Features

- **Password Protection**: Channel owners can set passwords with `/pass`
- **Message Retention**: Owners can enable mandatory message saving with `/save`
- **@ Mentions**: Use `@nickname` to mention users (with autocomplete)
- **Ownership Transfer**: Pass control to trusted users with `/transfer`
- **Geohash Channels**: Location-based channels for connecting with nearby users
- **Nostr Integration**: Connect channels to Nostr relays for extended reach

## Security & Privacy

### Encryption
- **Private Messages**: X25519 key exchange + AES-256-GCM encryption
- **Channel Messages**: Argon2id password derivation + AES-256-GCM
- **Digital Signatures**: Ed25519 for message authenticity
- **Forward Secrecy**: New key pairs generated each session

### Privacy Features
- **No Registration**: No accounts, emails, or phone numbers required
- **Ephemeral by Default**: Messages exist only in device memory
- **Cover Traffic**: Random delays and dummy messages prevent traffic analysis
- **Emergency Wipe**: Triple-tap logo to instantly clear all data
- **Bundled Tor Support**: Built-in Tor network integration for enhanced privacy when internet connectivity is available

## Performance & Efficiency

### Message Compression
- **LZ4 Compression**: Automatic compression for messages >100 bytes
- **30-70% bandwidth savings** on typical text messages
- **Smart compression**: Skips already-compressed data

### Battery Optimization
- **Adaptive Power Modes**: Automatically adjusts based on battery level
  - Performance mode: Full features when charging or >60% battery
  - Balanced mode: Default operation (30-60% battery)
  - Power saver: Reduced scanning when <30% battery
  - Ultra-low power: Emergency mode when <10% battery
- **Background efficiency**: Automatic power saving when app backgrounded
- **Configurable scanning**: Duty cycle adapts to battery state

### Network Efficiency
- **Optimized Bloom filters**: Faster duplicate detection with less memory
- **Message aggregation**: Batches small messages to reduce transmissions
- **Adaptive connection limits**: Adjusts peer connections based on power mode

## Technical Architecture

### Core Components

1. **MainActivity.kt**: Main activity with onboarding flow and permission management
2. **ChatViewModel.kt**: MVVM pattern managing app state and business logic
3. **ChatScreenHike.kt**: Modern Hike-inspired UI with neon aesthetics
4. **BluetoothMeshService.kt**: Core BLE mesh networking (central + peripheral roles)
5. **EncryptionService.kt**: Cryptographic operations using BouncyCastle
6. **NoiseChannelEncryption.kt**: Noise Protocol Framework implementation
7. **MessageHandler.kt**: Message processing and routing logic
8. **VoiceRecorder.kt**: Voice message recording and playback
9. **MediaSendingManager.kt**: File and image sharing functionality
10. **GeohashViewModel.kt**: Location-based channel management

### UI Architecture

- **Jetpack Compose**: Modern declarative UI framework
- **Material Design 3**: Latest Material Design components
- **MVVM Pattern**: Clean separation of concerns
- **StateFlow**: Reactive state management
- **Coroutines**: Asynchronous operations

### Binary Protocol
OffGrid uses an efficient binary protocol optimized for Bluetooth LE:
- Compact packet format with 1-byte type field
- TTL-based message routing (max 7 hops)
- Automatic fragmentation for large messages (up to 10MB)
- Message deduplication via unique IDs
- Noise Protocol Framework for secure channels

### Mesh Networking
- Each device acts as both client and peripheral
- Automatic peer discovery and connection management
- Store-and-forward for offline message delivery
- Adaptive duty cycling for battery optimization
- Multi-hop routing with TTL-based flood control
- Fragment reassembly for large files and media

### Dependencies

- **Jetpack Compose**: Modern declarative UI
- **BouncyCastle**: Cryptographic operations (X25519, Ed25519, AES-GCM, Argon2)
- **Kotlin Coroutines**: Asynchronous programming
- **CameraX**: Camera integration for QR scanning
- **ML Kit**: Barcode scanning for verification
- **OkHttp**: HTTP client for Nostr relay connections
- **Gson**: JSON serialization for Nostr events
- **EncryptedSharedPreferences**: Secure local storage

## Cross-Platform Communication

This Android port enables communication with the original iOS OffGrid app:

- **iPhone ↔ Android**: Bidirectional messaging over Bluetooth mesh
- **Mixed Groups**: iOS and Android users in same channels
- **Feature Parity**: Core commands and encryption work across platforms
- **Protocol Compatibility**: Compatible message format and routing behavior

**Note**: Some features may be platform-specific (e.g., UI differences, platform-specific optimizations).

**iOS Version**: For iPhone/iPad users, get the original OffGrid at [github.com/jackjackbits/OffGrid](https://github.com/jackjackbits/OffGrid)

## Contributing

Contributions are welcome! Key areas for enhancement:

1. **Performance**: Battery optimization and connection reliability
2. **UI/UX**: Enhanced Material Design 3 features and animations
3. **Security**: Additional cryptographic features and audits
4. **Testing**: Unit and integration test coverage
5. **Documentation**: API documentation and development guides
6. **Nostr Features**: Enhanced Nostr relay integration
7. **Media Features**: Additional media types and compression

### Development Guidelines

- Follow Kotlin coding conventions
- Use Jetpack Compose for all UI components
- Maintain MVVM architecture pattern
- Write unit tests for business logic
- Document public APIs with KDoc comments
- Test on multiple Android versions and devices

## Support & Issues

- **Bug Reports**: [Create an issue](../../issues) with device info, Android version, and logs
- **Feature Requests**: [Start a discussion](https://github.com/orgs/permissionlesstech/discussions)
- **Security Issues**: Email security concerns privately to the maintainers
- **iOS Compatibility**: Cross-reference with [original iOS repo](https://github.com/jackjackbits/OffGrid)
- **Documentation**: Check the [wiki](../../wiki) for additional guides

### Debugging

To enable debug logging:
1. Build the app in debug mode
2. Use `adb logcat` to view logs
3. Filter by tag: `adb logcat | grep OffGrid`

For iOS-specific issues, please refer to the [original iOS OffGrid repository](https://github.com/jackjackbits/OffGrid).

---

**Built with ❤️ for a decentralized future**
