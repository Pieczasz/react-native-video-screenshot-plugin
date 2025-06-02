# React Native Video Screenshot Plugin - Android Implementation

## Overview

This directory contains the Android implementation of the React Native Video Screenshot Plugin. The plugin extends [react-native-video](https://github.com/react-native-video/react-native-video) by providing screenshot capture functionality from active video players using Media3/ExoPlayer.

## What This Plugin Does

The plugin allows React Native applications to:

- Capture screenshots from currently playing videos
- Save screenshots to device gallery (MediaStore)
- Save screenshots to custom file paths
- Get video dimensions and playback information
- Check screenshot support status

## Architecture Overview

### Core Integration Strategy

The plugin integrates with react-native-video through the **Plugin System**:

```
React Native App Startup
            ↓
VideoScreenshotPluginPackage.createNativeModules()
            ↓
Register with ReactNativeVideoManager
            ↓
react-native-video calls plugin lifecycle methods:
            ↓
onInstanceCreated() → Store ExoPlayer reference
onInstanceRemoved() → Clean up player reference
            ↓
JavaScript calls screenshot methods
            ↓
Plugin uses stored player references to capture frames
```

### Core Components

#### 1. VideoScreenshotPluginModule.kt

The main native module implementing screenshot functionality and react-native-video integration.

**Key Features:**

- Implements `RNVExoplayerPlugin` interface for automatic integration
- Manages registry of active ExoPlayer instances
- Provides multiple screenshot capture methods (base64, file system, media library)
- Uses coroutines for asynchronous operations
- Implements robust frame extraction with multiple fallback strategies

**Public API Methods:**

```kotlin
@ReactMethod fun captureScreenshot(...)         // Returns base64
@ReactMethod fun saveScreenshotToLibrary(...)   // Saves to gallery
@ReactMethod fun saveScreenshotToPath(...)      // Saves to file path
@ReactMethod fun isScreenshotSupported(...)     // Checks support
@ReactMethod fun getVideoDimensions(...)        // Returns video size
@ReactMethod fun listAvailableVideos(...)       // Lists active players
```

#### 2. VideoScreenshotPluginPackage.kt

React Native package that registers the plugin with both React Native and react-native-video.

**Key Features:**

- Dual registration strategy (direct + reflection fallback)
- Comprehensive error handling and logging
- Version compatibility checks
- Graceful degradation if react-native-video is not available

## Implementation Details

### Player Lifecycle Management

```kotlin
// When react-native-video creates a new ExoPlayer instance
override fun onInstanceCreated(id: String, player: ExoPlayer) {
    DebugLog.d(TAG, "Player instance created - registering player with id: $id")

    playerRegistry[id] = player
    emitVideoPlayerReadyEvent(id)
}

// When react-native-video destroys an ExoPlayer instance
override fun onInstanceRemoved(id: String, player: ExoPlayer) {
    DebugLog.d(TAG, "Player instance removed - unregistering player with id: $id")

    playerRegistry.remove(id)
}
```

### Frame Extraction Strategy

The plugin uses a **multi-tier approach** for frame extraction:

#### 1. **MediaMetadataRetriever** (Primary)

```kotlin
private suspend fun extractFrameWithMediaMetadataRetriever(uri: Uri, timeUs: Long): Bitmap? {
    val retriever = retrieverPool.acquire()

    try {
        configureRetrieverDataSource(retriever, uri)

        // Try multiple extraction options in order of preference
        for (option in FRAME_EXTRACTION_OPTIONS) {
            val bitmap = retriever.getFrameAtTime(timeUs, option)
            if (bitmap != null) return bitmap
        }

        return attemptFallbackFrameExtraction(retriever)
    } finally {
        retrieverPool.release(retriever)
    }
}
```

**Extraction Options (in order of preference):**

- `OPTION_CLOSEST_SYNC` - Closest sync frame
- `OPTION_CLOSEST` - Closest frame (any type)
- `OPTION_PREVIOUS_SYNC` - Previous sync frame
- `OPTION_NEXT_SYNC` - Next sync frame

#### 2. **Enhanced Placeholder** (Fallback)

Creates visually appealing placeholder when extraction fails:

```kotlin
private suspend fun createFallbackPlaceholder(videoSize: VideoSize, player: ExoPlayer): Bitmap {
    val playerInfo = withContext(Dispatchers.Main) {
        PlayerInfo(player.currentPosition / 1000, player.duration / 1000)
    }

    return withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawPlaceholderBackground(canvas, width, height)
        drawPlaceholderContent(canvas, width, height, playerInfo)

        bitmap
    }
}
```

### Screenshot Output Modes

```kotlin
private sealed class ScreenshotOutputMode {
    object BASE64_ONLY : ScreenshotOutputMode()
    object SAVE_TO_LIBRARY : ScreenshotOutputMode()
    data class SAVE_TO_PATH(val filePath: String) : ScreenshotOutputMode()
}
```

### Thread Safety Design

The implementation uses **Kotlin Coroutines** for proper thread management:

- **Main Thread**: Player state access, UI operations
- **IO Thread**: File operations, MediaStore access
- **Default Thread**: Image processing, bitmap operations
- **Background Thread**: Frame extraction, compression

## Configuration Requirements

### Gradle Dependencies

The plugin requires these key dependencies:

```gradle

// Kotlin and Coroutines
implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

// Media3/ExoPlayer
implementation "androidx.media3:media3-exoplayer:$media3Version"
implementation "androidx.media3:media3-common:$media3Version"

// React Native Video
implementation project(':react-native-video')
```

### Android Permissions

For saving to gallery (MediaStore):

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                 android:maxSdkVersion="28" />
```

_Note: Android 10+ uses scoped storage and doesn't require this permission for MediaStore_

### Gradle Properties

```properties
VideoPluginSample_kotlinVersion=1.8.0
VideoPluginSample_minSdkVersion=21
VideoPluginSample_compileSdkVersion=33
VideoPluginSample_targetSdkVersion=33
VideoPluginSample_media3Version=1.3.1
```

## Plugin Registration Process

### Direct Registration (Preferred)

```kotlin
val videoManager = ReactNativeVideoManager.getInstance()
videoManager.registerPlugin(pluginModule)
```

### Reflection Fallback

```kotlin
try {
    val videoManagerClass = Class.forName("com.brentvatne.react.ReactNativeVideoManager")
    val getInstance = videoManagerClass.getMethod("getInstance")
    val instance = getInstance.invoke(null)
    val registerPlugin = videoManagerClass.getMethod("registerPlugin", pluginInterface)
    registerPlugin.invoke(instance, pluginModule)
} catch (e: Exception) {
    DebugLog.e(TAG, "Failed to register plugin via reflection: ${e.message}")
}
```

## Key Implementation Features

### 1. **MediaMetadataRetriever Pooling**

Reuses retriever instances for better performance:

```kotlin
class MediaMetadataRetrieverPool(private val maxSize: Int = 3) {
    private val pool = ConcurrentLinkedQueue<MediaMetadataRetriever>()

    fun acquire(): MediaMetadataRetriever {
        return pool.poll() ?: MediaMetadataRetriever()
    }

    fun release(retriever: MediaMetadataRetriever) {
        if (pool.size < maxSize) {
            pool.offer(retriever)
        } else {
            retriever.release()
        }
    }
}
```

### 2. **Efficient Bitmap Operations**

Uses Matrix transformations for better quality:

```kotlin
private fun resizeBitmapMaintainingAspectRatio(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val scale = minOf(scaleX, scaleY, 1.0f)
    if (scale >= 1.0f) return bitmap

    val matrix = Matrix().apply { setScale(scale, scale) }
    return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
}
```

### 3. **Async MediaStore Operations**

Non-blocking gallery saves with timeout protection:

```kotlin
private suspend fun saveImageToMediaStore(bitmap: Bitmap, format: String): Uri = withTimeout(10000) {
    withContext(Dispatchers.IO) {
        // MediaStore operations
    }
}
```

### 4. **Background Canvas Operations**

Placeholder generation on background threads:

```kotlin
return withContext(Dispatchers.Default) { // Background thread
    // Heavy canvas drawing operations
}
```

### 5. **WebP Support**

Modern compression format support:

```kotlin
val compressFormat = when (format.lowercase()) {
    "png" -> Bitmap.CompressFormat.PNG
    "webp" -> Bitmap.CompressFormat.WEBP_LOSSLESS
    else -> Bitmap.CompressFormat.JPEG
}
```

## Error Handling

The plugin implements comprehensive error handling:

```kotlin
try {
    val result = captureFrameFromPlayer(player, options, outputMode)
    promise.resolve(result)
} catch (e: Exception) {
    DebugLog.e(TAG, "Screenshot capture failed: ${e.message}")
    promise.reject("CAPTURE_FAILED", "Screenshot capture failed: ${e.message}", e)
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
  maxHeight: 1080,
});

