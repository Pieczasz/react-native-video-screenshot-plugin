import { NativeModules, Platform } from 'react-native';
import NativeVideoScreenshotPlugin from './NativeVideoScreenshotPlugin';

// Type definitions
export interface ScreenshotOptions {
  format?: 'jpeg' | 'png';
  quality?: number;
  maxWidth?: number;
  maxHeight?: number;
  includeTimestamp?: boolean;
}

export interface ScreenshotResult {
  base64: string;
  width: number;
  height: number;
  timestamp?: number;
  uri?: string;
  size?: number;
  platform: string;
  message?: string;
  isTestMode?: boolean;
  reason?: string;
  playerStatus?: number;
  error?: string;
  waitTime?: number;
}

export interface VideoDimensions {
  width: number;
  height: number;
}

export interface ModuleInfo {
  name: string;
  version: string;
  platform: string;
  playersRegistered: number;
  availablePlayerIds: string[];
  timestamp: number;
}

export interface PlayerRegistrationResult {
  status: string;
  playerId: string;
  message: string;
  hasCurrentItem: boolean;
  testVideoURL?: string;
  testMode: boolean;
  platform: string;
  note?: string;
}

export interface TestResult {
  status: string;
  message: string;
  timestamp: number;
  platform: string;
}

// Module detection and fallback
const LINKING_ERROR =
  `The package 'react-native-video-screenshot-plugin' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'cd ios && pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// Try to get the native module with multiple fallback strategies
function getNativeModule() {
  // Strategy 1: Try TurboModule (New Architecture)
  try {
    return NativeVideoScreenshotPlugin;
  } catch (turboError) {
    console.log('📱 TurboModule not available, trying legacy module...');

    // Strategy 2: Try Legacy NativeModules
    const legacyModule = NativeModules.VideoScreenshotPlugin;
    if (legacyModule) {
      console.log('📱 Using legacy NativeModules bridge');
      return legacyModule;
    }

    // Strategy 3: Try alternative naming
    const altModule = NativeModules.RNVideoScreenshotPlugin;
    if (altModule) {
      console.log('📱 Using alternative module name');
      return altModule;
    }

    console.error('❌ No native module found. TurboModule error:', turboError);
    throw new Error(LINKING_ERROR);
  }
}

// Get the native module instance
let nativeModule: any;
try {
  nativeModule = getNativeModule();
  console.log('✅ Native module loaded successfully');
} catch (error) {
  console.error('❌ Failed to load native module:', error);
  throw error;
}

/**
 * React Native Video Screenshot Plugin
 *
 * Provides screenshot capture functionality for react-native-video players
 * on both iOS and Android platforms.
 */
class VideoScreenshotPlugin {
  /**
   * Test if the native module is working
   */
  static async testMethod(): Promise<TestResult> {
    try {
      return await nativeModule.testMethod();
    } catch (error) {
      throw new Error(`Test method failed: ${error}`);
    }
  }

  /**
   * Get module information and status
   */
  static async getModuleInfo(): Promise<ModuleInfo> {
    try {
      return await nativeModule.getModuleInfo();
    } catch (error) {
      throw new Error(`Get module info failed: ${error}`);
    }
  }

  /**
   * Register a test player (for development/testing)
   */
  static async registerPlayer(playerId: string): Promise<PlayerRegistrationResult> {
    try {
      return await nativeModule.registerPlayer(playerId);
    } catch (error) {
      throw new Error(`Register player failed: ${error}`);
    }
  }

  /**
   * Debug method to list all registered players
   */
  static async debugListPlayers(): Promise<string[]> {
    try {
      return await nativeModule.debugListPlayers();
    } catch (error) {
      throw new Error(`Debug list players failed: ${error}`);
    }
  }

  /**
   * Capture a screenshot from the specified video player
   */
  static async captureScreenshot(
    videoId: string,
    options: ScreenshotOptions = {}
  ): Promise<ScreenshotResult> {
    try {
      return await nativeModule.captureScreenshot(videoId, options);
    } catch (error) {
      throw new Error(`Capture screenshot failed: ${error}`);
    }
  }

  /**
   * Capture a screenshot with automatic retry when video becomes ready
   */
  static async captureScreenshotWhenReady(
    videoId: string,
    options: ScreenshotOptions = {}
  ): Promise<ScreenshotResult> {
    try {
      return await nativeModule.captureScreenshotWhenReady(videoId, options);
    } catch (error) {
      throw new Error(`Capture screenshot when ready failed: ${error}`);
    }
  }

  /**
   * Capture a screenshot and save it to the device's photo library
   */
  static async saveScreenshotToLibrary(
    videoId: string,
    options: ScreenshotOptions = {}
  ): Promise<ScreenshotResult> {
    try {
      return await nativeModule.saveScreenshotToLibrary(videoId, options);
    } catch (error) {
      throw new Error(`Save screenshot to library failed: ${error}`);
    }
  }

  /**
   * Capture a screenshot and save it to a specific file path
   */
  static async saveScreenshotToPath(
    videoId: string,
    filePath: string,
    options: ScreenshotOptions = {}
  ): Promise<ScreenshotResult> {
    try {
      return await nativeModule.saveScreenshotToPath(videoId, filePath, options);
    } catch (error) {
      throw new Error(`Save screenshot to path failed: ${error}`);
    }
  }

  /**
   * Check if screenshot capture is supported for the given player
   */
  static async isScreenshotSupported(videoId: string): Promise<boolean> {
    try {
      return await nativeModule.isScreenshotSupported(videoId);
    } catch (error) {
      throw new Error(`Is screenshot supported check failed: ${error}`);
    }
  }

  /**
   * Get the current video dimensions for the specified player
   */
  static async getVideoDimensions(videoId: string): Promise<VideoDimensions> {
    try {
      return await nativeModule.getVideoDimensions(videoId);
    } catch (error) {
      throw new Error(`Get video dimensions failed: ${error}`);
    }
  }

  /**
   * List all currently available video players
   */
  static async listAvailableVideos(): Promise<string[]> {
    try {
      return await nativeModule.listAvailableVideos();
    } catch (error) {
      throw new Error(`List available videos failed: ${error}`);
    }
  }
}

export default VideoScreenshotPlugin;
