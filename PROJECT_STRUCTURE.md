# 📁 Project Structure

This document provides an overview of how the React Native Video Screenshot Plugin is organized and how the different components work together.

## 🏗️ Repository Structure

```
react-native-video-screenshot-plugin/
├── 📂 src/                                   # TypeScript source code
│   ├── index.tsx                            # Main plugin API and type definitions
│   └── 📂 __tests__/                        # Unit tests
│       └── index.test.ts                    # API tests with mocked native modules
│
├── 📂 lib/                                   # Compiled JavaScript output (auto-generated)
│   ├── index.js                            # Compiled JavaScript
│   ├── index.d.ts                          # TypeScript declarations
│   └── source maps...
│
├── 📂 ios/                                   # iOS native implementation
│   ├── VideoScreenshotPlugin.swift         # Main iOS plugin implementation
│   ├── VideoScreenshotPlugin.h             # Objective-C header
│   ├── VideoScreenshotPlugin.m             # Objective-C bridge
│   └── VideoScreenshotPlugin-Bridging-Header.h # Swift-ObjC bridge
│
├── 📂 android/                               # Android native implementation
│   ├── build.gradle                        # Android build configuration
│   └── 📂 src/main/
│       ├── AndroidManifest.xml             # Android manifest
│       └── 📂 java/com/videoscreenshotplugin/
│           ├── VideoScreenshotPluginModule.kt    # Main Android implementation
│           └── VideoScreenshotPluginPackage.kt   # React Native package
│
├── 📂 examples/                              # Example implementations
│   ├── 📂 bare/                             # Bare React Native example
│   │   ├── App.tsx                         # Complete example app
│   │   ├── package.json                    # Example dependencies
│   │   ├── metro.config.js                 # Metro bundler configuration
│   │   ├── babel.config.js                 # Babel configuration
│   │   ├── tsconfig.json                   # TypeScript configuration
│   │   ├── android/                        # Android project
│   │   ├── ios/                            # iOS project
│   │   └── README.md                       # Example setup instructions
│   └── 📂 expo-example/                     # Expo example (future)
│
├── 📂 scripts/                               # Build and utility scripts
├── 📄 package.json                          # Main package configuration
├── 📄 tsconfig.json                         # TypeScript compiler configuration
├── 📄 jest.config.js                        # Jest testing configuration
├── 📄 react-native.config.js                # React Native configuration
├── 📄 react-native-video-screenshot-plugin.podspec  # iOS CocoaPods spec
├── 📄 README.md                             # Main documentation
├── 📄 GETTING_STARTED.md                    # Step-by-step setup guide
├── 📄 PROJECT_STRUCTURE.md                  # This file - project organization
├── 📄 CONTRIBUTING.md                       # Contribution guidelines
├── 📄 CHANGELOG.md                          # Version history
└── 📄 LICENSE                               # MIT license
```

## 🔧 Core Components

### TypeScript API Layer (`src/index.tsx`)

The main plugin interface that developers interact with:

```typescript
// Core screenshot functions
export function captureScreenshot(videoRef: VideoRef, options?: ScreenshotOptions): Promise<ScreenshotResult>
export function captureScreenshotWhenReady(videoRef: VideoRef, options?: ScreenshotOptions): Promise<ScreenshotResult>
export function saveScreenshotToLibrary(videoRef: VideoRef, options?: ScreenshotOptions): Promise<ScreenshotResult>
export function saveScreenshotToPath(videoRef: VideoRef, filePath: string, options?: ScreenshotOptions): Promise<ScreenshotResult>

// Video information functions
export function isScreenshotSupported(videoRef: VideoRef): Promise<boolean>
export function getVideoDimensions(videoRef: VideoRef): Promise<{ width: number; height: number }>
export function listAvailableVideos(): Promise<string[]>

// Development & testing functions
export function testMethod(): Promise<TestResult>
export function getModuleInfo(): Promise<ModuleInfo>
export function registerPlayer(playerId: string): Promise<PlayerRegistrationResult>
export function debugListPlayers(): Promise<string[]>

// Type definitions
export interface ScreenshotOptions { ... }
export interface ScreenshotResult { ... }
export interface VideoRef { ... }
export interface ModuleInfo { ... }
export interface TestResult { ... }
export interface PlayerRegistrationResult { ... }
```

