# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-05-30

### Added

- 🎉 Initial release of React Native Video Screenshot Plugin
- 📸 **Core screenshot functionality**

  - `captureScreenshot()` - Capture video frames as base64 images
  - `saveScreenshotToLibrary()` - Save screenshots to device photo library
  - `saveScreenshotToPath()` - Save screenshots to custom file paths
  - `isScreenshotSupported()` - Check if screenshot capture is available
  - `getVideoDimensions()` - Get current video dimensions

- 🎯 **Comprehensive options support**

  - Multiple image formats (JPEG, PNG)
  - Configurable quality settings
  - Image resizing with aspect ratio preservation
  - Timestamp metadata inclusion
  - Cross-platform compatibility

- 📱 **Platform implementations**

  - **iOS**: Swift implementation using AVFoundation
    - Native AVAssetImageGenerator for frame extraction
    - Photo library integration with proper permissions
    - File system operations with directory creation
    - Memory management and resource cleanup
  - **Android**: Kotlin implementation using ExoPlayer
    - MediaMetadataRetriever for frame extraction
    - MediaStore integration for photo library access
    - Scoped storage support for Android 10+
    - Coroutines for async operations

- 🔧 **Developer experience features**

  - Full TypeScript support with comprehensive type definitions
  - Detailed JSDoc comments with usage examples
  - Proper error handling with descriptive error messages
  - Cross-platform API consistency
  - Tree-shaking support for optimal bundle size

- 📖 **Documentation and examples**

  - Comprehensive README with usage examples
  - Complete example app demonstrating all features
  - API reference documentation
  - Plugin development guide
  - Contributing guidelines
  - Cross-platform setup instructions

- 🧪 **Example application**

  - Interactive demo of all plugin features
  - Real-time screenshot preview
  - Plugin information display
  - Error handling demonstrations
  - Modern React Native UI with proper loading states
  - Cross-platform compatibility testing

- 🏗️ **Development infrastructure**
  - TypeScript configuration with strict mode
  - ESLint configuration with React Native and TypeScript rules
  - Prettier code formatting
  - Git ignore patterns for React Native projects
  - Package.json with proper peer dependencies
  - CocoaPods podspec for iOS integration
  - Gradle build configuration for Android

### Technical Details

#### iOS Implementation

- **Framework**: AVFoundation with Swift
- **Architecture**: Follows RNV plugin architecture pattern
- **Features**:
  - AVAssetImageGenerator for precise frame extraction
  - PHPhotoLibrary integration with authorization handling
  - FileManager operations with intermediate directory creation
  - Proper transform handling for video orientation
  - Memory management with proper bitmap cleanup
  - Main queue operations for UI-related tasks

#### Android Implementation

- **Framework**: ExoPlayer with Kotlin
- **Architecture**: Implements RNVExoplayerPlugin interface
- **Features**:
  - MediaMetadataRetriever for frame extraction
  - MediaStore integration with scoped storage support
  - Coroutines for non-blocking async operations
  - Bitmap manipulation and compression
  - Proper permission handling for storage access
  - File operations with automatic directory creation

#### Cross-Platform Features

- Consistent API across both platforms
- Unified error handling and error codes
- Similar performance characteristics
- Matching feature set and capabilities
- Identical TypeScript interface

### Plugin Development Showcase

This plugin demonstrates several important patterns for React Native Video plugin development:

1. **Plugin Registration**: Proper registration with ReactNativeVideoManager
2. **Player Lifecycle**: Handling player creation and destruction events
3. **Native Bridge**: Exposing native functionality to JavaScript
4. **Error Handling**: Comprehensive error handling with proper error codes
5. **Type Safety**: Full TypeScript support with detailed type definitions
6. **Cross-Platform**: Consistent API and behavior across iOS and Android
7. **Documentation**: Comprehensive documentation and examples
8. **Testing**: Example app for manual testing and validation

### Requirements

- React Native >= 0.72.0
- react-native-video >= 6.0.0
- iOS 11.0+
- Android API 21+

### Permissions

- **iOS**: `NSPhotoLibraryAddUsageDescription` for photo library access
- **Android**: Automatic permission handling for storage access

---

## Future Roadmap

### Planned Features (v1.1.0)

- 🎬 **Video frame sequences**: Capture multiple frames at different timestamps
- 🎨 **Image filters**: Apply basic filters to screenshots
- 📊 **Metadata extraction**: Extract more video metadata
- 🔄 **Background processing**: Support for background screenshot generation

### Planned Features (v1.2.0)

- 🎯 **Precise timestamp capture**: Capture at exact timestamps
- 📐 **Custom aspect ratios**: Crop to specific aspect ratios
- 🖼️ **Thumbnail generation**: Generate video thumbnails
- 📱 **New Architecture**: Full support for React Native new architecture

### Long-term Goals

- 🎪 **Advanced processing**: Integration with image processing libraries
- 🔗 **Plugin ecosystem**: Integration with other RNV plugins
- 📈 **Performance optimization**: Further performance improvements
- 🌐 **Web support**: Basic web platform support

---

## Contributing

This plugin serves as an example for the React Native Video community. Contributions are welcome and help improve both this plugin and the overall plugin ecosystem.

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup and guidelines.

## License

MIT License - see [LICENSE](LICENSE) file for details.
