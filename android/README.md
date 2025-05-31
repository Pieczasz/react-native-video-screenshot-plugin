# React Native Video Screenshot Plugin - Android Implementation

## Overview

This directory contains the Android implementation of the React Native Video Screenshot Plugin. The plugin extends [react-native-video](https://github.com/react-native-video/react-native-video) by providing screenshot capture functionality from active video players.

## Architecture

### Core Components

#### 1. VideoScreenshotPluginModule.kt

The main native module that implements the screenshot functionality and integrates with react-native-video as a plugin.

**Key Features:**

- Implements `RNVExoplayerPlugin` interface for automatic integration with react-native-video
- Manages a registry of active ExoPlayer instances
- Provides multiple screenshot capture methods (base64, file system, media library)
- Uses coroutines for asynchronous operations
- Implements robust frame extraction with multiple fallback strategies

**Public API Methods:**

- `captureScreenshot()` - Returns screenshot as base64
- `saveScreenshotToLibrary()` - Saves to device gallery
- `saveScreenshotToPath()` - Saves to custom file path
- `isScreenshotSupported()` - Checks if screenshot is possible
- `getVideoDimensions()` - Returns video dimensions
- `listAvailableVideos()` - Lists active video players

#### 2. VideoScreenshotPluginPackage.kt

The React Native package that registers the plugin with both React Native and react-native-video.

**Key Features:**

- Dual registration strategy (direct + reflection fallback)
- Comprehensive error handling and logging
- Version compatibility checks
- Graceful degradation if react-native-video is not available

### Integration with react-native-video

The plugin integrates with react-native-video through the plugin system:

```
React Native App Startup
            ↓
VideoScreenshotPluginPackage.createNativeModules()
            ↓
Register with ReactNativeVideoManager
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

```kotlin
// When react-native-video creates a new ExoPlayer instance
override fun onInstanceCreated(id: String, player: ExoPlayer) {
    playerRegistry[id] = player
    emitVideoPlayerReadyEvent(id)
}

// When react-native-video destroys an ExoPlayer instance
override fun onInstanceRemoved(id: String, player: ExoPlayer) {
    playerRegistry.remove(id)
}
```

### Frame Extraction Strategy

The plugin uses a multi-tier approach for frame extraction:

1. **MediaMetadataRetriever** (Primary)

   - Extracts actual video frames
   - Supports multiple extraction options
   - Handles various URI schemes (http, file, content)

2. **Enhanced Placeholder** (Fallback)
   - Creates visually appealing placeholder when extraction fails
   - Includes video information and current playback state
   - Maintains video aspect ratio

### Screenshot Output Modes

```kotlin
sealed class ScreenshotOutputMode {
    object BASE64_ONLY : ScreenshotOutputMode()
    object SAVE_TO_LIBRARY : ScreenshotOutputMode()
    data class SAVE_TO_PATH(val filePath: String) : ScreenshotOutputMode()
}
```

### Thread Safety

- **Main Thread**: Player state access, UI operations
- **IO Thread**: File operations, image processing
- **Background Thread**: Frame extraction, compression

### Error Handling

The plugin implements comprehensive error handling - so you don't have to worry ;)

- Player not found: Returns descriptive error with available players
- Frame extraction failure: Falls back to enhanced placeholder
- File system errors: Provides specific error messages
- Permission issues: Handled with user-friendly messages

## Configuration

### Gradle Configuration

The plugin is configured in `build.gradle` with:

- **Kotlin Support**: Latest Kotlin version with coroutines
- **Media3 Dependencies**: ExoPlayer and related libraries
- **Android SDK**: Minimum API 21, Target API 33+
- **Build Tools**: Android Gradle Plugin 8.0+

### Key Dependencies

```gradle
implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
implementation "androidx.media3:media3-exoplayer:$media3Version"
implementation project(':react-native-video')
```

## Build Configuration

### Properties (gradle.properties)

```properties
VideoPluginSample_kotlinVersion=1.8.0
VideoPluginSample_minSdkVersion=21
VideoPluginSample_compileSdkVersion=33
VideoPluginSample_targetSdkVersion=33
VideoPluginSample_media3Version=1.3.1
```

### Namespace Configuration

The plugin uses AGP 7.3+ namespace feature:

```gradle
android {
    namespace = "com.videoscreenshotplugin"
}
```

## Plugin Registration

### Direct Registration (Preferred)

```kotlin
val videoManager = ReactNativeVideoManager.getInstance()
videoManager.registerPlugin(pluginModule)
```

### Reflection Fallback

```kotlin
val videoManagerClass = Class.forName("com.brentvatne.react.ReactNativeVideoManager")
val getInstance = videoManagerClass.getMethod("getInstance")
val instance = getInstance.invoke(null)
val registerPlugin = videoManagerClass.getMethod("registerPlugin", pluginInterface)
registerPlugin.invoke(instance, pluginModule)
```

## Performance Considerations

### Memory Management

- Bitmap recycling after use
- Proper MediaMetadataRetriever cleanup
- Coroutine scope with SupervisorJob
- ConcurrentHashMap for thread-safe player registry

### Screenshot Quality

- Configurable JPEG quality (0-100)
- PNG support for lossless screenshots
- Aspect ratio preservation during resizing
- Efficient bitmap scaling algorithms

### Background Processing

All screenshot operations are performed on background threads to avoid blocking the UI:

```kotlin
screenshotScope.launch {
    // Background processing
    val result = withContext(Dispatchers.IO) {
        // Heavy operations here
    }
    // Return to main thread for completion
}
```

## Debugging

### Logging

The plugin provides comprehensive logging with the tag `VideoScreenshotPlugin`:

```bash
adb logcat | grep VideoScreenshotPlugin
```

### Debug Methods

- `debugListPlayers()` - Lists all registered players
- `listAvailableVideos()` - Shows available video instances
- Detailed error messages with context

### Common Issues

| Problem            | Cause                            | Solution                                |
| ------------------ | -------------------------------- | --------------------------------------- |
| Plugin not working | react-native-video not installed | Install react-native-video dependency   |
| Player not found   | Wrong video ID                   | Check ID matches JavaScript component   |
| Black screenshots  | Video not ready                  | Wait for video to load before capturing |
| Permission denied  | Missing storage permission       | Request WRITE_EXTERNAL_STORAGE          |
| Build failures     | Version mismatch                 | Check gradle.properties versions        |

## Testing

### Unit Testing

The plugin includes comprehensive tests for:

- Player lifecycle management
- Frame extraction methods
- Error handling scenarios
- Configuration parsing

### Integration Testing

Test with various video sources:

- Local files (file://)
- Remote URLs (http/https)
- Content URIs (content://)
- Different video formats and codecs

## Best Practices

### For Plugin Development

1. **Handle errors gracefully**
2. **Use appropriate thread contexts**
3. **Clean up resources properly**
4. **Provide comprehensive logging**
5. **Document public APIs thoroughly**

### For Performance

1. **Recycle bitmaps after use**
2. **Use background threads for heavy operations**
3. **Implement proper cancellation**
4. **Cache frequently used data**

### For Compatibility

1. **Support multiple react-native-video versions**
2. **Handle missing dependencies gracefully**
3. **Use reflection for optional integrations**
4. **Test across different Android versions**

## Contributing

When contributing to the Android implementation:

1. **Follow Kotlin coding conventions**
2. **Add comprehensive documentation**
3. **Include error handling**
4. **Add appropriate tests**
5. **Update this README if needed**

## Troubleshooting

### Debug Commands

```bash
# Clear logs and monitor plugin activity
adb logcat -c && adb logcat | grep VideoScreenshotPlugin

# Check if plugin is registered
adb logcat | grep "plugin registration"

# Monitor frame extraction
adb logcat | grep "Frame extracted"
```
