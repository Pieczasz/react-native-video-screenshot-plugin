# React Native Video Screenshot Plugin - iOS Implementation

## Overview

This directory contains the iOS implementation of the React Native Video Screenshot Plugin. The plugin extends [react-native-video](https://github.com/react-native-video/react-native-video) by providing screenshot capture functionality from active video players on iOS using AVFoundation.

## What This Plugin Does

The plugin allows React Native applications to:

- Capture screenshots from currently playing videos
- Save screenshots to device photo library
- Save screenshots to custom file paths
- Get video dimensions and playback information
- Check screenshot support status

## Architecture Overview

### Core Integration Strategy

The plugin integrates with react-native-video through the **Plugin System**:

```
React Native App Startup
            ↓
VideoScreenshotPlugin.init()
            ↓
Register with ReactNativeVideoManager.shared
            ↓
react-native-video calls plugin lifecycle methods:
            ↓
onInstanceCreated() → Store AVPlayer reference
onInstanceRemoved() → Clean up player reference
            ↓
JavaScript calls screenshot methods
            ↓
Plugin uses stored player references to capture frames
```

### Core Components

#### 1. VideoScreenshotPlugin.swift

The main Swift class that implements screenshot functionality and integrates with react-native-video.

**Key Features:**

- Inherits from `RNVPlugin` protocol for automatic integration
- Manages thread-safe registry of active AVPlayer instances
- Provides multiple screenshot capture methods (base64, photo library, file system)
- Uses Grand Central Dispatch for asynchronous operations
- Implements robust frame extraction using AVAssetImageGenerator

**Public API Methods:**

```swift
@objc public func captureScreenshot(...)         // Returns base64
@objc public func saveScreenshotToLibrary(...)   // Saves to photo library
@objc public func saveScreenshotToPath(...)      // Saves to file path
@objc public func isScreenshotSupported(...)     // Checks support
@objc public func getVideoDimensions(...)        // Returns video size
@objc public func listAvailableVideos(...)       // Lists active players
```

#### 2. VideoScreenshotPlugin.mm

Objective-C bridge file that exposes Swift methods to React Native.

**Key Features:**

- Uses `RCT_EXTERN_MODULE` and `RCT_EXTERN_METHOD` macros
- Handles Promise-based asynchronous method calls
- Maintains type safety between JavaScript and native code

#### 3. VideoScreenshotPlugin-Bridging-Header.h

Bridging header enabling Swift access to React Native's Objective-C APIs.

## Implementation Details

### Player Lifecycle Management

```swift
// When react-native-video creates a new AVPlayer instance
override public func onInstanceCreated(id: String, player: Any) {
    guard let avPlayer = player as? AVPlayer else { return }

    playerQueue.async(flags: .barrier) {
        self.players[id] = avPlayer
    }

    emitVideoPlayerReadyEvent(id)
}

// When react-native-video destroys an AVPlayer instance
override public func onInstanceRemoved(id: String, player: Any) {
    playerQueue.async(flags: .barrier) {
        self.players.removeValue(forKey: id)
    }
}
```

### Frame Extraction Strategy

The plugin uses **AVAssetImageGenerator** for high-quality frame extraction:

```swift
private func extractVideoFrame(from player: AVPlayer) throws -> UIImage? {
    let imageGenerator = getOrCreateImageGenerator(for: currentItem.asset)

    // Primary extraction attempt
    let cgImage = try imageGenerator.copyCGImage(at: currentTime, actualTime: nil)
    return UIImage(cgImage: cgImage)
}
```

**Fallback Strategy:**

1. **Precise Time Extraction** → **Relaxed Time Tolerance**
2. **Current Frame** → **Nearest Available Frame**
3. **Frame Extraction** → **Enhanced Placeholder**

### Thread Safety Design

The implementation uses Grand Central Dispatch for proper thread management:

- **Main Queue**: Player state access, UI operations, plugin lifecycle
- **Player Queue**: Thread-safe player registry operations (concurrent with barriers)
- **Background Queue**: Screenshot processing and image operations
- **Photo Library Queue**: System-managed photo library operations

### Screenshot Output Modes

```swift
private enum ScreenshotOutputMode {
    case base64Only
    case saveToLibrary
    case saveToPath(String)
}
```

## Configuration Requirements

### iOS Dependencies

The plugin requires these iOS frameworks:

- **AVFoundation**: Core video playback and frame extraction
- **Photos**: Photo library integration
- **UIKit**: Image processing and UI operations
- **Foundation**: File system and data handling

### Required Permissions

#### Photo Library Access

```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>This app needs access to photo library to save video screenshots</string>

<key>NSPhotoLibraryAddUsageDescription</key>
<string>This app needs access to save video screenshots to your photo library</string>
```

## Plugin Registration Process

### Automatic Registration

```swift
override init() {
    super.init()

    // Register with react-native-video's plugin system
    DispatchQueue.main.async {
        self.registerWithVideoManager()
    }
}

private func registerWithVideoManager() {
    #if canImport(react_native_video)
    ReactNativeVideoManager.shared.registerPlugin(plugin: self)
    #endif
}
```

### Plugin Protocol Conformance

```swift
class VideoScreenshotPlugin: RNVPlugin, RCTBridgeModule {
    // Automatically receives player lifecycle callbacks from react-native-video
    override public func onInstanceCreated(id: String, player: Any) { ... }
    override public func onInstanceRemoved(id: String, player: Any) { ... }
}
```

## Key Implementation Features

### 1. **Cached Image Generators**

Reuses `AVAssetImageGenerator` instances for better performance:

```swift
private lazy var imageGeneratorCache: NSCache<NSString, AVAssetImageGenerator> = {
    let cache = NSCache<NSString, AVAssetImageGenerator>()
    cache.countLimit = 10
    return cache
}()
```

### 2. **Modern Image Processing**

Uses `UIGraphicsImageRenderer` for efficient image operations:

```swift
let renderer = UIGraphicsImageRenderer(size: targetSize)
return renderer.image { context in
    image.draw(in: CGRect(origin: .zero, size: targetSize))
}
```

### 3. **Memory Management**

Automatic cleanup and memory pressure handling:

```swift
NotificationCenter.default.addObserver(
    forName: UIApplication.didReceiveMemoryWarningNotification,
    object: nil,
    queue: .main
) { [weak self] _ in
    self?.imageGeneratorCache.removeAllObjects()
}
```

## Error Handling

The plugin implements comprehensive error handling:

```swift
enum VideoScreenshotError: Error {
    case playerNotFound(String)
    case invalidPlayer
    case screenshotFailed(String)
    case permissionDenied
}
```

## Integration Example

```javascript
import VideoScreenshotPlugin from 'react-native-video-screenshot-plugin';

// Capture screenshot as base64
const result = await VideoScreenshotPlugin.captureScreenshot(videoId, {
  format: 'jpeg',
  quality: 0.8,
  maxWidth: 1920,
});

// Save to photo library
await VideoScreenshotPlugin.saveScreenshotToLibrary(videoId, options);
```

## iOS Version Support

- **Minimum iOS**: 12.0+
- **React Native**: 0.60+
- **Swift**: 5.0+

## Debugging

### View Logs

```bash
# iOS Simulator
xcrun simctl spawn booted log stream --predicate 'eventMessage contains "VideoScreenshotPlugin"'

# Physical Device
idevicesyslog | grep VideoScreenshotPlugin
```

### Debug Methods

- `debugListPlayers()` - Lists all registered players
- `listAvailableVideos()` - Shows available video instances
- `getModuleInfo()` - Returns plugin status and information

## Troubleshooting

| Issue              | Solution                                             |
| ------------------ | ---------------------------------------------------- |
| Plugin not working | Ensure react-native-video is properly installed      |
| Player not found   | Check video ID matches JavaScript component          |
| Black screenshots  | Wait for video to load before capturing              |
| Permission denied  | Add photo library permissions to Info.plist          |
| Build failures     | Ensure AVFoundation and Photos frameworks are linked |

## Contributing

When contributing to the iOS implementation:

1. Follow Swift coding conventions
2. Add comprehensive documentation
3. Include proper error handling
4. Add appropriate tests
5. Update this README for architectural changes
