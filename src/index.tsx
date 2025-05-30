import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  "The package 'react-native-video-screenshot-plugin' doesn't seem to be linked. Make sure: \n\n" +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

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
  /**
   * Base64 encoded image data
   */
  base64: string;

  /**
   * File URI if saved to device
   */
  uri?: string;

  /**
   * Image width in pixels
   */
  width: number;

  /**
   * Image height in pixels
   */
  height: number;

  /**
   * Video timestamp when screenshot was taken (in seconds)
   */
  timestamp?: number;

  /**
   * File size in bytes (if saved)
   */
  size?: number;
}

export interface VideoRef {
  /**
   * Reference ID for the video player instance
   */
  videoId: string;
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
 * Lists all available video player instances
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
 * Debug method to list all registered players with detailed info
 *
 * @returns Promise resolving to array of player IDs
 */
export function debugListPlayers(): Promise<string[]> {
  return VideoScreenshotPlugin.debugListPlayers();
}

// Types are already exported above, no need to re-export

// Default export
export default {
  captureScreenshot,
  saveScreenshotToLibrary,
  saveScreenshotToPath,
  isScreenshotSupported,
  getVideoDimensions,
  listAvailableVideos,
  debugListPlayers,
};
