# Contributing to React Native Video Screenshot Plugin

Thank you for your interest in contributing! This document provides guidelines for contributing to this plugin and serves as an example for other React Native Video plugins.

## 🎯 Plugin Purpose

This plugin serves dual purposes:

1. **Functional**: Provides comprehensive screenshot capabilities for react-native-video
2. **Educational**: Demonstrates best practices for RNV plugin development

## 🚀 Development Setup

### Prerequisites

- Node.js >= 16
- React Native development environment (0.68.0+)
- iOS development tools (Xcode 12+, CocoaPods)
- Android development tools (Android Studio, SDK API 21+)

### Setup Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/Pieczasz/react-native-video-screenshot-plugin.git
   cd react-native-video-screenshot-plugin
   ```

2. **Install dependencies**

   ```bash
   npm install
   ```

3. **Build the plugin**

   ```bash
   npm run build
   ```

4. **Set up the example app**

   ```bash
   cd examples/bare
   npm install

   # iOS setup
   cd ios && pod install && cd ..
   ```

5. **Run the example app**
   ```bash
   npm run ios
   # or
   npm run android
   ```

## 🏗️ Project Structure

Understanding the structure helps with contributions:

```
react-native-video-screenshot-plugin/
├── src/
│   ├── index.tsx                 # TypeScript API & comprehensive types
│   └── __tests__/                # Unit tests
├── lib/                          # Compiled output (auto-generated)
├── ios/
│   ├── VideoScreenshotPlugin.swift          # Main iOS implementation
│   ├── VideoScreenshotPlugin.h              # Objective-C header
│   ├── VideoScreenshotPlugin.m              # Objective-C bridge
│   └── VideoScreenshotPlugin-Bridging-Header.h
├── android/
│   └── src/main/java/com/videoscreenshotplugin/
│       ├── VideoScreenshotPluginModule.kt    # Main Android implementation
│       └── VideoScreenshotPluginPackage.kt   # Android package registration
├── examples/
│   ├── bare/                     # Bare React Native example
│   │   ├── App.tsx               # Complete example implementation
│   │   ├── android/              # Android project
│   │   ├── ios/                  # iOS project
│   │   └── package.json
│   └── expo-example/             # Future Expo example
├── scripts/                      # Build and utility scripts
├── react-native-video-screenshot-plugin.podspec    # iOS dependency spec
├── README.md                     # Main documentation
├── GETTING_STARTED.md            # Step-by-step setup guide
├── PROJECT_STRUCTURE.md          # Architecture documentation
├── CONTRIBUTING.md               # This file
└── CHANGELOG.md                  # Version history
```

## 🔧 Making Changes

### TypeScript Changes (src/index.tsx)

The main API includes these function categories:

#### Core Screenshot Functions

- `captureScreenshot()` - Basic screenshot capture
- `captureScreenshotWhenReady()` - Screenshot with retry mechanism
- `saveScreenshotToLibrary()` - Save to device photo library
- `saveScreenshotToPath()` - Save to custom file path

#### Video Information Functions

- `isScreenshotSupported()` - Check if video supports screenshots
- `getVideoDimensions()` - Get video dimensions
- `listAvailableVideos()` - List all available video players

#### Development & Testing Functions

- `testMethod()` - Test native module functionality
- `getModuleInfo()` - Get comprehensive module information
- `registerPlayer()` - Register test players
- `debugListPlayers()` - Debug player registration

When making changes:

- Update method signatures and comprehensive types
- Ensure JSDoc comments include usage examples
- Export types for better developer experience
- Add integration examples in comments
- Update interfaces to match native implementations

### iOS Changes (ios/)

- Follow Swift best practices and modern patterns
- Use proper error handling with Promise reject/resolve
- Clean up resources appropriately (memory management)
- Handle permissions correctly with clear user messaging
- Integrate properly with RNV plugin architecture
- Use main queue for UI operations, background for processing

### Android Changes (android/)

- Use Kotlin coroutines for async operations
- Follow Android storage best practices (scoped storage)
- Handle permissions and Android 13+ requirements
- Implement proper error handling with detailed messages
- Use null safety patterns throughout
- Integrate with ExoPlayer plugin system correctly

### Example App Changes (examples/bare/)

- Demonstrate new features with clear UI
- Show comprehensive error handling patterns
- Include loading states and user feedback
- Test on both platforms thoroughly
- Add debug tools and testing utilities
- Show best practices for real-world usage

## 📝 Coding Standards

### TypeScript

- Use strict TypeScript configuration
- Provide comprehensive type definitions for all interfaces
- Include detailed JSDoc comments with usage examples
- Export interfaces and types for external use
- Follow consistent naming conventions
- Use proper async/await patterns

Example:

````typescript
/**
 * Captures a screenshot from the current video frame
 * @param videoRef Reference to the video player instance
 * @param options Screenshot configuration options
 * @returns Promise resolving to screenshot data with metadata
 * @example
 * ```typescript
 * const result = await captureScreenshot(
 *   { videoId: 'my-video' },
 *   { format: 'jpeg', quality: 0.8 }
 * );
 * ```
 */
export function captureScreenshot(
  videoRef: VideoRef,
  options: ScreenshotOptions = {}
): Promise<ScreenshotResult>;
````

### Swift (iOS)

- Use modern Swift patterns and async/await where appropriate
- Implement proper memory management and resource cleanup
- Handle main/background thread operations correctly
- Follow iOS Human Interface Guidelines for permissions
- Use proper error handling with NSError
- Integrate with AVFoundation best practices

### Kotlin (Android)

- Use coroutines for async operations and proper scope management
- Implement proper null safety throughout
- Follow Material Design guidelines for UI interactions
- Handle Android permission model correctly (runtime permissions)
- Use proper lifecycle management
- Implement MediaStore integration for modern Android

### Documentation

- Update README.md for new features with examples
- Include comprehensive code examples in documentation
- Document breaking changes with migration guides
- Update API reference section with new methods
- Ensure GETTING_STARTED.md reflects new setup requirements

## 🧪 Testing

### Manual Testing

1. **Test in example app** on both platforms
2. **Verify error handling** scenarios (permissions, invalid videos)
3. **Test permission handling** (denial, granting, already granted)
4. **Verify memory usage** and cleanup
5. **Test different video formats** and sources
6. **Test on different device configurations**

### Automated Testing

- **TypeScript compilation**: `npm run build`
- **Linting**: `npm run lint` and `npm run lint:fix`
- **Type checking**: `npm run typecheck`
- **Unit tests**: `npm test` and `npm run test:watch`
- **Example app builds**: Ensure builds on both platforms

### Testing Checklist

- [ ] All API functions work as documented
- [ ] Error handling provides meaningful messages
- [ ] Permissions are handled gracefully
- [ ] Memory usage is reasonable
- [ ] Cross-platform behavior is consistent
- [ ] Example app demonstrates all features
- [ ] Documentation is accurate and complete

## 📦 Plugin Development Guidelines

This plugin demonstrates several important patterns for RNV plugin development:

### 1. Plugin Registration

```swift
// iOS - Register with ReactNativeVideoManager
@objc(VideoScreenshotPlugin)
class VideoScreenshotPlugin: NSObject, RCTBridgeModule {
    override init() {
        super.init()
        ReactNativeVideoManager.shared().registerPlugin(self)
    }
}
```

```kotlin
// Android - Implement RNVExoplayerPlugin interface
class VideoScreenshotPluginModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RNVExoplayerPlugin {

