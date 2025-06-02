# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-06-02

### Added

- 🎉 **Initial release of React Native Video Screenshot Plugin**
- 📸 **Core screenshot functionality**

  - `captureScreenshot()` - Capture video frames as base64 images with comprehensive metadata
  - `captureScreenshotWhenReady()` - Capture screenshots with automatic retry when video becomes ready
  - `saveScreenshotToLibrary()` - Save screenshots to device photo library with proper permissions
  - `saveScreenshotToPath()` - Save screenshots to custom file paths with directory creation
  - `isScreenshotSupported()` - Check if screenshot capture is available for specific videos
  - `getVideoDimensions()` - Get current video dimensions and aspect ratio information

- 🔧 **Video management and debugging tools**

  - `listAvailableVideos()` - List all currently available video players for screenshot operations
  - `testMethod()` - Test native module functionality and connectivity
  - `getModuleInfo()` - Get comprehensive module information, version, and status
  - `registerPlayer()` - Register test players for development and testing scenarios
  - `debugListPlayers()` - Debug method to list all registered players and their states

- 🎯 **Comprehensive options support**

  - Multiple image formats (JPEG, PNG) with quality control
  - Configurable quality settings (0.0-1.0 for JPEG compression)
  - Image resizing with automatic aspect ratio preservation
  - Maximum width and height constraints
  - Timestamp metadata inclusion with video position information
  - Cross-platform compatibility with consistent behavior

- 📱 **Platform implementations**

  - **iOS**: Swift implementation using AVFoundation
    - Native AVAssetImageGenerator for precise frame extraction
    - PHPhotoLibrary integration with proper authorization handling
    - FileManager operations with intermediate directory creation
    - Proper video orientation and transform handling
    - Memory management with automatic resource cleanup
    - Main queue operations for UI-related tasks
    - Comprehensive error handling with descriptive messages
  - **Android**: Kotlin implementation using ExoPlayer integration
    - MediaMetadataRetriever for efficient frame extraction
    - MediaStore integration for photo library access
    - Scoped storage support for Android 10+ (API 29+)
    - Coroutines for non-blocking async operations
    - Bitmap manipulation and compression optimization
    - Proper permission handling for storage access
    - File operations with automatic directory creation

- 🔧 **Developer experience features**

  - Full TypeScript support with comprehensive type definitions
  - Detailed JSDoc comments with usage examples for all methods
  - Proper error handling with descriptive error messages and error codes
  - Cross-platform API consistency with identical interfaces
  - Tree-shaking support for optimal bundle size
  - Debug tools and testing utilities for development
  - Class-based API alternative (`VideoScreenshotPluginClass`) for advanced usage

- 📖 **Documentation and examples**

  - Comprehensive README with complete API reference and advanced usage patterns
  - Step-by-step GETTING_STARTED guide for easy onboarding
  - PROJECT_STRUCTURE documentation for contributors and plugin developers
  - Complete example app demonstrating all features and best practices
  - Plugin development guide with patterns and best practices
  - Contributing guidelines with detailed setup instructions
  - Cross-platform setup instructions with troubleshooting

- 🧪 **Example application features**

  - Interactive demo of all plugin features with real-time testing
  - Real-time screenshot preview with image display
  - Plugin information display showing module status and capabilities
  - Error handling demonstrations with user-friendly messages
  - Modern React Native UI with proper loading states and feedback
  - Cross-platform compatibility testing and validation
  - Debug tools integration for development and troubleshooting

- 🏗️ **Development infrastructure**
  - TypeScript configuration with strict mode and comprehensive checking
  - ESLint configuration with React Native and TypeScript rules
  - Prettier code formatting with consistent style
  - Jest testing framework with unit tests
  - Git ignore patterns optimized for React Native projects
  - Package.json with proper peer dependencies and scripts
  - CocoaPods podspec for iOS integration with react-native-video
  - Gradle build configuration for Android with proper dependencies

### Technical Details

#### iOS Implementation

