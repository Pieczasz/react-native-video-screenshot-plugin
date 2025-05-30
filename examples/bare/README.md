# 📸 React Native Video Screenshot Plugin - Bare Example

This is a complete working example demonstrating how to use the `react-native-video-screenshot-plugin` with `react-native-video`.

## 🚀 Features Demonstrated

- ✅ **Video playback** with react-native-video
- ✅ **Screenshot capture** to base64
- ✅ **Save to photo library** with permissions
- ✅ **Save to custom file paths**
- ✅ **Video information** checking (dimensions, support)
- ✅ **Multiple video sources** switching
- ✅ **Error handling** and user feedback
- ✅ **Cross-platform** iOS and Android support

## 📦 Setup Instructions

### Prerequisites

- React Native development environment set up
- Node.js 16+ and npm 8+
- For iOS: Xcode and CocoaPods
- For Android: Android Studio and SDK

### 1. Install Dependencies

```bash
# From the example directory
cd examples/bare
npm install
```

### 2. Build the Plugin

```bash
# From the plugin root directory
cd ../..
npm install
npm run build
```

### 3. Platform Setup

#### iOS Setup

```bash
# Install iOS dependencies
cd examples/bare/ios
pod install
cd ..
```

**Add permissions to `ios/bare/Info.plist`:**

```xml
<key>NSPhotoLibraryAddUsageDescription</key>
<string>This app needs access to photo library to save video screenshots</string>
```

#### Android Setup

**Add permissions to `android/app/src/main/AndroidManifest.xml`:**

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" /> <!-- Android 13+ -->
```

### 4. Run the Example

```bash
# Start Metro bundler
npm start

# In another terminal - run on iOS
npm run ios

# Or run on Android
npm run android
```

## 🎬 How to Use the Example

1. **Play Video**: The app loads with a sample video ready to play
2. **Capture Screenshot**: Tap "📸 Capture Screenshot" to get base64 data
3. **Save to Library**: Tap "💾 Save to Photo Library" to save to device photos
4. **Save to Documents**: Tap "📁 Save to Documents" to save to app documents folder
5. **Switch Videos**: Use "Switch Video" to test different video sources
6. **View Information**: Check video dimensions and plugin support status

## 📁 Project Structure

```
examples/bare/
├── App.tsx                 # Main example application
├── package.json           # Dependencies and scripts
├── metro.config.js        # Metro bundler configuration
├── babel.config.js        # Babel transformer configuration
├── android/               # Android-specific files
├── ios/                   # iOS-specific files
└── README.md             # This file
```

## 🔧 Key Implementation Details

### Video Player Setup

```tsx
<Video
  ref={videoRef}
  source={{uri: videoUri}}
  id="unique-video-id" // Required for plugin tracking
  controls={true}
  onLoad={handleVideoLoad}
/>
```

### Screenshot Capture

```tsx
// Base64 capture
const result = await captureScreenshot(
  {videoId: 'unique-video-id'},
  {
    format: 'jpeg',
    quality: 0.8,
    maxWidth: 1920,
    includeTimestamp: true,
  },
);

// Save to photo library
const result = await saveScreenshotToLibrary(
  {videoId: 'unique-video-id'},
  {
    format: 'png',
    maxHeight: 1080,
  },
);

// Save to custom path
const filePath = `${RNFS.DocumentDirectoryPath}/screenshot.jpg`;
const result = await saveScreenshotToPath(
  {videoId: 'unique-video-id'},
  filePath,
  {
    format: 'jpeg',
    quality: 1.0,
  },
);
```

### Permission Handling

```tsx
// Android permissions
const granted = await PermissionsAndroid.requestMultiple([
  PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
  PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
]);
```

## 🐛 Troubleshooting

### Common Issues

1. **Metro bundler fails to find plugin**

   - Ensure you've built the plugin: `npm run build` from root
   - Clear Metro cache: `npx react-native start --reset-cache`

2. **iOS pod install fails**

   - Update CocoaPods: `gem install cocoapods`
   - Clear Pods: `cd ios && rm -rf Pods && pod install`

3. **Android permissions not working**

   - Check AndroidManifest.xml has all required permissions
   - For Android 11+, ensure you handle scoped storage properly

4. **Video not loading**
   - Check internet connection for remote videos
   - Verify video URL is accessible
   - Check video format is supported by react-native-video

### Build Issues

```bash
# Clean and rebuild everything
npm run clean
npm run build  # From plugin root
cd examples/bare && npm install
```

## 📚 Learning Resources

- [React Native Video Documentation](https://github.com/react-native-video/react-native-video)
- [React Native Permissions](https://github.com/zoontek/react-native-permissions)
- [React Native File System](https://github.com/itinance/react-native-fs)

## 💡 Extending the Example

This example can be extended to demonstrate:

- Batch screenshot capture
- Video thumbnail generation
- Custom screenshot overlays
- Different video formats and sources
- Advanced error handling
- Progress indicators
- Screenshot editing features

## 🤝 Contributing

If you find issues with this example or have suggestions for improvements, please open an issue or submit a pull request to the main plugin repository.

## 📄 License

This example is part of the react-native-video-screenshot-plugin project and is licensed under the MIT License.