    init {
        ReactNativeVideoManager.getInstance().registerPlugin(this)
    }
}
```

### 2. Player Lifecycle Management

```swift
// iOS - Handle player creation/destruction
func onVideoPlayerCreated(playerId: String, player: AVPlayer) {
    players[playerId] = player
}

func onVideoPlayerDestroyed(playerId: String) {
    players.removeValue(forKey: playerId)
}
```

### 3. Comprehensive Error Handling

```typescript
// TypeScript - Comprehensive error handling
try {
  // Check if video supports screenshots
  const supported = await isScreenshotSupported({ videoId: 'my-video' });
  if (!supported) {
    throw new Error('Screenshot not supported for this video');
  }

  const result = await captureScreenshot({ videoId: 'my-video' }, options);
  // Handle success
} catch (error) {
  // Handle specific error types with helpful messages
  console.error('Screenshot failed:', error);
}
```

### 4. Type Safety and Developer Experience

```typescript
// Comprehensive TypeScript definitions
export interface ScreenshotOptions {
  format?: 'jpeg' | 'png'; // Output format
  quality?: number; // JPEG quality (0-1)
  maxWidth?: number; // Max width in pixels
  maxHeight?: number; // Max height in pixels
  includeTimestamp?: boolean; // Include video timestamp
}

