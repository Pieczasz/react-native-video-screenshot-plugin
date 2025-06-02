# 📸 React Native Video Screenshot Plugin

A powerful React Native library for capturing screenshots from `react-native-video` players on both iOS and Android platforms. This plugin also serves as a comprehensive example for developing your own React Native Video plugins.

[![npm version](https://badge.fury.io/js/react-native-video-screenshot-plugin.svg)](https://badge.fury.io/js/react-native-video-screenshot-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/platform-iOS%20%7C%20Android-blue.svg)](https://reactnative.dev/)

## ✨ Features

- 📱 **Cross-platform**: Works on both iOS and Android
- 🎯 **Easy Integration**: Seamlessly integrates with react-native-video
- 🖼️ **Multiple Formats**: Support for JPEG and PNG output formats
- 📐 **Flexible Sizing**: Configurable max width/height with aspect ratio preservation
- 💾 **Multiple Save Options**: Save to photo library, custom paths, or get base64 data
- ⏱️ **Timestamp Support**: Include video timestamp metadata
- 🔍 **Quality Control**: Adjustable JPEG quality settings
- 🔧 **Debug Tools**: Built-in debugging and testing utilities
- 🚀 **TypeScript**: Full TypeScript support with comprehensive type definitions
- 📚 **Plugin Development Guide**: Learn how to create your own RNV plugins

## 📦 Installation

```bash
npm install react-native-video-screenshot-plugin
# or
yarn add react-native-video-screenshot-plugin
```

### iOS Setup

1. Install iOS dependencies:

   ```bash
   cd ios && pod install
   ```

2. Add photo library usage permission to your `Info.plist`:
   ```xml
   <key>NSPhotoLibraryAddUsageDescription</key>
   <string>This app needs access to photo library to save screenshots</string>
   ```

### Android Setup

1. Add permissions to your `android/app/src/main/AndroidManifest.xml`:

   ```xml
   <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
   <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
   ```

2. For Android 13+ (API 33+), add the more specific permission:
   ```xml
   <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
   ```

## 🚀 Quick Start

```tsx
import React, { useRef } from 'react';
import { View, TouchableOpacity, Text } from 'react-native';
import Video, { VideoRef } from 'react-native-video';
import { captureScreenshot, testMethod, getModuleInfo } from 'react-native-video-screenshot-plugin';

const App = () => {
  const videoRef = useRef<VideoRef>(null);

  // Test the plugin on app start
  React.useEffect(() => {
    const initializePlugin = async () => {
      try {
        // Test if the native module is working
        const testResult = await testMethod();
        console.log('Plugin test:', testResult);

        // Get module information
        const moduleInfo = await getModuleInfo();
        console.log('Module info:', moduleInfo);
      } catch (error) {
        console.error('Plugin initialization failed:', error);
      }
    };

    initializePlugin();
  }, []);

  const handleScreenshot = async () => {
    try {
      const result = await captureScreenshot(
        { videoId: 'my-video' },
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
        }
      );

      console.log('Screenshot captured!', {
        width: result.width,
        height: result.height,
        base64: result.base64.substring(0, 50) + '...',
      });
    } catch (error) {
      console.error('Screenshot failed:', error);
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <Video
        ref={videoRef}
        source={{ uri: 'https://example.com/video.mp4' }}
        style={{ width: '100%', height: 200 }}
        id="my-video" // Important: Add an ID for the plugin to track
        controls
      />

      <TouchableOpacity onPress={handleScreenshot}>
        <Text>Capture Screenshot</Text>
      </TouchableOpacity>
    </View>
  );
};

export default App;
```

## 📚 Complete API Reference

### Core Screenshot Functions

#### `captureScreenshot(videoRef, options?)`

Captures a screenshot from the current video frame and returns base64 data.

```tsx
const result = await captureScreenshot(
  { videoId: 'my-video' },
  {
    format: 'jpeg',
    quality: 0.9,
    maxWidth: 1280,
    includeTimestamp: true,
  }
);
```

#### `captureScreenshotWhenReady(videoRef, options?)`

Captures a screenshot with automatic retry when video becomes ready. Useful for handling videos that might not be immediately ready for capture.

```tsx
const result = await captureScreenshotWhenReady(
  { videoId: 'my-video' },
  {
    format: 'png',
    quality: 1.0,
  }
);
```

#### `saveScreenshotToLibrary(videoRef, options?)`

Captures a screenshot and saves it to the device's photo library.

```tsx
const result = await saveScreenshotToLibrary(
  { videoId: 'my-video' },
  {
    format: 'png',
    maxHeight: 1080,
  }
);
```

#### `saveScreenshotToPath(videoRef, filePath, options?)`

Captures a screenshot and saves it to a custom file path.

```tsx
import RNFS from 'react-native-fs';

const filePath = `${RNFS.DocumentDirectoryPath}/screenshots/capture.jpg`;
const result = await saveScreenshotToPath({ videoId: 'my-video' }, filePath, {
  format: 'jpeg',
  quality: 1.0,
});
```

### Video Information Functions

#### `isScreenshotSupported(videoRef)`

Checks if screenshot capture is supported for the current video.

```tsx
const supported = await isScreenshotSupported({ videoId: 'my-video' });
if (supported) {
  // Proceed with screenshot capture
}
```

#### `getVideoDimensions(videoRef)`

Gets the current video dimensions.

```tsx
const { width, height } = await getVideoDimensions({ videoId: 'my-video' });
console.log(`Video size: ${width}x${height}`);
```

#### `listAvailableVideos()`

Lists all currently available video players that can be used for screenshots.

```tsx
const videoIds = await listAvailableVideos();
console.log('Available videos:', videoIds);

// Use the first available video
if (videoIds.length > 0) {
  const result = await captureScreenshot({ videoId: videoIds[0] });
}
```

### Development & Testing Functions

#### `testMethod()`

Tests if the native module is working correctly. Useful for debugging setup issues.

```tsx
const testResult = await testMethod();
console.log('Module test result:', testResult);
// Returns: { status: string, message: string, timestamp: number, platform: string }
```

#### `getModuleInfo()`

Gets comprehensive information about the plugin module and its current state.

```tsx
const moduleInfo = await getModuleInfo();
console.log('Module info:', moduleInfo);
// Returns: {
//   name: string,
//   version: string,
//   platform: string,
//   playersRegistered: number,
//   availablePlayerIds: string[],
//   timestamp: number
// }
```

#### `registerPlayer(playerId)`

Manually registers a test player. Useful for development and testing scenarios.

```tsx
const registrationResult = await registerPlayer('test-player-1');
console.log('Player registration:', registrationResult);
// Returns: {
//   status: string,
//   playerId: string,
//   message: string,
//   hasCurrentItem: boolean,
//   testVideoURL?: string,
//   testMode: boolean,
//   platform: string,
//   note?: string
// }
```

#### `debugListPlayers()`

Debug method to list all registered players. Helpful for troubleshooting player registration issues.

```tsx
const players = await debugListPlayers();
console.log('Registered players:', players);
```

### Class-Based API

For advanced usage, you can also use the class-based API:

```tsx
import { VideoScreenshotPluginClass } from 'react-native-video-screenshot-plugin';

// All methods are available as static methods
const result = await VideoScreenshotPluginClass.captureScreenshot('my-video', options);
const moduleInfo = await VideoScreenshotPluginClass.getModuleInfo();
```

## ⚙️ Configuration Options

| Option             | Type              | Default     | Description                             |
| ------------------ | ----------------- | ----------- | --------------------------------------- |
| `format`           | `'jpeg' \| 'png'` | `'jpeg'`    | Output image format                     |
| `quality`          | `number`          | `0.9`       | JPEG quality (0-1)                      |
| `maxWidth`         | `number`          | `undefined` | Maximum width (preserves aspect ratio)  |
| `maxHeight`        | `number`          | `undefined` | Maximum height (preserves aspect ratio) |
| `includeTimestamp` | `boolean`         | `true`      | Include video timestamp in result       |

## 🔄 TypeScript Interfaces

### `ScreenshotResult`

Complete result object returned by screenshot functions:

```tsx
interface ScreenshotResult {
  base64: string; // Base64 encoded image data
  uri?: string; // File URI (when saved to device)
  width: number; // Image width in pixels
  height: number; // Image height in pixels
  timestamp?: number; // Video timestamp in seconds
  size?: number; // File size in bytes (when saved)
  platform?: string; // Platform information
  message?: string; // Status or error message
  isTestMode?: boolean; // Whether this was captured in test mode
  reason?: string; // Additional context information
  playerStatus?: number; // Player status code
  error?: string; // Error details if capture failed
  waitTime?: number; // Time waited for player to be ready
}
```

### `ScreenshotOptions`

Configuration options for screenshot capture:

```tsx
interface ScreenshotOptions {
  format?: 'jpeg' | 'png'; // Output format (default: 'jpeg')
  quality?: number; // JPEG quality 0-1 (default: 0.9)
  maxWidth?: number; // Maximum width in pixels
  maxHeight?: number; // Maximum height in pixels
  includeTimestamp?: boolean; // Include timestamp (default: true)
}
```

### `VideoRef`

Reference to a video player instance:

```tsx
interface VideoRef {
  videoId: string; // Unique identifier for the video instance
}
```

### `ModuleInfo`

Information about the plugin module:

```tsx
interface ModuleInfo {
  name: string; // Module name
  version: string; // Module version
  platform: string; // Current platform (iOS/Android)
  playersRegistered: number; // Number of registered players
  availablePlayerIds: string[]; // List of available player IDs
  timestamp: number; // Current timestamp
}
```

### `TestResult`

Result from module testing:

```tsx
interface TestResult {
  status: string; // Test status
  message: string; // Test result message
  timestamp: number; // Test execution timestamp
  platform: string; // Platform where test was executed
}
```

### `PlayerRegistrationResult`

Result from player registration:

```tsx
interface PlayerRegistrationResult {
  status: string; // Registration status
  playerId: string; // The registered player ID
  message: string; // Registration message
  hasCurrentItem: boolean; // Whether player has content loaded
  testVideoURL?: string; // Test video URL (if any)
  testMode: boolean; // Whether in test mode
  platform: string; // Platform information
  note?: string; // Additional notes
}
```

## 📱 Example App

The example app demonstrates all plugin features and serves as a comprehensive testing tool:

```bash
# Clone and setup
git clone https://github.com/Pieczasz/react-native-video-screenshot-plugin.git
cd react-native-video-screenshot-plugin/examples/bare
npm install

# iOS setup
cd ios && pod install && cd ..

# Run examples
npm run ios     # Run on iOS
npm run android # Run on Android
```

The example app includes:

- Interactive testing of all API functions
- Real-time screenshot preview
- Module information display
- Error handling demonstrations
- Performance testing tools

## 🎯 Advanced Usage

### Plugin Development Setup

This plugin serves as a complete example for developing React Native Video plugins. Here's what you can learn:

#### 1. Project Structure

```
react-native-video-screenshot-plugin/
├── src/                          # TypeScript source code
│   ├── index.tsx                # Main API exports
│   └── __tests__/              # Unit tests
├── lib/                         # Compiled JavaScript
├── ios/                         # iOS Swift implementation
│   ├── VideoScreenshotPlugin.swift
│   ├── VideoScreenshotPlugin.h
│   └── VideoScreenshotPlugin.m
├── android/                     # Android Kotlin implementation
│   └── src/main/java/com/videoscreenshotplugin/
├── examples/bare/              # Example React Native app
└── react-native-video-screenshot-plugin.podspec
```

#### 2. Native Module Implementation

**iOS (Swift):**

```swift
// Key patterns for RNV plugin development
@objc(VideoScreenshotPlugin)
class VideoScreenshotPlugin: NSObject, RCTBridgeModule {
  static func moduleName() -> String! {
    return "VideoScreenshotPlugin"
  }

  // Plugin registration with ReactNativeVideoManager
  override init() {
    super.init()
    ReactNativeVideoManager.shared().registerPlugin(self)
  }
}
```

**Android (Kotlin):**

```kotlin
// Plugin interface implementation
class VideoScreenshotPluginModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RNVExoplayerPlugin {

    override fun getName(): String = "VideoScreenshotPlugin"

    // Register with ReactNativeVideoManager
    init {
        ReactNativeVideoManager.getInstance().registerPlugin(this)
    }
}
```

#### 3. Bridge Method Patterns

```tsx
// JavaScript side - consistent with native implementations
export function captureScreenshot(
  videoRef: VideoRef,
  options: ScreenshotOptions = {}
): Promise<ScreenshotResult> {
  const config = {
    format: 'jpeg',
    quality: 0.9,
    includeTimestamp: true,
    ...options,
  };

  return VideoScreenshotPlugin.captureScreenshot(videoRef.videoId, config);
}
```

### Error Handling Best Practices

```tsx
const safeScreenshot = async (videoId: string) => {
  try {
    // 1. Test module availability
    const testResult = await testMethod();
    if (testResult.status !== 'success') {
      throw new Error(`Module not ready: ${testResult.message}`);
    }

    // 2. Check player availability
    const availableVideos = await listAvailableVideos();
    if (!availableVideos.includes(videoId)) {
      throw new Error(`Video player '${videoId}' not found`);
    }

    // 3. Verify screenshot support
    const supported = await isScreenshotSupported({ videoId });
    if (!supported) {
      throw new Error('Screenshot not supported for this video');
    }

    // 4. Get video info for context
    const dimensions = await getVideoDimensions({ videoId });
    console.log('Video dimensions:', dimensions);

    // 5. Capture with retry mechanism
    const result = await captureScreenshotWhenReady({ videoId });
    return result;
  } catch (error) {
    console.error('Screenshot error:', error);

    // Handle specific error types
    if (error.message.includes('PLAYER_NOT_FOUND')) {
      console.error('Video player not found - ensure video is loaded');
    } else if (error.message.includes('CAPTURE_FAILED')) {
      console.error('Failed to capture frame - try again when video is playing');
    }

    throw error;
  }
};
```

### Batch Screenshots

```tsx
const captureMultipleScreenshots = async (videoId: string, intervals: number[]) => {
  const screenshots = [];

  for (const interval of intervals) {
    try {
      // For precise timestamp capture, you'd seek to the time first
      // This requires additional video player control

      // Wait for video to be ready
      const result = await captureScreenshotWhenReady(
        { videoId },
        {
          format: 'jpeg',
          quality: 0.8,
          includeTimestamp: true,
        }
      );

      screenshots.push({
        targetTimestamp: interval,
        actualTimestamp: result.timestamp,
        ...result,
      });
    } catch (error) {
      console.warn(`Failed to capture at ${interval}s:`, error);
    }
  }

  return screenshots;
};
```

### Custom Screenshot Directory Management

```tsx
import RNFS from 'react-native-fs';

const setupScreenshotDirectory = async () => {
  const screenshotsDir = `${RNFS.DocumentDirectoryPath}/video-screenshots`;

  try {
    // Check if directory exists
    const exists = await RNFS.exists(screenshotsDir);
    if (!exists) {
      await RNFS.mkdir(screenshotsDir);
      console.log('Screenshots directory created');
    }

    return screenshotsDir;
  } catch (error) {
    console.error('Failed to setup screenshots directory:', error);
    throw error;
  }
};

const saveWithTimestamp = async (videoId: string) => {
  const screenshotsDir = await setupScreenshotDirectory();
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filename = `${videoId}_${timestamp}.jpg`;
  const filePath = `${screenshotsDir}/${filename}`;

  const result = await saveScreenshotToPath({ videoId }, filePath, {
    format: 'jpeg',
    quality: 0.95,
  });

  console.log(`Screenshot saved: ${result.uri}`);
  return result;
};
```

## 🐛 Troubleshooting

### Common Issues and Solutions

#### 1. "Module not found" Error

```bash
# iOS: Re-install pods
cd ios && pod install

# Android: Clean and rebuild
cd android && ./gradlew clean
```

#### 2. "Player not found" Error

```tsx
// Ensure video has loaded before capturing
const handleVideoLoad = async () => {
  // Wait a moment for player registration
  setTimeout(async () => {
    const available = await listAvailableVideos();
    console.log('Available players:', available);
  }, 1000);
};
```

#### 3. Permission Issues

```tsx
// Check permissions before saving to library
import { PermissionsAndroid, Platform } from 'react-native';

const requestStoragePermission = async () => {
  if (Platform.OS === 'android') {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
    );
    return granted === PermissionsAndroid.RESULTS.GRANTED;
  }
  return true;
};
```

#### 4. Black Screenshots

```tsx
// Some video sources may not support frame extraction
// Test with known working videos first
const testSources = [
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
];
```

### Debug Mode

Enable comprehensive logging:

```tsx
const debugPlugin = async () => {
  try {
    // Test basic functionality
    const testResult = await testMethod();
    console.log('🧪 Test Result:', testResult);

    // Get module information
    const moduleInfo = await getModuleInfo();
    console.log('📊 Module Info:', moduleInfo);

    // List available players
    const players = await debugListPlayers();
    console.log('🎬 Debug Players:', players);

    const available = await listAvailableVideos();
    console.log('📹 Available Videos:', available);
  } catch (error) {
    console.error('🚨 Debug failed:', error);
  }
};

// Call on app start in development
if (__DEV__) {
  debugPlugin();
}
```

## 🔧 Development

### Setting up for Development

```bash
git clone https://github.com/Pieczasz/react-native-video-screenshot-plugin.git
cd react-native-video-screenshot-plugin
npm install
npm run build
```

### Available Scripts

```bash
# Build TypeScript
npm run build
npm run watch          # Watch mode

# Testing
npm test               # Run tests
npm run test:watch     # Watch mode
npm run test:coverage  # With coverage

# Code Quality
npm run lint           # ESLint
npm run lint:fix       # Auto-fix issues
npm run typecheck      # TypeScript check
npm run format         # Prettier formatting

# Example App
npm run example:install    # Install example dependencies
npm run example:ios        # Run iOS example
npm run example:android    # Run Android example
npm run example:pods       # Install iOS pods for example

# Release
npm run validate       # Run all checks
npm run release        # Publish to npm
```

### Plugin Development Guidelines

1. **Follow RNV Plugin Architecture**

   - Implement proper plugin registration
   - Handle player lifecycle events
   - Use consistent error handling

2. **Cross-Platform Consistency**

   - Maintain identical APIs on both platforms
   - Use similar native implementations
   - Test thoroughly on both iOS and Android

3. **TypeScript Best Practices**

   - Provide comprehensive type definitions
   - Use strict mode configuration
   - Document all public APIs with JSDoc

4. **Testing Strategy**
   - Unit tests for all public functions
   - Integration tests with example app
   - Manual testing on real devices

## 🤝 Contributing

This plugin welcomes contributions and serves as a learning resource for the React Native Video plugin ecosystem.

### How to Contribute

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes** following the coding standards
4. **Add tests** for new functionality
5. **Update documentation** as needed
6. **Commit your changes**: `git commit -m 'Add amazing feature'`
7. **Push to branch**: `git push origin feature/amazing-feature`
8. **Open a Pull Request**

### Development Areas

- 🆕 **New Features**: Additional screenshot formats, video effects
- 🐛 **Bug Fixes**: Platform-specific issues, performance improvements
- 📚 **Documentation**: Examples, tutorials, API improvements
- 🧪 **Testing**: More comprehensive test coverage
- 🏗️ **Architecture**: New React Native architecture support

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [react-native-video](https://github.com/react-native-video/react-native-video) team for the excellent video player and plugin architecture
- React Native community for the amazing ecosystem
- Plugin developers who contribute to the RNV ecosystem

## 📞 Support & Community

- 🐛 [Report bugs](https://github.com/Pieczasz/react-native-video-screenshot-plugin/issues)
- 💡 [Request features](https://github.com/Pieczasz/react-native-video-screenshot-plugin/issues)
- 📖 [Documentation](https://github.com/Pieczasz/react-native-video-screenshot-plugin#readme)
- 💬 [Discussions](https://github.com/Pieczasz/react-native-video-screenshot-plugin/discussions)

## 🚀 Plugin Development Resources

This plugin demonstrates key concepts for RNV plugin development:

- **Plugin Registration**: How to register with ReactNativeVideoManager
- **Cross-Platform Implementation**: Consistent APIs across iOS and Android
- **Bridge Methods**: Exposing native functionality to JavaScript
- **Error Handling**: Comprehensive error management patterns
- **TypeScript Integration**: Full type safety and developer experience
- **Testing Strategies**: Unit, integration, and manual testing approaches
- **Documentation**: Comprehensive API documentation and examples

Use this plugin as a reference for creating your own React Native Video plugins!

---

**Made with ❤️ by [Bartłomiej Piekarz](https://github.com/Pieczasz)**

_Building tools to make React Native Video development more powerful and accessible._