- **Framework**: AVFoundation with Swift and Objective-C bridge
- **Architecture**: Integrates with react-native-video plugin architecture
- **Features**:
  - AVAssetImageGenerator for precise frame extraction at specific timestamps
  - PHPhotoLibrary integration with proper authorization request handling
  - FileManager operations with intermediate directory creation and cleanup
  - Proper video transform handling for correct orientation
  - Memory management with automatic bitmap and resource cleanup
  - Main queue operations for UI-related tasks and background processing
  - Comprehensive error handling with specific error codes and recovery suggestions
  - Integration with ReactNativeVideoManager for player lifecycle management

#### Android Implementation

- **Framework**: ExoPlayer integration with Kotlin
- **Architecture**: Implements RNVExoplayerPlugin interface for seamless integration
- **Features**:
  - MediaMetadataRetriever for efficient frame extraction from video streams
  - MediaStore integration with scoped storage support for modern Android
  - Coroutines for non-blocking async operations and improved performance
  - Bitmap manipulation and compression with memory optimization
  - Proper permission handling for storage access across different Android versions
  - File operations with automatic directory creation and error recovery
  - Integration with ReactNativeVideoManager for player instance tracking

#### Cross-Platform Features

- Consistent API design across both iOS and Android platforms
- Unified error handling with standardized error codes and messages
- Similar performance characteristics with optimized implementations
- Matching feature set and capabilities on both platforms
- Identical TypeScript interfaces for seamless cross-platform development
- Comprehensive debugging tools available on both platforms

### Plugin Development Showcase

This plugin demonstrates several important patterns for React Native Video plugin development:

1. **Plugin Registration**: Proper registration with ReactNativeVideoManager on both platforms
2. **Player Lifecycle Management**: Handling player creation and destruction events
3. **Native Bridge Integration**: Exposing native functionality to JavaScript with Promise-based APIs
4. **Comprehensive Error Handling**: Detailed error handling with proper error codes and recovery
5. **Type Safety**: Full TypeScript support with detailed type definitions and JSDoc
6. **Cross-Platform Consistency**: Consistent API and behavior across iOS and Android
7. **Documentation Excellence**: Comprehensive documentation serving as educational resource
8. **Testing Integration**: Example app for manual testing and validation of all features
9. **Development Tools**: Debug utilities and testing functions for development workflow
10. **Performance Optimization**: Memory management and resource cleanup best practices

### API Interfaces

```typescript
interface ScreenshotOptions {
  format?: 'jpeg' | 'png'; // Output image format
  quality?: number; // JPEG quality (0-1)
  maxWidth?: number; // Maximum width in pixels
  maxHeight?: number; // Maximum height in pixels
  includeTimestamp?: boolean; // Include video timestamp in result
}

interface ScreenshotResult {
  base64: string; // Base64 encoded image data
  uri?: string; // File URI (when saved to device)
  width: number; // Image width in pixels
  height: number; // Image height in pixels
  timestamp?: number; // Video timestamp in seconds
  size?: number; // File size in bytes (when saved)
  platform?: string; // Platform information
  message?: string; // Status or error message
  isTestMode?: boolean; // Whether captured in test mode
  reason?: string; // Additional context information
  playerStatus?: number; // Player status code
  error?: string; // Error details if capture failed
  waitTime?: number; // Time waited for player to be ready
}

interface VideoRef {
  videoId: string; // Unique identifier for video instance
}

interface ModuleInfo {
  name: string; // Module name
  version: string; // Module version
  platform: string; // Current platform (iOS/Android)
  playersRegistered: number; // Number of registered players
  availablePlayerIds: string[]; // List of available player IDs
  timestamp: number; // Current timestamp
}

interface TestResult {
  status: string; // Test status
  message: string; // Test result message
  timestamp: number; // Test execution timestamp
  platform: string; // Platform where test was executed
}

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

### Requirements

- React Native >= 0.68.0
- react-native-video >= 5.0.0
- iOS 11.0+ (supports iOS 11.0 through latest iOS versions)
- Android API 21+ (supports Android 5.0 through latest Android versions)
- Node.js >= 16 for development
- Xcode 12+ for iOS development
- Android Studio for Android development

### Platform Permissions

- **iOS**: `NSPhotoLibraryAddUsageDescription` for photo library access
- **Android**: Automatic permission handling for storage access with runtime permission requests

### Installation Requirements

```bash
npm install react-native-video-screenshot-plugin
npm install react-native-video  # Peer dependency

