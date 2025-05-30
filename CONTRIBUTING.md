# Contributing to React Native Video Screenshot Plugin

Thank you for your interest in contributing! This document provides guidelines for contributing to this plugin and serves as an example for other React Native Video plugins.

## 🎯 Plugin Purpose

This plugin serves dual purposes:

1. **Functional**: Provides screenshot capabilities for react-native-video
2. **Educational**: Demonstrates best practices for RNV plugin development

## 🚀 Development Setup

### Prerequisites

- Node.js >= 16
- React Native development environment
- iOS development tools (for iOS testing)
- Android development tools (for Android testing)

### Setup Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/pieczasz/react-native-video-screenshot-plugin.git
   cd react-native-video-screenshot-plugin
   ```

2. **Install dependencies**

   ```bash
   npm install
   ```

3. **Set up the example app**

   ```bash
   cd example
   npm install
   cd ios && pod install && cd ..
   ```

4. **Run the example app**
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
│   └── index.tsx                 # TypeScript API & types
├── ios/
│   ├── VideoScreenshotPlugin.swift      # iOS implementation
│   ├── VideoScreenshotPlugin.mm         # Objective-C bridge
│   └── VideoScreenshotPlugin-Bridging-Header.h
├── android/
│   └── src/main/java/com/videoscreenshotplugin/
│       ├── VideoScreenshotPluginModule.kt    # Android implementation
│       └── VideoScreenshotPluginPackage.kt   # Android package
├── example/                      # Example app demonstrating usage
│   ├── App.tsx                   # Main example implementation
│   └── package.json
└── react-native-video-screenshot-plugin.podspec    # iOS dependency spec
```

## 🔧 Making Changes

### TypeScript Changes (src/index.tsx)

- Update method signatures and types
- Ensure JSDoc comments are comprehensive
- Export types for better developer experience
- Add usage examples in comments

### iOS Changes (ios/)

- Follow Swift best practices
- Use proper error handling with Promise reject/resolve
- Clean up resources appropriately
- Handle permissions correctly

### Android Changes (android/)

- Use Kotlin coroutines for async operations
- Follow Android storage best practices
- Handle permissions and scoped storage
- Implement proper error handling

### Example App Changes (example/)

- Demonstrate new features
- Show error handling patterns
- Include loading states and user feedback
- Test on both platforms

## 📝 Coding Standards

### TypeScript

- Use strict TypeScript configuration
- Provide comprehensive type definitions
- Include JSDoc comments with examples
- Export interfaces and types

### Swift (iOS)

- Use modern Swift patterns
- Implement proper memory management
- Handle main/background thread operations correctly
- Follow iOS Human Interface Guidelines for permissions

### Kotlin (Android)

- Use coroutines for async operations
- Implement proper null safety
- Follow Material Design guidelines
- Handle Android permission model correctly

### Documentation

- Update README.md for new features
- Include code examples in documentation
- Document breaking changes
- Update API reference section

## 🧪 Testing

### Manual Testing

1. Test in the example app on both platforms
2. Verify error handling scenarios
3. Test permission handling
4. Verify memory usage and cleanup

### Automated Testing

- Run TypeScript compilation: `npm run build`
- Run linting: `npm run lint`
- Ensure example app builds and runs

## 📦 Plugin Development Guidelines

This plugin demonstrates several important patterns for RNV plugin development:

### 1. Plugin Registration

```swift
// iOS - Register with ReactNativeVideoManager
override init() {
    super.init()
    ReactNativeVideoManager.shared.registerPlugin(plugin: self)
}
```

```kotlin
// Android - Implement RNVExoplayerPlugin interface
class VideoScreenshotPluginModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RNVExoplayerPlugin
```

### 2. Player Lifecycle Management

```swift
// iOS - Handle player creation/destruction
override func onInstanceCreated(id: String, player: AVPlayer) {
    players[id] = player
}

override func onInstanceRemoved(id: String, player: AVPlayer) {
    players.removeValue(forKey: id)
}
```

### 3. Error Handling

```typescript
// TypeScript - Comprehensive error handling
try {
  const result = await captureScreenshot(videoRef, options);
  // Handle success
} catch (error) {
  // Handle specific error types
  console.error('Screenshot failed:', error);
}
```

### 4. Type Safety

```typescript
// Comprehensive TypeScript definitions
export interface ScreenshotOptions {
  format?: 'jpeg' | 'png';
  quality?: number;
  maxWidth?: number;
  maxHeight?: number;
  includeTimestamp?: boolean;
}
```

## 🎨 UI/UX Guidelines

### Example App Design

- Use native platform design patterns
- Provide clear feedback for all actions
- Handle loading states gracefully
- Show meaningful error messages
- Include preview functionality

### Permission Handling

- Request permissions at appropriate times
- Provide clear explanations for permission needs
- Handle permission denial gracefully
- Follow platform-specific permission patterns

## 🔍 Code Review Process

### Before Submitting

1. **Test thoroughly** on both platforms
2. **Update documentation** for any changes
3. **Run linting** and fix any issues
4. **Verify example app** works correctly
5. **Check TypeScript compilation**

### Pull Request Guidelines

- Provide clear description of changes
- Include screenshots/videos for UI changes
- Reference any related issues
- Update relevant documentation
- Add tests if applicable

### Review Criteria

- Code follows established patterns
- Cross-platform compatibility maintained
- Proper error handling implemented
- Documentation is updated
- Example app demonstrates changes

## 🐛 Bug Reports

When reporting bugs:

1. **Provide reproduction steps**
2. **Include platform and version info**
3. **Share relevant logs/error messages**
4. **Test in the example app first**
5. **Check existing issues**

### Bug Report Template

```markdown
**Platform:** iOS/Android
**React Native Version:** x.x.x
**Plugin Version:** x.x.x

**Description:**
Clear description of the bug

**Steps to Reproduce:**

1. Step 1
2. Step 2
3. Step 3

**Expected Behavior:**
What should happen

**Actual Behavior:**
What actually happens

**Logs:**
Relevant error logs or console output
```

## 💡 Feature Requests

For new features:

1. **Check existing issues** first
2. **Provide clear use case** description
3. **Consider cross-platform implications**
4. **Suggest implementation approach**
5. **Be willing to contribute**

## 🤝 Community Guidelines

- Be respectful and inclusive
- Help others learn from this example
- Share knowledge about plugin development
- Provide constructive feedback
- Follow the Code of Conduct

## 📚 Resources

### React Native Video Plugin Development

- [RNV Plugin Architecture](https://docs.thewidlarzgroup.com/react-native-video/)
- [React Native Documentation](https://reactnative.dev/)
- [Swift Documentation](https://docs.swift.org/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)

### Platform-Specific Resources

- [iOS AVFoundation](https://developer.apple.com/documentation/avfoundation)
- [Android ExoPlayer](https://exoplayer.dev/)
- [React Native Bridge](https://reactnative.dev/docs/native-modules-intro)

## ❓ Questions?

If you have questions about contributing:

- Open a discussion on GitHub
- Check existing documentation
- Look at the example implementation
- Ask in the React Native Video community

---

**Thank you for contributing to the React Native Video ecosystem! 🎉**