**Key features:**

- ✅ Full TypeScript support with comprehensive type definitions
- ✅ Consistent API across all methods
- ✅ Default options handling
- ✅ Error propagation from native modules
- ✅ Debug and testing utilities

### iOS Implementation (`ios/`)

**VideoScreenshotPlugin.swift** - Main iOS implementation:

- Integrates with react-native-video's plugin architecture
- Uses AVFoundation for video frame extraction
- Handles photo library permissions and operations
- Implements image compression and resizing
- Manages multiple video player instances
- Provides debugging and testing capabilities

**VideoScreenshotPlugin.h/.m** - Objective-C bridge:

- Exposes Swift methods to React Native bridge
- Handles method registration and module setup
- Maintains compatibility with RN bridge system

### Android Implementation (`android/`)

**VideoScreenshotPluginModule.kt** - Main Android implementation:

- Integrates with react-native-video's ExoPlayer plugin system
- Uses MediaMetadataRetriever for frame extraction
- Handles Android storage permissions and MediaStore operations
- Implements custom file path saving with directory creation
- Supports different Android API levels and scoped storage
- Provides comprehensive debugging tools

**VideoScreenshotPluginPackage.kt** - React Native package:

- Registers the native module with React Native
- Standard React Native package implementation following current patterns

## 🔄 Plugin Architecture Integration

### react-native-video Plugin System

This plugin leverages react-native-video's plugin architecture:

#### iOS Plugin Integration

```swift
@objc(VideoScreenshotPlugin)
class VideoScreenshotPlugin: NSObject, RCTBridgeModule {
    override init() {
        super.init()
        // Register with ReactNativeVideoManager
        ReactNativeVideoManager.shared().registerPlugin(self)
    }

    // Handle player lifecycle events
    func onVideoPlayerCreated(playerId: String, player: AVPlayer) {
        players[playerId] = player
    }

    func onVideoPlayerDestroyed(playerId: String) {
        players.removeValue(forKey: playerId)
    }
}
```

#### Android Plugin Integration

```kotlin
class VideoScreenshotPluginModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RNVExoplayerPlugin {

    init {
        // Register with ReactNativeVideoManager
        ReactNativeVideoManager.getInstance().registerPlugin(this)
    }

    // Handle ExoPlayer lifecycle events
    override fun onInstanceCreated(id: String, player: ExoPlayer) {
        players[id] = player
    }

    override fun onInstanceRemoved(id: String, player: ExoPlayer) {
        players.remove(id)
    }
}
```

### Video Instance Tracking

The plugin tracks video player instances using their IDs:

1. **Video Component Setup**:

   ```tsx
   <Video
     id="my-video"  // ← Plugin uses this ID to track the player
     source={...}
     ref={videoRef}
   />
   ```

2. **API Usage**:

   ```tsx
   await captureScreenshot({ videoId: 'my-video' }, options);
   ```

3. **Native Lookup**:
   - Plugin looks up the player instance by ID
   - Performs screenshot operation on that specific player
   - Returns detailed result with metadata

## 🛠️ Development Workflow

### 1. TypeScript Development

```bash
# Watch mode for development
npm run watch

# Build for production
npm run build

# Type checking
npm run typecheck

# Linting
npm run lint
npm run lint:fix
```

### 2. Testing

```bash
# Run unit tests
npm test

# Watch mode
npm run test:watch

# Coverage report
npm run test:coverage

# Testing covers:
# - API exports and signatures
# - Default option handling
# - Native module integration
# - Error propagation
```

### 3. Example App Development

```bash
# Work with bare React Native example
cd examples/bare
npm install

# Install iOS pods
npm run pods

# Start Metro bundler
npm run start

# Run on platforms
npm run ios
npm run android
```

### 4. Native Development

#### iOS Development

