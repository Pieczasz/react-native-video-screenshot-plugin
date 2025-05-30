# 🚀 Getting Started with React Native Video Screenshot Plugin

This guide will help you integrate the `react-native-video-screenshot-plugin` into your React Native project and start capturing video screenshots.

## 📋 Prerequisites

- React Native 0.68.0 or higher
- react-native-video 5.0.0 or higher
- Node.js 16+ and npm 8+
- For iOS: Xcode 12+ and CocoaPods
- For Android: Android Studio and SDK API 21+

## 📦 Installation

### 1. Install the Plugin

```bash
npm install react-native-video-screenshot-plugin
# or
yarn add react-native-video-screenshot-plugin
```

### 2. Install Peer Dependencies

```bash
npm install react-native-video
# or
yarn add react-native-video
```

### 3. Platform Setup

#### iOS Setup

1. **Install iOS dependencies:**
   ```bash
   cd ios && pod install && cd ..
   ```

2. **Add photo library permission to `ios/YourApp/Info.plist`:**
   ```xml
   <key>NSPhotoLibraryAddUsageDescription</key>
   <string>This app needs access to photo library to save video screenshots</string>
   ```

#### Android Setup

1. **Add permissions to `android/app/src/main/AndroidManifest.xml`:**
   ```xml
   <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
   <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
   ```

   For Android 13+ (API 33+), also add:
   ```xml
   <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
   ```

2. **For React Native 0.60+, the plugin should auto-link. If not, follow manual linking steps below.**

#### Manual Linking (if needed)

<details>
<summary>Manual Android Linking</summary>

1. **Add to `android/settings.gradle`:**
   ```gradle
   include ':react-native-video-screenshot-plugin'
   project(':react-native-video-screenshot-plugin').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-video-screenshot-plugin/android')
   ```

2. **Add to `android/app/build.gradle`:**
   ```gradle
   dependencies {
       implementation project(':react-native-video-screenshot-plugin')
   }
   ```

3. **Add to `MainApplication.java`:**
   ```java
   import com.videoscreenshotplugin.VideoScreenshotPluginPackage;

   @Override
   protected List<ReactPackage> getPackages() {
       return Arrays.<ReactPackage>asList(
           new MainReactPackage(),
           new VideoScreenshotPluginPackage()
       );
   }
   ```
</details>

## 🎯 Quick Start

### Basic Implementation

```tsx
import React, { useRef } from 'react';
import { View, TouchableOpacity, Text, Alert } from 'react-native';
import Video, { VideoRef } from 'react-native-video';
import { captureScreenshot } from 'react-native-video-screenshot-plugin';

const VideoScreenshotExample = () => {
  const videoRef = useRef<VideoRef>(null);

  const handleScreenshot = async () => {
    try {
      const result = await captureScreenshot(
        { videoId: 'my-video' }, // Must match the Video component's id prop
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
      
      Alert.alert('Success', 'Screenshot captured successfully!');
    } catch (error) {
      Alert.alert('Error', `Failed to capture screenshot: ${error}`);
    }
  };

  return (
    <View style={{ flex: 1 }}>
      <Video
        ref={videoRef}
        source={{ uri: 'https://example.com/video.mp4' }}
        style={{ width: '100%', height: 200 }}
        id="my-video" // ⚠️ Important: Required for plugin tracking
        controls
        resizeMode="contain"
      />
      
      <TouchableOpacity onPress={handleScreenshot} style={{ padding: 20 }}>
        <Text>Capture Screenshot</Text>
      </TouchableOpacity>
    </View>
  );
};

export default VideoScreenshotExample;
```

### Key Points

1. **Video ID**: Always provide an `id` prop to the Video component - this is how the plugin tracks video instances
2. **Import**: Import the functions you need from the plugin
3. **Error Handling**: Always wrap plugin calls in try-catch blocks
4. **Permissions**: Handle photo library permissions on iOS and storage permissions on Android

## 📚 Core API Usage

### 1. Capture Screenshot to Base64

```tsx
import { captureScreenshot } from 'react-native-video-screenshot-plugin';

const result = await captureScreenshot(
  { videoId: 'my-video' },
  {
    format: 'jpeg',        // 'jpeg' | 'png'
    quality: 0.9,          // 0-1 (JPEG only)
    maxWidth: 1920,        // Optional: resize width
    maxHeight: 1080,       // Optional: resize height
    includeTimestamp: true // Include video timestamp in result
  }
);

console.log('Base64 data:', result.base64);
console.log('Dimensions:', result.width, 'x', result.height);
console.log('Timestamp:', result.timestamp, 'seconds');
```

### 2. Save to Photo Library

```tsx
import { saveScreenshotToLibrary } from 'react-native-video-screenshot-plugin';

const result = await saveScreenshotToLibrary(
  { videoId: 'my-video' },
  {
    format: 'png',
    maxHeight: 1080
  }
);

console.log('Saved to:', result.uri);
console.log('File size:', result.size, 'bytes');
```

### 3. Save to Custom Path

```tsx
import { saveScreenshotToPath } from 'react-native-video-screenshot-plugin';
import RNFS from 'react-native-fs';

const filePath = `${RNFS.DocumentDirectoryPath}/screenshot.jpg`;
const result = await saveScreenshotToPath(
  { videoId: 'my-video' },
  filePath,
  {
    format: 'jpeg',
    quality: 1.0
  }
);

console.log('Saved to:', result.uri);
```

