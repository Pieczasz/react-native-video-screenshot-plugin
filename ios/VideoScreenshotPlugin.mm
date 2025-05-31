/*
 * React Native Video Screenshot Plugin - iOS Objective-C Bridge
 * 
 * This file provides the Objective-C bridge between React Native and the Swift implementation
 * of the VideoScreenshotPlugin. It exposes the Swift methods to React Native's module system
 * and handles the method call routing from JavaScript to native code.
 * 
 * Bridge Architecture:
 * - Uses RCT_EXTERN_MODULE macro to expose the Swift class to React Native
 * - Uses RCT_EXTERN_METHOD macros to expose individual methods
 * - Handles Promise-based asynchronous method calls
 * - Maintains method signatures that match the JavaScript API
 * 
 * Method Categories:
 * 1. Screenshot Capture Methods - Core functionality for taking screenshots
 * 2. File System Methods - Saving screenshots to various locations
 * 3. Utility Methods - Support functions for debugging and information
 * 
 * Integration with react-native-video Plugin System:
 * - Methods accept player IDs that match react-native-video's player instances
 * - Plugin receives player lifecycle callbacks from react-native-video
 * - Screenshots are captured from active AVPlayer instances managed by react-native-video
 */

#import <React/RCTBridgeModule.h>

// ================================
// MODULE REGISTRATION
// ================================

/**
 * Register the VideoScreenshotPlugin Swift class with React Native
 * This tells React Native about our plugin and makes it available for JavaScript calls
 */
@interface RCT_EXTERN_MODULE(VideoScreenshotPlugin, NSObject)

// ================================
// MODULE CONFIGURATION
// ================================

/**
 * Specify that this module requires main queue setup
 * This ensures proper integration with React Native's bridge system
 */
+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

/**
 * Export module constants to JavaScript
 * These will be available immediately when the module is imported
 */
- (NSDictionary *)constantsToExport
{
    return @{
        @"name": @"VideoScreenshotPlugin",
        @"version": @"1.0.0",
        @"supportsTurboModule": @YES
    };
}

// ================================
// TEST METHOD FOR DEBUGGING
// ================================

/**
 * Test method to verify the module is working
 */
