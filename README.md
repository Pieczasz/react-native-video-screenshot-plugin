# 📸 React Native Video Screenshot Plugin

A powerful React Native library for capturing screenshots from `react-native-video` players on both iOS and Android platforms.

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
- 🚀 **TypeScript**: Full TypeScript support with comprehensive type definitions

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
import { captureScreenshot } from 'react-native-video-screenshot-plugin';

const App = () => {
  const videoRef = useRef<VideoRef>(null);

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

## 📚 API Reference

### `captureScreenshot(videoRef, options?)`

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

### `saveScreenshotToLibrary(videoRef, options?)`

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

### `saveScreenshotToPath(videoRef, filePath, options?)`

Captures a screenshot and saves it to a custom file path.

```tsx
import RNFS from 'react-native-fs';

const filePath = `${RNFS.DocumentDirectoryPath}/screenshots/capture.jpg`;
const result = await saveScreenshotToPath(
  { videoId: 'my-video' },
  filePath,
  {
    format: 'jpeg',
    quality: 1.0,
  }
);
```

### `isScreenshotSupported(videoRef)`

Checks if screenshot capture is supported for the current video.

```tsx
const supported = await isScreenshotSupported({ videoId: 'my-video' });
if (supported) {
  // Proceed with screenshot capture
}
```

### `getVideoDimensions(videoRef)`

Gets the current video dimensions.

```tsx
const { width, height } = await getVideoDimensions({ videoId: 'my-video' });
console.log(`Video size: ${width}x${height}`);
```

## ⚙️ Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `format` | `'jpeg' \| 'png'` | `'jpeg'` | Output image format |
| `quality` | `number` | `0.9` | JPEG quality (0-1) |
| `maxWidth` | `number` | `undefined` | Maximum width (preserves aspect ratio) |
| `maxHeight` | `number` | `undefined` | Maximum height (preserves aspect ratio) |
| `includeTimestamp` | `boolean` | `true` | Include video timestamp in result |

## 🔄 Return Types

### `ScreenshotResult`

```tsx
interface ScreenshotResult {
  base64: string;           // Base64 encoded image data
  uri?: string;             // File URI (when saved to device)
  width: number;            // Image width in pixels
  height: number;           // Image height in pixels
  timestamp?: number;       // Video timestamp in seconds
  size?: number;            // File size in bytes (when saved)
}
```

### `VideoRef`

```tsx
interface VideoRef {
  videoId: string;          // Unique identifier for the video instance
}
```

## 📱 Example App

The example app demonstrates all plugin features:

```bash
# Run the example
git clone https://github.com/your-username/react-native-video-screenshot-plugin.git
cd react-native-video-screenshot-plugin
npm install
npm run build

# Run on iOS
npm run ios

# Run on Android
npm run android
```

## 🎯 Advanced Usage

### Custom Screenshot Directory

```tsx
import RNFS from 'react-native-fs';

// Create screenshots directory
const screenshotsDir = `${RNFS.DocumentDirectoryPath}/screenshots`;
await RNFS.mkdir(screenshotsDir);

// Save with timestamp
const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
const filePath = `${screenshotsDir}/video_${timestamp}.jpg`;

const result = await saveScreenshotToPath(
  { videoId: 'my-video' },
  filePath,
  { format: 'jpeg', quality: 0.95 }
);
```

### Batch Screenshots

```tsx
const captureMultipleScreenshots = async (videoId: string, intervals: number[]) => {
  const screenshots = [];
  
  for (const interval of intervals) {
    // Seek to specific time
    videoRef.current?.seek(interval);
    
    // Wait for seek to complete
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // Capture screenshot
    const result = await captureScreenshot({ videoId });
    screenshots.push({ timestamp: interval, ...result });
  }
  
  return screenshots;
};
```

### Error Handling

```tsx
const safeScreenshot = async (videoId: string) => {
  try {
    // Check if supported first
    const supported = await isScreenshotSupported({ videoId });
    if (!supported) {
      throw new Error('Screenshot not supported for this video');
    }
    
    // Get video info
    const dimensions = await getVideoDimensions({ videoId });
    console.log('Video dimensions:', dimensions);
    
    // Capture screenshot
    const result = await captureScreenshot({ videoId });
    return result;
    
  } catch (error) {
    console.error('Screenshot error:', error);
    // Handle specific error types
    if (error.code === 'PLAYER_NOT_FOUND') {
      console.error('Video player not found');
    } else if (error.code === 'CAPTURE_FAILED') {
      console.error('Failed to capture frame');
    }
    throw error;
  }
};
```

## 🐛 Troubleshooting

### Common Issues

1. **"Player not found" error**
   - Ensure the video has the correct `id` prop
   - Wait for video to load before capturing

2. **"Permission denied" for photo library**
   - Add photo library permissions to Info.plist (iOS)
   - Request runtime permissions on Android

3. **"Capture failed" error**
   - Check if video is actually playing/loaded
   - Verify video format is supported

4. **Screenshots appear black**
   - Some video formats/sources may not support frame extraction
   - Try different video sources for testing

### Debug Mode

Enable debug logging to troubleshoot issues:

```tsx
// Enable debug mode in development
if (__DEV__) {
  console.log('Video screenshot plugin loaded');
}
```

## 🔧 Development

### Setting up for Development

```bash
git clone https://github.com/your-username/react-native-video-screenshot-plugin.git
cd react-native-video-screenshot-plugin
npm install
npm run build
```

### Running Tests

```bash
npm test
npm run test:watch
```

### Linting

```bash
npm run lint
npm run lint:fix
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [react-native-video](https://github.com/react-native-video/react-native-video) team for the excellent video player
- React Native community for the amazing ecosystem

## 📞 Support

- 🐛 [Report bugs](https://github.com/your-username/react-native-video-screenshot-plugin/issues)
- 💡 [Request features](https://github.com/your-username/react-native-video-screenshot-plugin/issues)
- 📖 [Documentation](https://github.com/your-username/react-native-video-screenshot-plugin#readme)

---

Made with ❤️ by [Your Name](https://github.com/your-username)