### 4. Check Video Support

```tsx
import { isScreenshotSupported, getVideoDimensions } from 'react-native-video-screenshot-plugin';

// Check if screenshot is supported for this video
const supported = await isScreenshotSupported({ videoId: 'my-video' });

// Get video dimensions
const dimensions = await getVideoDimensions({ videoId: 'my-video' });
console.log(`Video size: ${dimensions.width}x${dimensions.height}`);
```

## 🔧 Advanced Configuration

### Permission Handling

```tsx
import { PermissionsAndroid, Platform } from 'react-native';

const requestStoragePermission = async () => {
  if (Platform.OS === 'android') {
    try {
      const granted = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
        PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
      ]);
      
      return Object.values(granted).every(status => status === 'granted');
    } catch (error) {
      console.error('Permission request failed:', error);
      return false;
    }
  }
  return true; // iOS permissions handled by Info.plist
};

// Use before saving to library or custom paths
const handleSaveToLibrary = async () => {
  const hasPermission = await requestStoragePermission();
  if (!hasPermission) {
    Alert.alert('Permission Required', 'Storage permission is needed to save screenshots');
    return;
  }
  
  // Proceed with saving...
};
```

### Multiple Video Players

```tsx
const VideoGallery = () => {
  const videos = [
    { id: 'video-1', uri: 'https://example.com/video1.mp4' },
    { id: 'video-2', uri: 'https://example.com/video2.mp4' },
  ];

  const captureFromVideo = async (videoId: string) => {
    try {
      const result = await captureScreenshot({ videoId }, { format: 'jpeg' });
      // Handle result...
    } catch (error) {
      console.error(`Failed to capture from ${videoId}:`, error);
    }
  };

  return (
    <View>
      {videos.map(video => (
        <View key={video.id}>
          <Video
            source={{ uri: video.uri }}
            id={video.id} // Unique ID for each video
            style={{ width: 300, height: 200 }}
            controls
          />
          <TouchableOpacity onPress={() => captureFromVideo(video.id)}>
            <Text>Capture from {video.id}</Text>
          </TouchableOpacity>
        </View>
      ))}
    </View>
  );
};
```

### Error Handling Best Practices

```tsx
import { 
  captureScreenshot,
  type ScreenshotResult 
} from 'react-native-video-screenshot-plugin';

const captureWithErrorHandling = async (videoId: string): Promise<ScreenshotResult | null> => {
  try {
    // Check if video is ready and supports screenshots
    const supported = await isScreenshotSupported({ videoId });
    if (!supported) {
      Alert.alert('Error', 'Screenshot not supported for this video');
      return null;
    }

    const result = await captureScreenshot(
      { videoId },
      {
        format: 'jpeg',
        quality: 0.8,
        maxWidth: 1920,
      }
    );

    return result;
  } catch (error) {
    if (error instanceof Error) {
      console.error('Screenshot error:', error.message);
      
      // Handle specific error types
      if (error.message.includes('Video not found')) {
        Alert.alert('Error', 'Video player not found. Make sure the video is loaded.');
      } else if (error.message.includes('Permission denied')) {
        Alert.alert('Error', 'Permission denied. Please check app permissions.');
      } else {
        Alert.alert('Error', `Screenshot failed: ${error.message}`);
      }
    } else {
      Alert.alert('Error', 'Unknown error occurred');
    }
    
    return null;
  }
};
```

## 🧪 Testing Your Integration

### 1. Run the Example

The plugin includes a complete working example:

```bash
# Clone or navigate to plugin directory
cd examples/bare

# Install dependencies
npm install

# Build the plugin first
cd ../.. && npm run build && cd examples/bare

# Run on iOS
npm run ios

# Run on Android
npm run android
```

### 2. Debug Common Issues

1. **Video ID not found**: Ensure the `id` prop matches exactly between Video component and plugin calls
2. **Permission denied**: Check Info.plist (iOS) and AndroidManifest.xml permissions
3. **Video not loaded**: Wait for the video's `onLoad` callback before capturing screenshots
4. **Metro bundler issues**: Clear cache with `npx react-native start --reset-cache`

## 🔗 Next Steps

1. **Explore the [complete example](./examples/bare/README.md)** for a full implementation
2. **Read the [API Reference](./README.md#-api-reference)** for detailed method documentation
3. **Check [Troubleshooting](./README.md#-troubleshooting)** for common issues and solutions
4. **Review [Contributing Guidelines](./CONTRIBUTING.md)** if you want to contribute

## 💡 Tips for Success

- Always test on both iOS and Android devices
- Handle permissions gracefully with clear user messaging
- Use appropriate image formats (JPEG for photos, PNG for graphics)
- Consider image size and quality trade-offs for your use case
- Implement loading states for better user experience
- Cache screenshots when appropriate to avoid repeated captures

## 🤝 Need Help?

- Check the [examples](./examples/bare/) for working implementations
- Read the [full documentation](./README.md)
- Open an [issue](https://github.com/your-username/react-native-video-screenshot-plugin/issues) for bugs or questions
- Contribute improvements via [pull requests](https://github.com/your-username/react-native-video-screenshot-plugin/pulls)

Happy coding! 🎉 