- Open `examples/bare/ios/bare.xcworkspace` in Xcode
- Modify Swift implementation in `ios/VideoScreenshotPlugin.swift`
- Test with example app in iOS Simulator or device
- Use Xcode debugger for native debugging

#### Android Development

- Open `examples/bare/android/` in Android Studio
- Modify Kotlin implementation in `android/src/main/java/com/videoscreenshotplugin/`
- Test with example app in Android Emulator or device
- Use Android Studio debugger and logcat for debugging

## 📦 Build and Distribution

### Library Build Process

1. **TypeScript Compilation**: `src/` → `lib/`
2. **Type Definition Generation**: `.d.ts` files with source maps
3. **Source Maps**: For debugging compiled JavaScript

### Package Contents

The published npm package includes:

- `lib/` - Compiled JavaScript and comprehensive type definitions
- `src/` - TypeScript source (for source maps and debugging)
- `ios/` - iOS native implementation (Swift + ObjC bridge)
- `android/` - Android native implementation (Kotlin)
- `react-native-video-screenshot-plugin.podspec` - iOS CocoaPods spec
- Documentation files and examples

### Example Distribution

The examples serve multiple purposes:

- ✅ **Integration test** - Verifies plugin works in real React Native apps
- ✅ **Documentation** - Shows proper usage patterns and best practices
- ✅ **Quick start** - Developers can copy and modify for their needs
- ✅ **Testing platform** - For manual testing during development
- ✅ **Plugin development reference** - Demonstrates RNV plugin patterns

## 🔗 Integration Points

### react-native-video Integration

- **Plugin Registration**: Automatic registration with RNV plugin system
- **Player Lifecycle**: Handles player creation/destruction events seamlessly
- **Video ID Mapping**: Maps React Native video IDs to native player instances
- **Cross-Platform Consistency**: Unified behavior across iOS and Android

### React Native Bridge Integration

- **Promise-based API**: All methods return Promises for async operations
- **Error Handling**: Native errors properly propagated to JavaScript
- **Type Safety**: Full TypeScript support with comprehensive interfaces
- **Performance**: Optimized bridge communication with minimal overhead

### Platform APIs

- **iOS**: AVFoundation, Photos.framework, UIKit, FileManager
- **Android**: MediaMetadataRetriever, MediaStore, ExoPlayer, File I/O
- **Permissions**: Proper handling of photo library and storage permissions

## 🧩 Extension Points

### Adding New Features

1. **TypeScript API**: Add method signature to `src/index.tsx`
2. **iOS Implementation**: Add method to `VideoScreenshotPlugin.swift`
3. **Android Implementation**: Add method to `VideoScreenshotPluginModule.kt`
4. **Tests**: Add test cases to `src/__tests__/index.test.ts`
5. **Documentation**: Update README, GETTING_STARTED, and examples
6. **Example**: Add usage example in `examples/bare/App.tsx`

### Custom Options

New screenshot options can be added by:

1. Extending `ScreenshotOptions` interface
2. Implementing handling in both native platforms
3. Adding default values and validation
4. Adding tests and documentation
5. Updating examples

### Plugin Development Patterns

This plugin demonstrates:

- **Plugin Registration**: How to register with ReactNativeVideoManager
- **Cross-Platform APIs**: Maintaining consistent interfaces
- **Error Handling**: Comprehensive error management patterns
- **Type Safety**: Full TypeScript integration
- **Testing**: Unit and integration testing strategies
- **Documentation**: Comprehensive developer resources

## 📚 Related Documentation

- [README.md](README.md) - Main documentation and comprehensive API reference
- [GETTING_STARTED.md](GETTING_STARTED.md) - Step-by-step setup guide for new users
- [examples/bare/README.md](examples/bare/README.md) - Example app setup and usage
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines and development setup
- [CHANGELOG.md](CHANGELOG.md) - Version history and release notes
- [react-native-video docs](https://github.com/react-native-video/react-native-video) - Base video player documentation

---

This structure provides a solid foundation for a production-ready React Native video screenshot plugin while maintaining clean separation of concerns, comprehensive documentation, and serving as an educational resource for the React Native Video plugin ecosystem.
