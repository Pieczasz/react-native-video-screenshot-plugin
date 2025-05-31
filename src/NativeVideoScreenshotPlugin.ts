import { NativeModules } from 'react-native';

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
  format: string;
  size: number;
  timestamp?: number;
  uri?: string;
  message?: string;
  isTestMode?: boolean;
  reason?: string;
  platform: string;
}

export interface VideoDimensions {
  width: number;
  height: number;
}

export interface TestResult {
  status: string;
  message: string;
  timestamp: number;
  platform: string;
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

export interface VideoScreenshotPluginInterface {
  // Test and debug methods
  testMethod(): Promise<TestResult>;
  getModuleInfo(): Promise<ModuleInfo>;
  registerPlayer(playerId: string): Promise<PlayerRegistrationResult>;
  debugListPlayers(): Promise<string[]>;

  // Core screenshot methods
  captureScreenshot(videoId: string, options?: ScreenshotOptions): Promise<ScreenshotResult>;
  captureScreenshotWhenReady(
    videoId: string,
    options?: ScreenshotOptions
  ): Promise<ScreenshotResult>;

  // File system methods
  saveScreenshotToLibrary(videoId: string, options?: ScreenshotOptions): Promise<ScreenshotResult>;
  saveScreenshotToPath(
    videoId: string,
    filePath: string,
    options?: ScreenshotOptions
  ): Promise<ScreenshotResult>;

  // Information methods
  isScreenshotSupported(videoId: string): Promise<boolean>;
  getVideoDimensions(videoId: string): Promise<VideoDimensions>;
  listAvailableVideos(): Promise<string[]>;
}

// Use old bridge pattern - same as Android implementation
export default NativeModules.VideoScreenshotPlugin as VideoScreenshotPluginInterface;
