# React Native Video Screenshot Plugin - iOS Implementation

## Overview

This directory contains the iOS implementation of the React Native Video Screenshot Plugin. The plugin extends [react-native-video](https://github.com/react-native-video/react-native-video) by providing screenshot capture functionality from active video players on iOS using AVFoundation.

## Architecture

### Core Components

#### 1. VideoScreenshotPlugin.swift

The main Swift class that implements the screenshot functionality and integrates with react-native-video.

**Key Features:**

- Inherits from `RNVAVPlayerPlugin` protocol for automatic integration with react-native-video
- Manages a thread-safe registry of active AVPlayer instances
- Provides multiple screenshot capture methods (base64, photo library, file system)
- Uses Grand Central Dispatch for asynchronous operations
- Implements robust frame extraction with fallback strategies using AVAssetImageGenerator

**Public API Methods:**

- `captureScreenshot()` - Returns screenshot as base64
- `saveScreenshotToLibrary()` - Saves to device photo library
- `saveScreenshotToPath()` - Saves to custom file path
- `isScreenshotSupported()` - Checks if screenshot is possible
- `getVideoDimensions()` - Returns video dimensions
- `listAvailableVideos()` - Lists active video players

#### 2. VideoScreenshotPlugin.mm

The Objective-C bridge file that exposes Swift methods to React Native.

**Key Features:**

- Uses `RCT_EXTERN_MODULE` and `RCT_EXTERN_METHOD` macros
- Provides comprehensive documentation for each exposed method
- Handles Promise-based asynchronous method calls
- Maintains type safety between JavaScript and native code

#### 3. VideoScreenshotPlugin-Bridging-Header.h

The bridging header that allows Swift code to access React Native's Objective-C APIs.

**Includes:**

- React Native bridge module interfaces
- Event emitter capabilities
- UI manager access
- Utility functions

### Integration with react-native-video

The plugin integrates with react-native-video through the plugin system:

```
React Native App Startup
            ↓
VideoScreenshotPlugin.init()
            ↓
Register with ReactNativeVideoManager.shared
            ↓
react-native-video calls plugin lifecycle methods:
            ↓
onInstanceCreated() → Store player reference
onInstanceRemoved() → Clean up player reference
            ↓
JavaScript calls screenshot methods
            ↓
Plugin uses stored player references to capture frames
```

## Implementation Details

### Player Lifecycle Management

```swift
// When react-native-video creates a new AVPlayer instance
override func onInstanceCreated(id: String, player: AVPlayer) {
    playerQueue.sync {
        players[id] = player
    }
    emitVideoPlayerReadyEvent(playerId: id)
}

// When react-native-video destroys an AVPlayer instance
override func onInstanceRemoved(id: String, player: AVPlayer) {
    playerQueue.sync {
        players.removeValue(forKey: id)
    }
}
```

### Frame Extraction Strategy

The plugin uses AVAssetImageGenerator for high-quality frame extraction:

1. **AVAssetImageGenerator** (Primary)

   - Uses `generateCGImagesAsynchronously` for non-blocking extraction
   - Applies preferred track transform for correct orientation
   - Supports precise time tolerance for accurate frame timing
   - Handles video transformations (rotation, scaling) automatically

2. **Fallback Strategy**
   - Relaxes time tolerance if precise extraction fails
   - Provides graceful degradation for difficult video formats
   - Comprehensive error reporting for debugging

### Screenshot Output Modes

```swift
private enum ScreenshotOutputMode {
    case base64Only
    case saveToLibrary
    case saveToPath(String)
}
```

### Thread Safety

The iOS implementation uses Grand Central Dispatch for thread management:

- **Main Queue**: Player state access, UI operations, plugin lifecycle
- **Player Queue**: Thread-safe player registry operations (serial)
- **Screenshot Queue**: Concurrent screenshot processing operations
- **Photo Library Queue**: System-managed photo library operations

### Error Handling

The plugin implements comprehensive error handling:

- Player not found: Returns descriptive error with available players
- Frame extraction failure: Falls back to relaxed time tolerance
- Permission issues: Handles photo library authorization gracefully
- File system errors: Provides specific error messages with paths

## Configuration

### iOS-Specific Dependencies

The plugin uses iOS system frameworks:

- **AVFoundation**: Core video playback and frame extraction
- **Photos**: Photo library integration for gallery saves
- **UIKit**: Image processing and format conversion
- **Foundation**: File system operations and data handling

### Permissions

#### Photo Library Access (NSPhotoLibraryUsageDescription)

Required for `saveScreenshotToLibrary()` method:

```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>This app needs access to photo library to save video screenshots</string>
```

#### Optional: Photo Library Add Usage (NSPhotoLibraryAddUsageDescription)

For iOS 11+, more specific permission:

```xml
<key>NSPhotoLibraryAddUsageDescription</key>
<string>This app needs access to save video screenshots to your photo library</string>
```

## Plugin Registration

### Automatic Registration

The plugin automatically registers itself during initialization:

```swift
override init() {
    super.init()
    ReactNativeVideoManager.shared.registerPlugin(plugin: self)
}

deinit {
    ReactNativeVideoManager.shared.unregisterPlugin(plugin: self)
}
```

### Plugin Protocol Conformance

```swift
class VideoScreenshotPlugin: RNVAVPlayerPlugin {
    // Inherits from react-native-video's plugin protocol
    // Automatically receives player lifecycle callbacks
}
```

## Performance Considerations

### Memory Management

- **Automatic Reference Counting**: Swift handles memory automatically
- **Queue-based Operations**: Prevents blocking of main thread
- **Image Data Cleanup**: Releases image data after processing
- **Player Registry**: Thread-safe weak references to prevent retain cycles

### Screenshot Quality

- **AVAssetImageGenerator Configuration**: Optimized for quality and performance
- **Image Format Support**: Both JPEG and PNG with configurable compression
- **Maximum Size Limits**: Prevents memory issues with very large videos
- **Transform Handling**: Proper orientation and aspect ratio preservation

### Background Processing

All screenshot operations use background queues:

```swift
screenshotQueue.async { [weak self] in
    // Heavy processing on background queue
    let result = processScreenshot()

    DispatchQueue.main.async {
        // Return to main queue for completion
        resolve(result)
    }
}
```

## Debugging

### Logging

The plugin provides comprehensive logging with the tag `VideoScreenshotPlugin`:

```bash
# View iOS simulator logs
xcrun simctl spawn booted log stream --predicate 'processImagePath contains "VideoScreenshotPlugin"'

# View device logs (requires device connection)
idevicesyslog | grep VideoScreenshotPlugin
```

### Debug Methods

- `debugListPlayers()` - Lists all registered players
- `listAvailableVideos()` - Shows available video instances
- Detailed error messages with context
- Performance timing information

### Common Issues

| Problem                 | Cause                            | Solution                                         |
| ----------------------- | -------------------------------- | ------------------------------------------------ |
| Plugin not working      | react-native-video not installed | Install and link react-native-video properly     |
| Player not found        | Wrong video ID                   | Check ID matches JavaScript component            |
| Black/empty screenshots | Video not ready                  | Wait for video to load before capturing          |
| Permission denied       | Missing photo library permission | Add NSPhotoLibraryUsageDescription to Info.plist |
| Build failures          | Missing framework                | Ensure AVFoundation and Photos are linked        |
| Memory warnings         | Large image generation           | Reduce maxWidth/maxHeight or compression quality |

## Testing

### Unit Testing

The plugin can be tested with:

- Player lifecycle management scenarios
- Frame extraction with various video formats
- Error handling edge cases
- Permission flow testing

### Integration Testing

Test with various video sources:

- Local files (file://)
- Remote URLs (http/https)
- Bundled resources
- Different video formats (MP4, MOV, M4V)
- Various codecs (H.264, H.265, VP9)

## Best Practices

### For Plugin Development

1. **Always handle errors gracefully with descriptive messages**
2. **Use appropriate dispatch queues for operations**
3. **Implement proper memory management**
4. **Provide comprehensive logging for debugging**
5. **Follow iOS design patterns and conventions**

### For Performance

1. **Use background queues for heavy operations**
2. **Configure AVAssetImageGenerator optimally**
3. **Implement proper cancellation for long operations**
4. **Cache frequently accessed data appropriately**

### For Compatibility

1. **Support multiple react-native-video versions**
2. **Handle iOS version differences gracefully**
3. **Test across different device capabilities**
4. **Provide fallbacks for older iOS versions**

## iOS Version Support

### Minimum Requirements

- **iOS 12.0+**: For AVAssetImageGenerator features
- **React Native 0.60+**: For autolinking support
- **Swift 5.0+**: For modern Swift features

### Framework Compatibility

- **AVFoundation**: Core functionality available on all supported iOS versions
- **Photos**: Photo library integration (iOS 8+)
- **Grand Central Dispatch**: Threading and concurrency

## Contributing

When contributing to the iOS implementation:

1. **Follow Swift coding conventions and style guides**
2. **Add comprehensive documentation and comments**
3. **Include proper error handling for all scenarios**
4. **Add appropriate unit and integration tests**
5. **Update this README if architectural changes are made**

## Troubleshooting

### Debug Commands

```bash
# View real-time logs from iOS simulator
xcrun simctl spawn booted log stream --level debug --predicate 'processImagePath contains "YourAppName"'

# Monitor plugin activity specifically
xcrun simctl spawn booted log stream --predicate 'eventMessage contains "VideoScreenshotPlugin"'

# Check photo library permissions
xcrun simctl privacy booted grant photos com.yourapp.bundle.id
```

### Performance Monitoring

```bash
# Monitor memory usage
instruments -t "Allocations" -D /tmp/allocations.trace YourApp.app

# Profile image generation performance
instruments -t "Time Profiler" -D /tmp/profiler.trace YourApp.app
```

## Advanced Configuration

### Custom Image Generator Settings

For specialized use cases, you can modify the image generator configuration:

```swift
// Example: High precision frame extraction
imageGenerator.requestedTimeToleranceAfter = CMTime.zero
imageGenerator.requestedTimeToleranceBefore = CMTime.zero

// Example: Lower memory usage
imageGenerator.maximumSize = CGSize(width: 1920, height: 1080)
```

### Error Recovery Strategies

The plugin implements multiple fallback strategies:

1. **Precise Time Extraction** → **Relaxed Time Tolerance**
2. **Original Video Size** → **Scaled Down Version**
3. **Current Frame** → **Nearest Available Frame**

This ensures maximum compatibility across different video formats and playback states.