# iOS
cd ios && pod install

# Android (auto-linking supported)
# Manual linking instructions available if needed
```

---

## Future Roadmap

### Planned Features (v1.1.0)

- 🎬 **Video frame sequences**: Capture multiple frames at different timestamps with batch operations
- 🎨 **Image filters**: Apply basic filters and effects to screenshots (brightness, contrast, saturation)
- 📊 **Enhanced metadata extraction**: Extract comprehensive video metadata (duration, bitrate, codec info)
- 🔄 **Background processing**: Support for background screenshot generation without blocking UI
- 📐 **Custom crop regions**: Specify custom crop areas within video frames
- 🎯 **Precise timestamp capture**: Capture frames at exact timestamps with frame-accurate seeking

### Planned Features (v1.2.0)

- 📐 **Custom aspect ratios**: Crop screenshots to specific aspect ratios with positioning control
- 🖼️ **Thumbnail generation**: Generate video thumbnails with customizable sizes and intervals
- 📱 **New Architecture**: Full support for React Native new architecture (Fabric/TurboModules)
- 🎪 **Advanced image processing**: Integration with image processing libraries for advanced effects
- 📊 **Performance analytics**: Built-in performance monitoring and optimization tools
- 🔗 **Plugin ecosystem**: Better integration with other React Native Video plugins

### Long-term Goals

- 🌐 **Web support**: Basic web platform support for React Native Web applications
- 📈 **Performance optimization**: Further performance improvements and memory usage optimization
- 🎥 **Video editing**: Basic video editing capabilities (trimming, merging, effects)
- 🤖 **AI integration**: AI-powered features like automatic thumbnail selection and content analysis
- 🔌 **Plugin marketplace**: Contribute to a broader ecosystem of React Native Video plugins
- 📱 **Multi-platform**: Explore support for additional platforms (Windows, macOS)

---

## Plugin Development Education

This plugin serves as a comprehensive educational resource for the React Native Video community:

### Learning Resources Provided

- **Complete plugin architecture** implementation following RNV best practices
- **Cross-platform development** patterns with iOS and Android native code
- **TypeScript integration** with comprehensive type definitions and JSDoc
- **Testing strategies** with example app and manual testing approaches
- **Documentation patterns** for open-source plugin development
- **Error handling** best practices for production-ready plugins
- **Performance optimization** techniques for native module development

### Contributing to the RNV Ecosystem

This plugin welcomes contributions and serves as a learning platform:

- **Educational focus**: Help developers learn plugin development patterns
- **Community driven**: Encourage contributions from developers of all skill levels
- **Best practices**: Demonstrate and evolve best practices for the community
- **Plugin templates**: Serve as a template for new plugin development
- **Knowledge sharing**: Document patterns and solutions for common challenges

---

## Contributing

This plugin serves as an educational example for the React Native Video community. Contributions are welcome and help improve both this plugin and the overall plugin ecosystem.

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup and comprehensive guidelines.

### Areas for Contribution

- 🆕 **New Features**: Additional screenshot formats, video effects, and processing capabilities
- 🐛 **Bug Fixes**: Platform-specific issues, performance improvements, and edge case handling
- 📚 **Documentation**: Enhanced examples, tutorials, API improvements, and educational content
- 🧪 **Testing**: More comprehensive test coverage, automated testing, and validation tools
- 🏗️ **Architecture**: New React Native architecture support and performance optimization
- 🎨 **Examples**: Additional example implementations and use case demonstrations

## License

MIT License - see [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- [react-native-video](https://github.com/react-native-video/react-native-video) team for the excellent video player and plugin architecture
- React Native community for the amazing ecosystem and continuous innovation
- Plugin developers who contribute to the RNV ecosystem and share knowledge
- Contributors and users who help improve this plugin and provide valuable feedback
