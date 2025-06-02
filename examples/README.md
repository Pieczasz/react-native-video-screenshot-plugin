# 📁 Examples Directory

This directory contains example implementations of the `react-native-video-screenshot-plugin` in different React Native environments.

## 🎯 Available Examples

### 📱 Bare React Native Example (`/bare`)

A complete working implementation demonstrating all plugin features in a standard React Native project.

**Status**: ✅ **Fully Functional**

**Features Demonstrated**:

- Video playback with react-native-video
- Screenshot capture to base64
- Save screenshots to photo library
- Save screenshots to custom file paths
- Permission handling for both platforms
- Error handling and user feedback
- Cross-platform iOS and Android support

**Best for**: Testing the plugin, understanding implementation details, and production-ready integration patterns.

[📖 View Bare Example Documentation →](./bare/README.md)

### ⚡ Expo Example (`/expo-example`)

Expo-based implementation for managed workflow projects.

**Status**: 🚧 **Project Setup Only - Not Yet Available**

This directory currently contains only the initial Expo project setup. The actual plugin integration and example implementation is not yet available.

**Planned Features**:

- Expo-compatible video screenshot functionality
- Managed workflow implementation
- Expo dev client compatibility
- Cross-platform support within Expo ecosystem

[📖 View Expo Example Documentation →](./expo-example/README.md)

## 🚀 Quick Start

### For Bare React Native Development

```bash
cd examples/bare
npm install
npx react-native run-ios # or npx react-native run-android
```

### For Expo Development

The Expo example is not yet available. Please use the bare React Native example for now.

## 📋 Prerequisites

- **Node.js** 16+ and npm 8+
- **React Native CLI** (for bare example)
- **Expo CLI** (for expo example, when available)
- **iOS**: Xcode and CocoaPods
- **Android**: Android Studio and SDK

## 🤝 Contributing

We welcome contributions to improve and expand these examples:

1. **Bare Example**: Bug fixes, feature demonstrations, improved documentation
2. **Expo Example**: Help implement the Expo integration (coming soon!)

Please ensure all examples follow the established patterns and include proper documentation.

## 📚 Documentation

- [Main Plugin Documentation](../README.md)
- [Getting Started Guide](../GETTING_STARTED.md)
- [API Reference](../README.md#api-reference)
- [Contributing Guidelines](../CONTRIBUTING.md)
