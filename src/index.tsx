import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  "The package 'react-native-video-screenshot-plugin' doesn't seem to be linked. Make sure: \n\n" +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// Get the native module directly since both iOS and Android use the old bridge pattern
const VideoScreenshotPlugin = NativeModules.VideoScreenshotPlugin
  ? NativeModules.VideoScreenshotPlugin
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export interface ScreenshotOptions {
  /**
   * Output format for the screenshot
   * @default 'jpeg'
   */
  format?: 'jpeg' | 'png';

  /**
   * Quality for JPEG format (0-1)
   * @default 0.9
   */
  quality?: number;

  /**
   * Maximum width for the screenshot (maintains aspect ratio)
   */
  maxWidth?: number;

  /**
   * Maximum height for the screenshot (maintains aspect ratio)
   */
  maxHeight?: number;

  /**
   * Include video timestamp in metadata
   * @default true
   */
  includeTimestamp?: boolean;
}

export interface ScreenshotResult {
  base64: string;
  uri?: string;
  width: number;
  height: number;
  timestamp?: number;
  size?: number;
  platform?: string;
  message?: string;
  isTestMode?: boolean;
  reason?: string;
  playerStatus?: number;
  error?: string;
  waitTime?: number;
}

export interface VideoRef {
  videoId: string;
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

/**
 * Test if the native module is working
 */
export function testMethod(): Promise<TestResult> {
  return VideoScreenshotPlugin.testMethod();
}

/**
 * Get module information and status
 */
export function getModuleInfo(): Promise<ModuleInfo> {
  return VideoScreenshotPlugin.getModuleInfo();
}

/**
 * Register a test player (for development/testing)
 */
export function registerPlayer(playerId: string): Promise<PlayerRegistrationResult> {
  return VideoScreenshotPlugin.registerPlayer(playerId);
}

/**
 * Captures a screenshot from the currently playing video frame
 *
 * @param videoRef Reference to the video player instance
 * @param options Screenshot configuration options
 * @returns Promise resolving to screenshot data
 *
 * @example
 * ```typescript
 * import { captureScreenshot } from 'react-native-video-screenshot-plugin';
 *
 * const result = await captureScreenshot(
 *   { videoId: 'my-video' },
 *   { format: 'jpeg', quality: 0.8, maxWidth: 1920 }
 * );
 * console.log('Screenshot captured:', result.base64);
 * ```
 */
export function captureScreenshot(
  videoRef: VideoRef,
  options: ScreenshotOptions = {}
): Promise<ScreenshotResult> {
  const config = {
    format: 'jpeg',
    quality: 0.9,
    includeTimestamp: true,
    ...options,
  };

  return VideoScreenshotPlugin.captureScreenshot(videoRef.videoId, config);
}

/**
 * Captures a screenshot with automatic retry when video becomes ready
 */
export function captureScreenshotWhenReady(
  videoRef: VideoRef,
  options: ScreenshotOptions = {}
): Promise<ScreenshotResult> {
  const config = {
    format: 'jpeg',
    quality: 0.9,
    includeTimestamp: true,
    ...options,
  };

  return VideoScreenshotPlugin.captureScreenshotWhenReady(videoRef.videoId, config);
}

/**
 * Captures a screenshot and saves it to the device's photo library
 *
 * @param videoRef Reference to the video player instance
 * @param options Screenshot configuration options
 * @returns Promise resolving to screenshot data with file URI
 *
 * @example
 * ```typescript
 * import { saveScreenshotToLibrary } from 'react-native-video-screenshot-plugin';
 *
 * const result = await saveScreenshotToLibrary(
 *   { videoId: 'my-video' },
 *   { format: 'png' }
 * );
 * console.log('Screenshot saved to:', result.uri);
 * ```
 */
export function saveScreenshotToLibrary(
  videoRef: VideoRef,
  options: ScreenshotOptions = {}
): Promise<ScreenshotResult> {
  const config = {
    format: 'jpeg',
    quality: 0.9,
    includeTimestamp: true,
    ...options,
  };

  return VideoScreenshotPlugin.saveScreenshotToLibrary(videoRef.videoId, config);
}

/**
 * Captures a screenshot and saves it to a custom file path
 *
 * @param videoRef Reference to the video player instance
 * @param filePath Custom file path where to save the screenshot
 * @param options Screenshot configuration options
 * @returns Promise resolving to screenshot data with file URI
 *
 * @example
 * ```typescript
 * import { saveScreenshotToPath } from 'react-native-video-screenshot-plugin';
 *
 * const documentsPath = `${DocumentDirectoryPath}/screenshots/video_capture.jpg`;
 * const result = await saveScreenshotToPath(
 *   { videoId: 'my-video' },
 *   documentsPath,
 *   { format: 'jpeg', quality: 1.0 }
 * );
 * console.log('Screenshot saved to:', result.uri);
 * ```
 */
export function saveScreenshotToPath(
  videoRef: VideoRef,
  filePath: string,
  options: ScreenshotOptions = {}
): Promise<ScreenshotResult> {
  const config = {
    format: 'jpeg',
    quality: 0.9,
    includeTimestamp: true,
    ...options,
  };

  return VideoScreenshotPlugin.saveScreenshotToPath(videoRef.videoId, filePath, config);
}

/**
 * Checks if screenshot capture is supported for the current video
 *
 * @param videoRef Reference to the video player instance
 * @returns Promise resolving to boolean indicating support
 *
 * @example
 * ```typescript
 * import { isScreenshotSupported } from 'react-native-video-screenshot-plugin';
 *
 * const supported = await isScreenshotSupported({ videoId: 'my-video' });
 * if (supported) {
 *   // Proceed with screenshot capture
 * }
 * ```
 */
export function isScreenshotSupported(videoRef: VideoRef): Promise<boolean> {
  return VideoScreenshotPlugin.isScreenshotSupported(videoRef.videoId);
}

/**
 * Gets the current video dimensions
 *
 * @param videoRef Reference to the video player instance
 * @returns Promise resolving to video dimensions
 *
 * @example
 * ```typescript
 * import { getVideoDimensions } from 'react-native-video-screenshot-plugin';
 *
 * const { width, height } = await getVideoDimensions({ videoId: 'my-video' });
 * console.log(`Video size: ${width}x${height}`);
 * ```
 */
export function getVideoDimensions(videoRef: VideoRef): Promise<{ width: number; height: number }> {
  return VideoScreenshotPlugin.getVideoDimensions(videoRef.videoId);
}

/**
 * Lists all currently available video players
 *
 * @returns Promise resolving to array of video IDs
 *
 * @example
 * ```typescript
 * import { listAvailableVideos } from 'react-native-video-screenshot-plugin';
 *
 * const videoIds = await listAvailableVideos();
 * console.log('Available videos:', videoIds);
 * ```
 */
export function listAvailableVideos(): Promise<string[]> {
  return VideoScreenshotPlugin.listAvailableVideos();
}

/**
 * Debug method to list all registered players
 */
export function debugListPlayers(): Promise<string[]> {
  return VideoScreenshotPlugin.debugListPlayers();
}

// Export the class-based API as well for advanced usage
export class VideoScreenshotPluginClass {
  static testMethod = testMethod;
  static getModuleInfo = getModuleInfo;
  static registerPlayer = registerPlayer;
  static captureScreenshot = (videoId: string, options?: ScreenshotOptions) =>
    VideoScreenshotPlugin.captureScreenshot(videoId, options);
  static captureScreenshotWhenReady = (videoId: string, options?: ScreenshotOptions) =>
    VideoScreenshotPlugin.captureScreenshotWhenReady(videoId, options);
  static saveScreenshotToLibrary = (videoId: string, options?: ScreenshotOptions) =>
    VideoScreenshotPlugin.saveScreenshotToLibrary(videoId, options);
  static saveScreenshotToPath = (videoId: string, filePath: string, options?: ScreenshotOptions) =>
    VideoScreenshotPlugin.saveScreenshotToPath(videoId, filePath, options);
  static isScreenshotSupported = (videoId: string) =>
    VideoScreenshotPlugin.isScreenshotSupported(videoId);
  static getVideoDimensions = (videoId: string) =>
    VideoScreenshotPlugin.getVideoDimensions(videoId);
  static listAvailableVideos = listAvailableVideos;
  static debugListPlayers = debugListPlayers;
}

export default VideoScreenshotPluginClass;
