# 📡 OffGrid

> **Completely offline. Fully decentralized. Peer-to-peer messaging over Bluetooth Low Energy.**

OffGrid is a Flutter-based chat application that operates without any internet connection, cellular data, or centralized servers. It uses Bluetooth Low Energy (BLE) mesh networking to enable users to communicate directly with each other in a mesh topology, making it ideal for disaster relief, remote areas, and privacy-conscious communication.

---

## 🎯 What is OffGrid?

OffGrid is a **fully peer-to-peer chat application** that works entirely offline using Bluetooth Low Energy (BLE) technology. Unlike traditional messaging apps that rely on internet connectivity and centralized servers, OffGrid allows users to:

- **Chat with nearby peers** without any internet or cellular connection
- **Relay messages** automatically through multiple devices to reach users beyond direct BLE range
- **Maintain complete privacy** — no servers, no cloud, no data collection
- **Work in remote or offline environments** — mountains, caves, rural areas, or disaster zones
- **Create decentralized communication networks** spontaneously without infrastructure

---

## 📚 About the Project

OffGrid is a learning and research project designed to demonstrate:
- **Bluetooth Low Energy mesh networking** principles
- **TTL-bounded flooding** routing algorithm for decentralized message delivery
- **Local SQLite storage** for offline message persistence
- **State management with Riverpod** in a peer-to-peer context
- **Cross-platform Flutter development** with platform-specific BLE capabilities

This project emphasizes **clear, well-commented code** suitable for educational purposes and future contributors.

---

## ⚙️ Technical Overview

### Architecture Stack
The application follows a **strict 5-layer architecture**:

```
┌─────────────────────────────────────────────┐
│           UI Layer (Flutter)                │
├─────────────────────────────────────────────┤
│        State Management (Riverpod)          │
├─────────────────────────────────────────────┤
│        Mesh Router & Deduplication          │
├─────────────────────────────────────────────┤
│      BLE Transport (flutter_blue_plus)      │
├─────────────────────────────────────────────┤
│      Local Database (SQLite via sqflite)    │
└─────────────────────────────────────────────┘
```

Each layer has a single responsibility and no cross-layer dependencies.

### Core Technologies

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **BLE Protocol** | flutter_blue_plus | Bluetooth Low Energy connectivity |
| **Encryption** | Ed25519 (pointycastle) | User identity and key generation |
| **Database** | SQLite (sqflite) | Local message & peer storage |
| **State** | Flutter Riverpod | Reactive state management |
| **Storage** | FlutterSecureStorage | Secure keypair storage |
| **UI Framework** | Flutter Material 3 | Cross-platform UI (Android/iOS) |

### Mesh Networking Protocol

**Packet Structure:**
```dart
{
  id: UUID,              // Unique packet identifier
  type: 'chat' | 'ack' | 'discovery',  // Message type
  from: String,          // Sender's peer ID
  to: String,            // Recipient or 'BROADCAST'
  ttl: int,              // Time-to-live (hops remaining)
  timestamp: int,        // Unix milliseconds
  payload: Map           // Message content
}
```

**Routing Algorithm:**
- **TTL-bounded flooding**: Messages start with TTL=5 and decrement with each relay
- **Deduplication cache**: In-memory map prevents duplicate relay (10-minute expiry)
- **Deterministic routing**: If packet.to == myId → store locally, else → relay to other peers
- **No coordinators needed**: Each device independently makes relay decisions

### User Identity

On first launch, OffGrid generates:
- **Ed25519 keypair** (32-byte public key, 32-byte private key)
- **User ID**: SHA-256 hash of public key (hex-encoded, 64 characters)
- Keys are stored in FlutterSecureStorage; identity persists across app restarts

---

## 🚀 Current Progress

### ✅ Phase 1 - Identity & Scaffold (COMPLETE)

**What's Done:**
- ✅ Project structure and folder organization
- ✅ Core data models (Message, Peer, Chat)
- ✅ Onboarding screen with identity generation
- ✅ Home screen with welcome UI
- ✅ Ed25519 keypair generation (uses pointycastle)
- ✅ Secure key storage (FlutterSecureStorage)
- ✅ User preference persistence (SharedPreferences)
- ✅ Material dark theme with Material 3
- ✅ Android BLE permissions configured (minSdkVersion 21)
- ✅ flutter analyze: **No issues**

**Current State:**
The app boots, shows onboarding on first launch, and persists user identity across restarts.

---

## 🔜 Planned Phases

### Phase 2 - SQLite Database & Data Access

**Scope:**
- [ ] Implement SqliteHelper singleton for database initialization
- [ ] Create three database tables: `peers`, `chats`, `messages`
- [ ] Build Data Access Objects (DAOs):
  - PeerDAO (upsert, query, list peers)
  - ChatDAO (upsert, query, determine direct chat IDs)
  - MessageDAO (insert, query by chat, update status)
- [ ] Add database indices for message queries
- [ ] Add first database test via UI button

**Expected Outcome:** Offline message and peer storage ready for Phases 3+.

---

### Phase 3 - Packet Model & BLE Transport

**Scope:**
- [ ] Implement Packet class with factory constructors (chat, ack, discovery)
- [ ] Build BleTransport layer:
  - BLE scanning and connection management
  - MTU-aware packet chunking (180-byte limit)
  - Incoming/outgoing packet streams
  - Device list management