export interface ScreenshotResult {
  base64: string; // Base64 encoded image
  uri?: string; // File URI (when saved)
  width: number; // Image width in pixels
  height: number; // Image height in pixels
  timestamp?: number; // Video timestamp in seconds
  size?: number; // File size in bytes
  platform?: string; // Platform information
  message?: string; // Status or error message
  // ... additional debugging fields
}
```

## 🎨 UI/UX Guidelines

### Example App Design

- Use native platform design patterns (Material Design, iOS HIG)
- Provide clear, immediate feedback for all actions
- Handle loading states gracefully with proper indicators
- Show meaningful error messages with recovery suggestions
- Include comprehensive preview functionality
- Demonstrate debugging tools and information display

### Permission Handling

- Request permissions at appropriate times (just-in-time)
- Provide clear explanations for permission needs
- Handle permission denial gracefully with alternatives
- Follow platform-specific permission patterns
- Test permission flows thoroughly

## 🔍 Code Review Process

### Before Submitting

1. **Test thoroughly** on both iOS and Android
2. **Update documentation** for any API changes
3. **Run all linting** and fix any issues: `npm run lint:fix`
4. **Verify example app** works correctly and demonstrates changes
5. **Check TypeScript compilation**: `npm run build`
6. **Test edge cases** and error scenarios
7. **Verify memory usage** and resource cleanup

### Pull Request Guidelines

- **Provide clear description** of changes and motivation
- **Include screenshots/videos** for UI changes
- **Reference any related issues** with proper linking
- **Update relevant documentation** (README, GETTING_STARTED, etc.)
- **Add or update tests** if applicable
- **Ensure cross-platform compatibility**

### Review Criteria

- Code follows established patterns and conventions
- Cross-platform compatibility is maintained
- Proper error handling is implemented with helpful messages
- Documentation is updated and accurate
- Example app demonstrates changes effectively
- TypeScript types are comprehensive and accurate

## 🐛 Bug Reports

When reporting bugs:

1. **Test in the example app first** to isolate the issue
2. **Provide clear reproduction steps** with minimal test case
3. **Include platform and version info** (RN, plugin, OS versions)
4. **Share relevant logs/error messages** with full stack traces
5. **Check existing issues** to avoid duplicates

### Bug Report Template

````markdown
**Environment:**

- Platform: iOS/Android
- React Native Version: x.x.x
- Plugin Version: x.x.x
- OS Version: iOS x.x / Android API xx
- Device: iPhone/Pixel/etc.

**Description:**
Clear and concise description of the bug

**Steps to Reproduce:**

1. Step 1
2. Step 2
3. Step 3

**Expected Behavior:**
What should happen

**Actual Behavior:**
What actually happens

**Code Sample:**

```typescript
// Minimal code sample that reproduces the issue
```
````

**Logs/Screenshots:**
Relevant error logs, console output, or screenshots

````

## 💡 Feature Requests

For new features:

1. **Check existing issues** and roadmap first
2. **Provide clear use case** description with real-world examples
3. **Consider cross-platform implications** and implementation challenges
4. **Suggest implementation approach** if you have ideas
5. **Be willing to contribute** or help with testing

### Feature Request Template

```markdown
**Feature Description:**
Clear description of the proposed feature

**Use Case:**
Real-world scenario where this would be useful

**Proposed API:**
```typescript
// Example API design
````

**Platform Considerations:**
How this would work on iOS vs Android

**Implementation Notes:**
Any thoughts on implementation approach

```

## 🤝 Community Guidelines

- **Be respectful and inclusive** in all interactions
- **Help others learn** from this plugin example
- **Share knowledge** about plugin development patterns
- **Provide constructive feedback** with specific suggestions
- **Follow the Code of Conduct** and GitHub community guidelines
- **Contribute to the educational aspect** of this plugin

## 📚 Resources

### React Native Video Plugin Development

- [RNV Plugin Architecture Documentation](https://docs.thewidlarzgroup.com/react-native-video/)
- [React Native Documentation](https://reactnative.dev/)
- [React Native Video Repository](https://github.com/react-native-video/react-native-video)

### Platform-Specific Resources

- [iOS AVFoundation Framework](https://developer.apple.com/documentation/avfoundation)
- [Android ExoPlayer Documentation](https://exoplayer.dev/)
- [React Native Native Modules](https://reactnative.dev/docs/native-modules-intro)
- [Swift Documentation](https://docs.swift.org/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)

### Development Tools

- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [ESLint React Native Rules](https://github.com/Intellicode/eslint-plugin-react-native)
- [React Native Testing](https://reactnative.dev/docs/testing-overview)

## ❓ Questions?

If you have questions about contributing:

- **Open a discussion** on GitHub for general questions
- **Check existing documentation** thoroughly first
- **Look at the example implementation** for usage patterns
- **Ask in the React Native Video community** for plugin-specific questions
- **Review similar plugins** for inspiration and patterns

## 🎯 Development Roadmap

### Current Focus Areas

- **Performance optimization** - Memory usage and speed improvements
- **Error handling** - More comprehensive error scenarios
- **Documentation** - Even more examples and tutorials
- **Testing** - Automated testing framework
- **New Architecture** - React Native new architecture support

### How to Contribute

1. **Pick an area** that interests you
2. **Start small** with documentation or example improvements
3. **Ask questions** if you're unsure about implementation
4. **Share your expertise** in areas you know well
5. **Help others** learn from your contributions

---

**Thank you for contributing to the React Native Video ecosystem! 🎉**

*Your contributions help make React Native Video more powerful and accessible for developers worldwide.*
```
