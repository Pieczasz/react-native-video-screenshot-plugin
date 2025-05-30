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
│   ├── VideoScreenshotPlugin.mm            # Objective-C bridge
│   └── VideoScreenshotPlugin-Bridging-Header.h
│
├── 📂 android/                               # Android native implementation
│   ├── build.gradle                        # Android build configuration
│   └── 📂 src/main/
│       ├── AndroidManifest.xml             # Android manifest
│       └── 📂 java/com/videoscreenshotplugin/
│           ├── VideoScreenshotPluginModule.kt    # Main Android implementation
│           └── VideoScreenshotPluginPackage.kt   # React Native package
│
├── 📂 example/                               # Example implementation
│   ├── App.tsx                             # Complete example app
│   ├── package.json                        # Example dependencies
│   ├── metro.config.js                     # Metro bundler configuration
│   ├── babel.config.js                     # Babel configuration with module resolver
│   ├── tsconfig.json                       # TypeScript configuration
│   └── README.md                           # Example setup instructions
│
├── 📄 package.json                          # Main package configuration
├── 📄 tsconfig.json                         # TypeScript compiler configuration
├── 📄 jest.config.js                        # Jest testing configuration
├── 📄 react-native.config.js                # React Native configuration
├── 📄 react-native-video-screenshot-plugin.podspec  # iOS CocoaPods spec
├── 📄 setup-example.sh                      # Automated example setup script
├── 📄 README.md                             # Main documentation
├── 📄 GETTING_STARTED.md                    # Step-by-step setup guide
├── 📄 CONTRIBUTING.md                       # Contribution guidelines
├── 📄 CHANGELOG.md                          # Version history
└── 📄 LICENSE                               # MIT license
```

## 🔧 Core Components

### TypeScript API Layer (`src/index.tsx`)

The main plugin interface that developers interact with:

```typescript
// Main exported functions
export function captureScreenshot(videoRef: VideoRef, options?: ScreenshotOptions): Promise<ScreenshotResult>
export function saveScreenshotToLibrary(videoRef: VideoRef, options?: ScreenshotOptions): Promise<ScreenshotResult>
export function saveScreenshotToPath(videoRef: VideoRef, filePath: string, options?: ScreenshotOptions): Promise<ScreenshotResult>
export function isScreenshotSupported(videoRef: VideoRef): Promise<boolean>
export function getVideoDimensions(videoRef: VideoRef): Promise<{ width: number; height: number }>

// Type definitions
export interface ScreenshotOptions { ... }
export interface ScreenshotResult { ... }
export interface VideoRef { ... }
```

**Key features:**
- ✅ Full TypeScript support with comprehensive type definitions
- ✅ Consistent API across all methods
- ✅ Default options handling
- ✅ Error propagation from native modules

### iOS Implementation (`ios/`)

**VideoScreenshotPlugin.swift** - Main iOS implementation:
- Integrates with react-native-video's plugin architecture
- Uses AVFoundation for video frame extraction
- Handles photo library permissions
- Implements image compression and resizing
- Manages multiple video player instances

**VideoScreenshotPlugin.mm** - Objective-C bridge:
- Exposes Swift methods to React Native bridge
- Handles method registration and module setup

### Android Implementation (`android/`)

**VideoScreenshotPluginModule.kt** - Main Android implementation:
- Integrates with react-native-video's ExoPlayer plugin system
- Uses MediaMetadataRetriever for frame extraction
- Handles Android storage permissions
- Implements MediaStore integration
- Supports custom file path saving

**VideoScreenshotPluginPackage.kt** - React Native package:
- Registers the native module with React Native
- Standard React Native package implementation

## 🔄 Plugin Architecture Integration

### react-native-video Plugin System

This plugin leverages react-native-video's plugin architecture:

#### iOS Plugin Integration
```swift
@objc(VideoScreenshotPlugin)
class VideoScreenshotPlugin: RNVAVPlayerPlugin {
    override func onInstanceCreated(id: String, player: AVPlayer) {
        // Track video player instances
        players[id] = player
    }
    