- [ ] Handle BLE connection lifecycle (connect, disconnect, error recovery)
- [ ] Implement characteristic notifications for incoming data

**Expected Outcome:** App can discover and connect to nearby BLE devices.

---

### Phase 4 - Mesh Router & Deduplication

**Scope:**
- [ ] Build MeshRouter class:
  - TTL-bounded flooding logic
  - Deduplication cache (in-memory, 10-min expiry)
  - Message relay decision-making
  - Packet delivery to appropriate DAOs
- [ ] Connect Router to BLE transport (incoming packets)
- [ ] Test with multiple devices: verify message relay through 3+ hops

**Expected Outcome:** Multi-hop message delivery across a mesh network.

---

### Phase 5 - Riverpod State Management

**Scope:**
- [ ] Create providers.dart with Riverpod state:
  - myIdProvider (user's own ID)
  - connectedPeersProvider (reactive peer list)
  - messagesProvider (chat messages by ID)
  - chatsProvider (list of conversations)
- [ ] Integrate BleTransport and MeshRouter into providers
- [ ] Update UI screens to use providers instead of direct database calls
- [ ] Handle real-time updates as packets arrive

**Expected Outcome:** Reactive UI that updates as messages and peers change.

---

### Phase 6 - Chat UI Screens

**Scope:**
- [ ] Build PeersScreen: list discovered peers, initiate chats
- [ ] Build ChatListScreen: show active conversations
- [ ] Build ChatDetailScreen: message list, text input, send button
- [ ] Add message status indicators (sending → sent → delivered → read)
- [ ] Update HomeScreen to navigate to chat features

**Expected Outcome:** Fully functional chat interface for peer-to-peer messaging.

---

### Phase 7 - Polish & Testing

**Scope:**
- [ ] Error handling and edge cases
- [ ] Permissions flow (iOS/Android)
- [ ] Connection stability and reconnection logic
- [ ] Battery optimization for BLE scanning
- [ ] Unit tests for model classes
- [ ] Integration tests for routing and DAOs
- [ ] App icon and branding

**Expected Outcome:** Production-ready offline chat app.

---

## 📦 Dependencies

```yaml
flutter_blue_plus: ^1.31.0       # BLE transport
sqflite: ^2.3.3                  # Local database
path: ^1.9.0                     # File paths
flutter_riverpod: ^2.5.1         # State management
flutter_secure_storage: ^9.2.2   # Secure key storage
shared_preferences: ^2.2.3       # User preferences
uuid: ^4.4.0                     # Unique IDs
pointycastle: ^3.9.1             # Ed25519 crypto
permission_handler: ^11.3.1      # Runtime permissions
```

---

## 🛠️ Getting Started

### Prerequisites
- Flutter SDK (3.11+)
- Android SDK (minSdkVersion 21)
- iOS 12.0+ (for BLE support)
- Real device (BLE is not well-supported in emulator)

### Installation

```bash
# Clone and navigate
git clone <repo-url>
cd offgrid

# Get dependencies
flutter pub get

# Run on device
flutter run
```

### First Launch
1. App shows OnboardingScreen
2. Enter a display name (2-30 characters)
3. Tap "Get Started"
4. Keypair is generated and stored
5. HomeScreen displays with welcome message and user ID

---

## 🧪 Testing

```bash
# Run analysis
flutter analyze

# Run tests (future)
flutter test

# Run on specific device
flutter run -d <device-id>
```

---

## 📖 Architecture & Code Organization

```
lib/
├── main.dart                 # App entry point, routing logic
├── ui/
│   ├── screens/             # UI screens
│   │   ├── onboarding_screen.dart
│   │   └── home_screen.dart
│   └── widgets/             # Reusable widgets (future)
├── models/                  # Data models
│   ├── message.dart
│   ├── peer.dart
│   └── chat.dart
├── ble/                     # BLE transport layer
│   └── ble_transport.dart   # (Phase 3)
├── mesh/                    # Mesh routing & packets
│   ├── packet.dart          # (Phase 3)
│   └── mesh_router.dart     # (Phase 4)
├── data/                    # Database & DAOs
│   ├── db_helper.dart       # (Phase 2)
│   ├── peer_dao.dart        # (Phase 2)
│   ├── chat_dao.dart        # (Phase 2)
│   └── message_dao.dart     # (Phase 2)
└── state/                   # Riverpod providers
    └── providers.dart       # (Phase 5)
```

---

## 🤝 Contributing

This is an educational project. Contributions, bug reports, and suggestions are welcome. Please keep code:
- **Well-commented** for learning purposes
- **Layered and separated** by responsibility
- **Tested** before submission

---

## 🎓 Learning Resources

- [Bluetooth Low Energy Overview](https://en.wikipedia.org/wiki/Bluetooth_Low_Energy)
- [flutter_blue_plus Documentation](https://pub.dev/packages/flutter_blue_plus)
- [SQLite with sqflite](https://pub.dev/packages/sqflite)
- [Riverpod State Management](https://riverpod.dev/)
- [Ed25519 Elliptic Curve Cryptography](https://en.wikipedia.org/wiki/Curve25519)

---

**Made for offline, decentralized communication.**