// Save to device gallery
await VideoScreenshotPlugin.saveScreenshotToLibrary(videoId, options);

// Save to custom path
await VideoScreenshotPlugin.saveScreenshotToPath(videoId, '/path/to/screenshot.jpg', options);
```

## Android Version Support

- **Minimum SDK**: 21 (Android 5.0)
- **Target SDK**: 33+ (Android 13+)
- **React Native**: 0.60+
- **Kotlin**: 1.8.0+

## Debugging

### View Logs

```bash
# Filter plugin logs
adb logcat | grep VideoScreenshotPlugin

# Clear logs and monitor
adb logcat -c && adb logcat | grep VideoScreenshotPlugin
```

### Debug Methods

- `debugListPlayers()` - Lists all registered players
- `listAvailableVideos()` - Shows available video instances
- Detailed error messages with context

## Troubleshooting

| Issue              | Solution                                        |
| ------------------ | ----------------------------------------------- |
| Plugin not working | Ensure react-native-video is properly installed |
| Player not found   | Check video ID matches JavaScript component     |
| Black screenshots  | Wait for video to load before capturing         |
| Permission denied  | Request WRITE_EXTERNAL_STORAGE (Android < 10)   |
| Build failures     | Check gradle.properties versions                |

## Build Configuration

### Gradle Setup

The plugin uses modern Android build features:

```gradle
android {
    namespace = "com.videoscreenshotplugin"
    compileSdkVersion = 33

    defaultConfig {
        minSdkVersion 21
        targetSdkVersion 33
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
```

## Contributing

When contributing to the Android implementation:

1. Follow Kotlin coding conventions
2. Add comprehensive documentation
3. Include error handling
4. Add appropriate tests
5. Update this README for architectural changes