RCT_EXTERN_METHOD(testMethod:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

/**
 * Manual method to register a player for testing
 */
RCT_EXTERN_METHOD(registerPlayer:(NSString *)playerId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

// ================================
// CORE SCREENSHOT METHODS
// ================================

/**
 * Captures a screenshot from the specified video player and returns it as base64
 * 
 * JavaScript Usage:
 * const result = await VideoScreenshotPlugin.captureScreenshot(videoId, options);
 * 
 * @param videoId NSString - The unique identifier for the video player (matches react-native-video player ID)
 * @param options NSDictionary - Configuration options (format, quality, dimensions, etc.)
 * @param resolve RCTPromiseResolveBlock - Promise resolver for successful capture
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(captureScreenshot:(NSString *)videoId
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

// ================================
// FILE SYSTEM METHODS
// ================================

/**
 * Captures a screenshot and saves it to the device's photo library
 * Requires photo library permissions to be granted
 * 
 * JavaScript Usage:
 * const result = await VideoScreenshotPlugin.saveScreenshotToLibrary(videoId, options);
 * 
 * @param videoId NSString - The unique identifier for the video player
 * @param options NSDictionary - Configuration options for the screenshot
 * @param resolve RCTPromiseResolveBlock - Promise resolver with saved image information
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(saveScreenshotToLibrary:(NSString *)videoId
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

/**
 * Captures a screenshot and saves it to a specific file path
 * Provides direct file system access for custom storage locations
 * 
 * JavaScript Usage:
 * const result = await VideoScreenshotPlugin.saveScreenshotToPath(videoId, filePath, options);
 * 
 * @param videoId NSString - The unique identifier for the video player
 * @param filePath NSString - Absolute file path where the screenshot should be saved
 * @param options NSDictionary - Configuration options for the screenshot
 * @param resolve RCTPromiseResolveBlock - Promise resolver with file information
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(saveScreenshotToPath:(NSString *)videoId
                  filePath:(NSString *)filePath
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

// ================================
// INFORMATION AND UTILITY METHODS
// ================================

/**
 * Checks if screenshot capture is currently supported for the given player
 * Useful for enabling/disabling screenshot UI elements
 * 
 * JavaScript Usage:
 * const isSupported = await VideoScreenshotPlugin.isScreenshotSupported(videoId);
 * 
 * @param videoId NSString - The unique identifier for the video player to check
 * @param resolve RCTPromiseResolveBlock - Resolves with boolean indicating support status
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(isScreenshotSupported:(NSString *)videoId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

/**
 * Gets the current video dimensions for the specified player
 * Useful for calculating aspect ratios and sizing screenshot outputs
 * 
 * JavaScript Usage:
 * const dimensions = await VideoScreenshotPlugin.getVideoDimensions(videoId);
 * // Returns: { width: number, height: number }
 * 
 * @param videoId NSString - The unique identifier for the video player
 * @param resolve RCTPromiseResolveBlock - Resolves with width and height information
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(getVideoDimensions:(NSString *)videoId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

// ================================
// DEBUGGING AND DEVELOPMENT METHODS
// ================================

/**
 * Lists all currently available video players
 * Useful for debugging and understanding which players are active
 * 
 * JavaScript Usage:
 * const availableVideos = await VideoScreenshotPlugin.listAvailableVideos();
 * // Returns: string[] - Array of video player IDs
 * 
 * @param resolve RCTPromiseResolveBlock - Resolves with array of available player IDs
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(listAvailableVideos:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

/**
 * Debug method to inspect the current state of registered players
 * Provides detailed logging and should only be used during development
 * 
 * JavaScript Usage:
 * const playerList = await VideoScreenshotPlugin.debugListPlayers();
 * 
 * @param resolve RCTPromiseResolveBlock - Resolves with detailed player information
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(debugListPlayers:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

/**
 * Get module information for debugging
 * 
 * JavaScript Usage:
 * const moduleInfo = await VideoScreenshotPlugin.getModuleInfo();
 * 
 * @param resolve RCTPromiseResolveBlock - Resolves with module information
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(getModuleInfo:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

/**
 * Capture screenshot with automatic retry when video becomes ready
 * This method waits for the video to be ready before taking a screenshot
 * 
 * JavaScript Usage:
 * const result = await VideoScreenshotPlugin.captureScreenshotWhenReady(videoId, options);
 * 
 * @param videoId NSString - The unique identifier for the video player
 * @param options NSDictionary - Configuration options for the screenshot
 * @param resolve RCTPromiseResolveBlock - Promise resolver for successful capture
 * @param reject RCTPromiseRejectBlock - Promise rejecter for error handling
 */
RCT_EXTERN_METHOD(captureScreenshotWhenReady:(NSString *)videoId
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

@end

/*
 * Bridge Configuration Notes for react-native-video Plugin Integration:
 * 
 * 1. Method Naming: All methods use camelCase to match JavaScript conventions
 * 2. Parameter Types: Use NSString, NSDictionary, NSArray for complex types
 * 3. Promises: All asynchronous methods use Promise pattern with resolve/reject
 * 4. Thread Safety: React Native handles thread marshalling between JS and native
 * 5. Error Handling: Use reject() for errors, resolve() for successful responses
 * 
 * Plugin Integration Flow:
 * 1. react-native-video creates AVPlayer instances and assigns unique IDs
 * 2. Plugin receives these player instances through onInstanceCreated() callback
 * 3. JavaScript calls screenshot methods with the player ID
 * 4. Plugin looks up the corresponding AVPlayer instance and captures screenshot
 * 5. Plugin returns captured image data through Promise resolution
 * 
 * Common Parameter Formats:
 * 
 * options Dictionary can contain:
 * - format: "jpeg" | "png" - Output image format
 * - quality: 0.0-1.0 - JPEG compression quality (ignored for PNG)
 * - maxWidth: number - Maximum width in pixels
 * - maxHeight: number - Maximum height in pixels
 * - includeTimestamp: boolean - Whether to include playback timestamp
 * 
 * Return Value Formats:
 * 
 * Screenshot results typically contain:
 * - base64: string - Base64 encoded image data
 * - width: number - Image width in pixels
 * - height: number - Image height in pixels
 * - timestamp?: number - Playback timestamp in seconds (if requested)
 * - uri?: string - File path or photo library identifier (for save operations)
 * - size?: number - File size in bytes (for file save operations)
 * 
 * Usage Example with react-native-video:
 * 
 * ```javascript
 * import Video from 'react-native-video';
 * import VideoScreenshotPlugin from 'react-native-video-screenshot-plugin';
 * 
 * // In your component:
 * const videoRef = useRef(null);
 * 
 * const takeScreenshot = async () => {
 *   try {
 *     // Get the player ID from the Video component
 *     const playerId = videoRef.current?.getPlayerId?.();
 *     
 *     if (playerId) {
 *       const screenshot = await VideoScreenshotPlugin.captureScreenshot(playerId, {
 *         format: 'jpeg',
 *         quality: 0.8
 *       });
 *       
 *       console.log('Screenshot captured:', screenshot.base64);
 *     }
 *   } catch (error) {
 *     console.error('Screenshot failed:', error);
 *   }
 * };
 * 
 * <Video
 *   ref={videoRef}
 *   source={{ uri: 'https://example.com/video.mp4' }}
 *   // ... other props
 * />
 * ```
 */