    override func onInstanceRemoved(id: String, player: AVPlayer) {
        // Clean up when video is removed
        players.removeValue(forKey: id)
    }
}
```

#### Android Plugin Integration
```kotlin
class VideoScreenshotPluginModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RNVExoplayerPlugin {
    
    override fun onInstanceCreated(id: String, player: ExoPlayer) {
        // Track ExoPlayer instances
        players[id] = player
    }
    
    override fun onInstanceRemoved(id: String, player: ExoPlayer) {
        // Clean up when video is removed
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
   await captureScreenshot({ videoId: 'my-video' }, options)
   ```

3. **Native Lookup**:
   - Plugin looks up the player instance by ID
   - Performs screenshot operation on that specific player

## 🛠️ Development Workflow

### 1. TypeScript Development
```bash
# Watch mode for development
npm run watch

# Build for production
npm run build

# Type checking
npm run typecheck
```

### 2. Testing
```bash
# Run unit tests
npm test

# Watch mode
npm run test:watch

# Testing covers:
# - API exports and signatures
# - Default option handling
# - Native module integration
# - Error propagation
```

### 3. Example App Development
```bash
# Setup new example project
npm run example:setup

# Or work with existing example
cd example
npm install
npm run start

# iOS
npm run ios

# Android  
npm run android
```

### 4. Native Development

#### iOS Development
- Open `ios/VideoScreenshotPlugin.xcworkspace` in Xcode
- Modify Swift implementation
- Test with example app in iOS Simulator

#### Android Development
- Open `android/` in Android Studio
- Modify Kotlin implementation  
- Test with example app in Android Emulator

## 📦 Build and Distribution

### Library Build Process
1. **TypeScript Compilation**: `src/` → `lib/`
2. **Type Definition Generation**: `.d.ts` files
3. **Source Maps**: For debugging

### Package Contents
The published npm package includes:
- `lib/` - Compiled JavaScript and type definitions
- `src/` - TypeScript source (for source maps)
- `ios/` - iOS native implementation
- `android/` - Android native implementation
- `react-native-video-screenshot-plugin.podspec` - iOS CocoaPods spec
- Documentation files

### Example Distribution
The example serves as:
- ✅ **Integration test** - Verifies plugin works in real app
- ✅ **Documentation** - Shows proper usage patterns
- ✅ **Quick start** - Developers can copy and modify
- ✅ **Testing platform** - For manual testing during development

## 🔗 Integration Points

### react-native-video Integration
- **Plugin Registration**: Automatic registration with RNV plugin system
- **Player Lifecycle**: Handles player creation/destruction events
- **Video ID Mapping**: Maps React Native video IDs to native player instances

### React Native Bridge Integration
- **Promise-based API**: All methods return Promises
- **Error Handling**: Native errors propagated to JavaScript
- **Type Safety**: Full TypeScript support

### Platform APIs
- **iOS**: AVFoundation, Photos.framework, UIKit
- **Android**: MediaMetadataRetriever, MediaStore, ExoPlayer

## 🧩 Extension Points

### Adding New Features
1. **TypeScript API**: Add method signature to `src/index.tsx`
2. **iOS Implementation**: Add method to `VideoScreenshotPlugin.swift`
3. **Android Implementation**: Add method to `VideoScreenshotPluginModule.kt`
4. **Tests**: Add test cases to `src/__tests__/index.test.ts`
5. **Documentation**: Update README and examples

### Custom Options
New screenshot options can be added by:
1. Extending `ScreenshotOptions` interface
2. Implementing handling in both native platforms
3. Adding tests and documentation

## 📚 Related Documentation

- [README.md](README.md) - Main documentation and API reference
- [GETTING_STARTED.md](GETTING_STARTED.md) - Step-by-step setup guide
- [example/README.md](example/README.md) - Example app setup
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
- [react-native-video docs](https://github.com/react-native-video/react-native-video) - Base video player documentation

---

This structure provides a solid foundation for a production-ready React Native video screenshot plugin while maintaining clean separation of concerns and comprehensive documentation. 