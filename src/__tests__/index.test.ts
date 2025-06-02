import {
  captureScreenshot,
  captureScreenshotWhenReady,
  saveScreenshotToLibrary,
  saveScreenshotToPath,
  isScreenshotSupported,
  getVideoDimensions,
  listAvailableVideos,
  debugListPlayers,
  testMethod,
  getModuleInfo,
  registerPlayer,
  VideoScreenshotPluginClass,
  type ScreenshotOptions,
  type ScreenshotResult,
  type VideoRef,
  type ModuleInfo,
  type TestResult,
  type PlayerRegistrationResult,
} from '../index.tsx';

// Mock the native module
jest.mock('react-native', () => ({
  NativeModules: {
    VideoScreenshotPlugin: {
      captureScreenshot: jest.fn(),
      captureScreenshotWhenReady: jest.fn(),
      saveScreenshotToLibrary: jest.fn(),
      saveScreenshotToPath: jest.fn(),
      isScreenshotSupported: jest.fn(),
      getVideoDimensions: jest.fn(),
      listAvailableVideos: jest.fn(),
      debugListPlayers: jest.fn(),
      testMethod: jest.fn(),
      getModuleInfo: jest.fn(),
      registerPlayer: jest.fn(),
    },
  },
  Platform: {
    select: jest.fn(obj => obj.ios || obj.default),
  },
}));

describe('VideoScreenshotPlugin', () => {
  const mockVideoRef: VideoRef = { videoId: 'test-video' };
  const mockOptions: ScreenshotOptions = {
    format: 'jpeg',
    quality: 0.8,
    maxWidth: 1920,
    maxHeight: 1080,
    includeTimestamp: true,
  };

  const mockScreenshotResult: ScreenshotResult = {
    base64: 'mock-base64-data',
    uri: 'file://mock-path/screenshot.jpg',
    width: 1920,
    height: 1080,
    timestamp: 10.5,
    size: 123456,
    platform: 'ios',
    message: 'Screenshot captured successfully',
    isTestMode: false,
    playerStatus: 1,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Native Module Linking', () => {
    it('should handle missing native module gracefully', () => {
      // Create a mock proxy similar to what happens when native module is missing
      const mockProxy = new Proxy(
        {},
        {
          get() {
            throw new Error(
              "The package 'react-native-video-screenshot-plugin' doesn't seem to be linked. Make sure: \n\n" +
                "- You have run 'pod install'\n" +
                '- You rebuilt the app after installing the package\n' +
                '- You are not using Expo Go\n'
            );
          },
        }
      );

      // Test that calling any method on the proxy throws the linking error
      expect(() => (mockProxy as any).someMethod()).toThrow(/doesn't seem to be linked/);
      expect(() => (mockProxy as any).captureScreenshot()).toThrow(/doesn't seem to be linked/);
      expect(() => (mockProxy as any).testMethod()).toThrow(/doesn't seem to be linked/);
    });

    it('should use native module when available', () => {
      const { NativeModules } = require('react-native');
      // This test verifies that when NativeModules.VideoScreenshotPlugin exists,
      // we use it instead of the proxy
      expect(NativeModules.VideoScreenshotPlugin).toBeTruthy();
      expect(typeof NativeModules.VideoScreenshotPlugin.captureScreenshot).toBe('function');
    });
  });

  describe('Type Exports', () => {
    it('should export all required functions', () => {
      expect(typeof captureScreenshot).toBe('function');
      expect(typeof captureScreenshotWhenReady).toBe('function');
      expect(typeof saveScreenshotToLibrary).toBe('function');
      expect(typeof saveScreenshotToPath).toBe('function');
      expect(typeof isScreenshotSupported).toBe('function');
      expect(typeof getVideoDimensions).toBe('function');
      expect(typeof listAvailableVideos).toBe('function');
      expect(typeof debugListPlayers).toBe('function');
      expect(typeof testMethod).toBe('function');
      expect(typeof getModuleInfo).toBe('function');
      expect(typeof registerPlayer).toBe('function');
    });

    it('should export VideoScreenshotPluginClass', () => {
      expect(VideoScreenshotPluginClass).toBeDefined();
      expect(typeof VideoScreenshotPluginClass.captureScreenshot).toBe('function');
      expect(typeof VideoScreenshotPluginClass.getModuleInfo).toBe('function');
    });
  });

  describe('captureScreenshot', () => {
    beforeEach(() => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(mockScreenshotResult);
    });

    it('should call native module with correct parameters', async () => {
      const { NativeModules } = require('react-native');

      const result = await captureScreenshot(mockVideoRef, mockOptions);

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        'test-video',
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
          maxHeight: 1080,
          includeTimestamp: true,
        }
      );
      expect(result).toEqual(mockScreenshotResult);
    });

    it('should use default options when none provided', async () => {
      const { NativeModules } = require('react-native');

      await captureScreenshot(mockVideoRef);

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        'test-video',
        {
          format: 'jpeg',
          quality: 0.9,
          includeTimestamp: true,
        }
      );
    });

    it('should handle partial options correctly', async () => {
      const { NativeModules } = require('react-native');

      await captureScreenshot(mockVideoRef, { format: 'png', maxWidth: 800 });

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        'test-video',
        {
          format: 'png',
          quality: 0.9,
          maxWidth: 800,
          includeTimestamp: true,
        }
      );
    });

    it('should handle different image formats', async () => {
      const { NativeModules } = require('react-native');

      // Test PNG format
      await captureScreenshot(mockVideoRef, { format: 'png' });
      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenLastCalledWith(
        'test-video',
        expect.objectContaining({ format: 'png' })
      );

      // Test JPEG format
      await captureScreenshot(mockVideoRef, { format: 'jpeg' });
      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenLastCalledWith(
        'test-video',
        expect.objectContaining({ format: 'jpeg' })
      );
    });

    it('should handle quality boundaries', async () => {
      const { NativeModules } = require('react-native');

      // Test minimum quality
      await captureScreenshot(mockVideoRef, { quality: 0 });
      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenLastCalledWith(
        'test-video',
        expect.objectContaining({ quality: 0 })
      );

      // Test maximum quality
      await captureScreenshot(mockVideoRef, { quality: 1 });
      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenLastCalledWith(
        'test-video',
        expect.objectContaining({ quality: 1 })
      );

      // Test mid-range quality
      await captureScreenshot(mockVideoRef, { quality: 0.5 });
      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenLastCalledWith(
        'test-video',
        expect.objectContaining({ quality: 0.5 })
      );
    });

    it('should handle dimension constraints', async () => {
      const { NativeModules } = require('react-native');

      await captureScreenshot(mockVideoRef, {
        maxWidth: 1280,
        maxHeight: 720,
      });

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        'test-video',
        expect.objectContaining({
          maxWidth: 1280,
          maxHeight: 720,
        })
      );
    });

    it('should handle timestamp option', async () => {
      const { NativeModules } = require('react-native');

      // Test with timestamp enabled
      await captureScreenshot(mockVideoRef, { includeTimestamp: true });
      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenLastCalledWith(
        'test-video',
        expect.objectContaining({ includeTimestamp: true })
      );

      // Test with timestamp disabled
      await captureScreenshot(mockVideoRef, { includeTimestamp: false });
      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenLastCalledWith(
        'test-video',
        expect.objectContaining({ includeTimestamp: false })
      );
    });
  });

  describe('captureScreenshotWhenReady', () => {
    it('should call native module with correct parameters', async () => {
      const { NativeModules } = require('react-native');
      const extendedResult = { ...mockScreenshotResult, waitTime: 2.5 };
      NativeModules.VideoScreenshotPlugin.captureScreenshotWhenReady.mockResolvedValue(
        extendedResult
      );

      const result = await captureScreenshotWhenReady(mockVideoRef, mockOptions);

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshotWhenReady).toHaveBeenCalledWith(
        'test-video',
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
          maxHeight: 1080,
          includeTimestamp: true,
        }
      );
      expect(result).toEqual(extendedResult);
      expect(result.waitTime).toBe(2.5);
    });

    it('should use default options when none provided', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshotWhenReady.mockResolvedValue(
        mockScreenshotResult
      );

      await captureScreenshotWhenReady(mockVideoRef);

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshotWhenReady).toHaveBeenCalledWith(
        'test-video',
        {
          format: 'jpeg',
          quality: 0.9,
          includeTimestamp: true,
        }
      );
    });
  });

  describe('saveScreenshotToLibrary', () => {
    it('should call native module with correct parameters', async () => {
      const { NativeModules } = require('react-native');
      const libraryResult = {
        ...mockScreenshotResult,
        uri: 'photo-library://screenshot_1234567890',
        savedToLibrary: true,
      };
      NativeModules.VideoScreenshotPlugin.saveScreenshotToLibrary.mockResolvedValue(libraryResult);

      const result = await saveScreenshotToLibrary(mockVideoRef, mockOptions);

      expect(NativeModules.VideoScreenshotPlugin.saveScreenshotToLibrary).toHaveBeenCalledWith(
        'test-video',
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
          maxHeight: 1080,
          includeTimestamp: true,
        }
      );
      expect(result).toEqual(libraryResult);
      expect(result.uri).toContain('photo-library://');
    });

    it('should handle permission denied errors', async () => {
      const { NativeModules } = require('react-native');
      const permissionError = new Error('Photo library access permission denied');
      permissionError.name = 'PERMISSION_DENIED';
      NativeModules.VideoScreenshotPlugin.saveScreenshotToLibrary.mockRejectedValue(
        permissionError
      );

      await expect(saveScreenshotToLibrary(mockVideoRef, mockOptions)).rejects.toThrow(
        'Photo library access permission denied'
      );
    });
  });

  describe('saveScreenshotToPath', () => {
    const mockFilePath = '/path/to/screenshots/video_capture.jpg';

    it('should call native module with correct parameters', async () => {
      const { NativeModules } = require('react-native');
      const pathResult = {
        ...mockScreenshotResult,
        uri: `file://${mockFilePath}`,
        size: 98765,
      };
      NativeModules.VideoScreenshotPlugin.saveScreenshotToPath.mockResolvedValue(pathResult);

      const result = await saveScreenshotToPath(mockVideoRef, mockFilePath, mockOptions);

      expect(NativeModules.VideoScreenshotPlugin.saveScreenshotToPath).toHaveBeenCalledWith(
        'test-video',
        mockFilePath,
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
          maxHeight: 1080,
          includeTimestamp: true,
        }
      );
      expect(result).toEqual(pathResult);
      expect(result.uri).toBe(`file://${mockFilePath}`);
      expect(result.size).toBe(98765);
    });

    it('should handle invalid file paths', async () => {
      const { NativeModules } = require('react-native');
      const invalidPathError = new Error('Invalid file path');
      invalidPathError.name = 'INVALID_PATH';
      NativeModules.VideoScreenshotPlugin.saveScreenshotToPath.mockRejectedValue(invalidPathError);

      await expect(saveScreenshotToPath(mockVideoRef, '', mockOptions)).rejects.toThrow(
        'Invalid file path'
      );
    });

    it('should handle filesystem errors', async () => {
      const { NativeModules } = require('react-native');
      const fsError = new Error('Failed to write file to path');
      fsError.name = 'FILESYSTEM_ERROR';
      NativeModules.VideoScreenshotPlugin.saveScreenshotToPath.mockRejectedValue(fsError);

      await expect(
        saveScreenshotToPath(mockVideoRef, '/readonly/path.jpg', mockOptions)
      ).rejects.toThrow('Failed to write file to path');
    });
  });

  describe('isScreenshotSupported', () => {
    it('should return true for supported videos', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.isScreenshotSupported.mockResolvedValue(true);

      const result = await isScreenshotSupported(mockVideoRef);

      expect(NativeModules.VideoScreenshotPlugin.isScreenshotSupported).toHaveBeenCalledWith(
        'test-video'
      );
      expect(result).toBe(true);
    });

    it('should return false for unsupported videos', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.isScreenshotSupported.mockResolvedValue(false);

      const result = await isScreenshotSupported(mockVideoRef);

      expect(result).toBe(false);
    });

    it('should return false when player not found', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.isScreenshotSupported.mockResolvedValue(false);

      const result = await isScreenshotSupported({ videoId: 'non-existent' });

      expect(result).toBe(false);
    });
  });

  describe('getVideoDimensions', () => {
    it('should return video dimensions', async () => {
      const { NativeModules } = require('react-native');
      const mockDimensions = { width: 1920, height: 1080 };
      NativeModules.VideoScreenshotPlugin.getVideoDimensions.mockResolvedValue(mockDimensions);

      const result = await getVideoDimensions(mockVideoRef);

      expect(NativeModules.VideoScreenshotPlugin.getVideoDimensions).toHaveBeenCalledWith(
        'test-video'
      );
      expect(result).toEqual(mockDimensions);
    });

    it('should handle different aspect ratios', async () => {
      const { NativeModules } = require('react-native');

      // Test 4:3 aspect ratio
      const dimensions43 = { width: 1024, height: 768 };
      NativeModules.VideoScreenshotPlugin.getVideoDimensions.mockResolvedValue(dimensions43);

      let result = await getVideoDimensions(mockVideoRef);
      expect(result).toEqual(dimensions43);

      // Test 16:9 aspect ratio
      const dimensions169 = { width: 1920, height: 1080 };
      NativeModules.VideoScreenshotPlugin.getVideoDimensions.mockResolvedValue(dimensions169);

      result = await getVideoDimensions(mockVideoRef);
      expect(result).toEqual(dimensions169);

      // Test vertical video
      const dimensionsVertical = { width: 720, height: 1280 };
      NativeModules.VideoScreenshotPlugin.getVideoDimensions.mockResolvedValue(dimensionsVertical);

      result = await getVideoDimensions(mockVideoRef);
      expect(result).toEqual(dimensionsVertical);
    });

    it('should handle video not loaded error', async () => {
      const { NativeModules } = require('react-native');
      const noVideoError = new Error('No video loaded in player');
      noVideoError.name = 'NO_VIDEO';
      NativeModules.VideoScreenshotPlugin.getVideoDimensions.mockRejectedValue(noVideoError);

      await expect(getVideoDimensions(mockVideoRef)).rejects.toThrow('No video loaded in player');
    });
  });

  describe('listAvailableVideos', () => {
    it('should return list of available video IDs', async () => {
      const { NativeModules } = require('react-native');
      const mockVideoIds = ['video-1', 'video-2', 'video-3'];
      NativeModules.VideoScreenshotPlugin.listAvailableVideos.mockResolvedValue(mockVideoIds);

      const result = await listAvailableVideos();

      expect(NativeModules.VideoScreenshotPlugin.listAvailableVideos).toHaveBeenCalled();
      expect(result).toEqual(mockVideoIds);
    });

    it('should return empty array when no videos available', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.listAvailableVideos.mockResolvedValue([]);

      const result = await listAvailableVideos();

      expect(result).toEqual([]);
    });
  });

  describe('debugListPlayers', () => {
    it('should return debug list of players', async () => {
      const { NativeModules } = require('react-native');
      const mockDebugInfo = ['Player-1 [READY]', 'Player-2 [LOADING]'];
      NativeModules.VideoScreenshotPlugin.debugListPlayers.mockResolvedValue(mockDebugInfo);

      const result = await debugListPlayers();

      expect(NativeModules.VideoScreenshotPlugin.debugListPlayers).toHaveBeenCalled();
      expect(result).toEqual(mockDebugInfo);
    });
  });

  describe('testMethod', () => {
    it('should return test result', async () => {
      const { NativeModules } = require('react-native');
      const mockTestResult: TestResult = {
        status: 'success',
        message: 'Native module is working correctly',
        timestamp: Date.now(),
        platform: 'ios',
      };
      NativeModules.VideoScreenshotPlugin.testMethod.mockResolvedValue(mockTestResult);

      const result = await testMethod();

      expect(NativeModules.VideoScreenshotPlugin.testMethod).toHaveBeenCalled();
      expect(result).toEqual(mockTestResult);
      expect(result.status).toBe('success');
      expect(result.platform).toBe('ios');
    });

    it('should handle test failures', async () => {
      const { NativeModules } = require('react-native');
      const mockFailureResult: TestResult = {
        status: 'failure',
        message: 'Test failed: Module not properly initialized',
        timestamp: Date.now(),
        platform: 'ios',
      };
      NativeModules.VideoScreenshotPlugin.testMethod.mockResolvedValue(mockFailureResult);

      const result = await testMethod();

      expect(result.status).toBe('failure');
      expect(result.message).toContain('Test failed');
    });
  });

  describe('getModuleInfo', () => {
    it('should return module information', async () => {
      const { NativeModules } = require('react-native');
      const mockModuleInfo: ModuleInfo = {
        name: 'VideoScreenshotPlugin',
        version: '1.0.0',
        platform: 'ios',
        playersRegistered: 3,
        availablePlayerIds: ['video-1', 'video-2', 'video-3'],
        timestamp: Date.now(),
      };
      NativeModules.VideoScreenshotPlugin.getModuleInfo.mockResolvedValue(mockModuleInfo);

      const result = await getModuleInfo();

      expect(NativeModules.VideoScreenshotPlugin.getModuleInfo).toHaveBeenCalled();
      expect(result).toEqual(mockModuleInfo);
      expect(result.name).toBe('VideoScreenshotPlugin');
      expect(result.playersRegistered).toBe(3);
      expect(result.availablePlayerIds).toHaveLength(3);
    });
  });

  describe('registerPlayer', () => {
    it('should register a test player', async () => {
      const { NativeModules } = require('react-native');
      const mockRegistrationResult: PlayerRegistrationResult = {
        status: 'success',
        playerId: 'test-player-123',
        message: 'Player registered successfully',
        hasCurrentItem: true,
        testVideoURL: 'https://example.com/test-video.mp4',
        testMode: true,
        platform: 'ios',
        note: 'This is a test player for development',
      };
      NativeModules.VideoScreenshotPlugin.registerPlayer.mockResolvedValue(mockRegistrationResult);

      const result = await registerPlayer('test-player-123');

      expect(NativeModules.VideoScreenshotPlugin.registerPlayer).toHaveBeenCalledWith(
        'test-player-123'
      );
      expect(result).toEqual(mockRegistrationResult);
      expect(result.testMode).toBe(true);
      expect(result.hasCurrentItem).toBe(true);
    });

    it('should handle registration failures', async () => {
      const { NativeModules } = require('react-native');
      const mockFailureResult: PlayerRegistrationResult = {
        status: 'failure',
        playerId: 'invalid-player',
        message: 'Player registration failed: Invalid player ID',
        hasCurrentItem: false,
        testMode: false,
        platform: 'ios',
      };
      NativeModules.VideoScreenshotPlugin.registerPlayer.mockResolvedValue(mockFailureResult);

      const result = await registerPlayer('invalid-player');

      expect(result.status).toBe('failure');
      expect(result.hasCurrentItem).toBe(false);
    });
  });

  describe('VideoScreenshotPluginClass', () => {
    it('should have all static methods', () => {
      expect(typeof VideoScreenshotPluginClass.testMethod).toBe('function');
      expect(typeof VideoScreenshotPluginClass.getModuleInfo).toBe('function');
      expect(typeof VideoScreenshotPluginClass.registerPlayer).toBe('function');
      expect(typeof VideoScreenshotPluginClass.captureScreenshot).toBe('function');
      expect(typeof VideoScreenshotPluginClass.captureScreenshotWhenReady).toBe('function');
      expect(typeof VideoScreenshotPluginClass.saveScreenshotToLibrary).toBe('function');
      expect(typeof VideoScreenshotPluginClass.saveScreenshotToPath).toBe('function');
      expect(typeof VideoScreenshotPluginClass.isScreenshotSupported).toBe('function');
      expect(typeof VideoScreenshotPluginClass.getVideoDimensions).toBe('function');
      expect(typeof VideoScreenshotPluginClass.listAvailableVideos).toBe('function');
      expect(typeof VideoScreenshotPluginClass.debugListPlayers).toBe('function');
    });

    it('should call native methods correctly through class interface', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(mockScreenshotResult);

      const result = await VideoScreenshotPluginClass.captureScreenshot('test-video', mockOptions);

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        'test-video',
        mockOptions
      );
      expect(result).toEqual(mockScreenshotResult);
    });

    it('should call captureScreenshotWhenReady through class interface', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshotWhenReady.mockResolvedValue(
        mockScreenshotResult
      );

      const result = await VideoScreenshotPluginClass.captureScreenshotWhenReady(
        'test-video',
        mockOptions
      );

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshotWhenReady).toHaveBeenCalledWith(
        'test-video',
        mockOptions
      );
      expect(result).toEqual(mockScreenshotResult);
    });

    it('should call saveScreenshotToLibrary through class interface', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.saveScreenshotToLibrary.mockResolvedValue(
        mockScreenshotResult
      );

      const result = await VideoScreenshotPluginClass.saveScreenshotToLibrary(
        'test-video',
        mockOptions
      );

      expect(NativeModules.VideoScreenshotPlugin.saveScreenshotToLibrary).toHaveBeenCalledWith(
        'test-video',
        mockOptions
      );
      expect(result).toEqual(mockScreenshotResult);
    });

    it('should call saveScreenshotToPath through class interface', async () => {
      const { NativeModules } = require('react-native');
      const mockPath = '/test/path.jpg';
      NativeModules.VideoScreenshotPlugin.saveScreenshotToPath.mockResolvedValue(
        mockScreenshotResult
      );

      const result = await VideoScreenshotPluginClass.saveScreenshotToPath(
        'test-video',
        mockPath,
        mockOptions
      );

      expect(NativeModules.VideoScreenshotPlugin.saveScreenshotToPath).toHaveBeenCalledWith(
        'test-video',
        mockPath,
        mockOptions
      );
      expect(result).toEqual(mockScreenshotResult);
    });

    it('should call isScreenshotSupported through class interface', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.isScreenshotSupported.mockResolvedValue(true);

      const result = await VideoScreenshotPluginClass.isScreenshotSupported('test-video');

      expect(NativeModules.VideoScreenshotPlugin.isScreenshotSupported).toHaveBeenCalledWith(
        'test-video'
      );
      expect(result).toBe(true);
    });

    it('should call getVideoDimensions through class interface', async () => {
      const { NativeModules } = require('react-native');
      const mockDimensions = { width: 1920, height: 1080 };
      NativeModules.VideoScreenshotPlugin.getVideoDimensions.mockResolvedValue(mockDimensions);

      const result = await VideoScreenshotPluginClass.getVideoDimensions('test-video');

      expect(NativeModules.VideoScreenshotPlugin.getVideoDimensions).toHaveBeenCalledWith(
        'test-video'
      );
      expect(result).toEqual(mockDimensions);
    });

    it('should call listAvailableVideos through class interface', async () => {
      const { NativeModules } = require('react-native');
      const mockVideoIds = ['video-1', 'video-2'];
      NativeModules.VideoScreenshotPlugin.listAvailableVideos.mockResolvedValue(mockVideoIds);

      const result = await VideoScreenshotPluginClass.listAvailableVideos();

      expect(NativeModules.VideoScreenshotPlugin.listAvailableVideos).toHaveBeenCalled();
      expect(result).toEqual(mockVideoIds);
    });

    it('should call debugListPlayers through class interface', async () => {
      const { NativeModules } = require('react-native');
      const mockDebugInfo = ['Player-1 [READY]'];
      NativeModules.VideoScreenshotPlugin.debugListPlayers.mockResolvedValue(mockDebugInfo);

      const result = await VideoScreenshotPluginClass.debugListPlayers();

      expect(NativeModules.VideoScreenshotPlugin.debugListPlayers).toHaveBeenCalled();
      expect(result).toEqual(mockDebugInfo);
    });
  });

  describe('Error Handling', () => {
    it('should handle player not found errors', async () => {
      const { NativeModules } = require('react-native');
      const playerNotFoundError = new Error("Video player 'invalid-id' not found");
      playerNotFoundError.name = 'PLAYER_NOT_FOUND';
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockRejectedValue(playerNotFoundError);

      await expect(captureScreenshot({ videoId: 'invalid-id' })).rejects.toThrow(
        "Video player 'invalid-id' not found"
      );
    });

    it('should handle screenshot capture failures', async () => {
      const { NativeModules } = require('react-native');
      const captureFailedError = new Error('Screenshot capture failed: Video frame not available');
      captureFailedError.name = 'CAPTURE_FAILED';
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockRejectedValue(captureFailedError);

      await expect(captureScreenshot(mockVideoRef)).rejects.toThrow(
        'Screenshot capture failed: Video frame not available'
      );
    });

    it('should handle invalid player errors', async () => {
      const { NativeModules } = require('react-native');
      const invalidPlayerError = new Error('Invalid video player instance');
      invalidPlayerError.name = 'INVALID_PLAYER';
      NativeModules.VideoScreenshotPlugin.getVideoDimensions.mockRejectedValue(invalidPlayerError);

      await expect(getVideoDimensions(mockVideoRef)).rejects.toThrow(
        'Invalid video player instance'
      );
    });

    it('should handle save operation failures', async () => {
      const { NativeModules } = require('react-native');
      const saveFailedError = new Error('Failed to save screenshot: Insufficient storage space');
      saveFailedError.name = 'SAVE_FAILED';
      NativeModules.VideoScreenshotPlugin.saveScreenshotToPath.mockRejectedValue(saveFailedError);

      await expect(saveScreenshotToPath(mockVideoRef, '/full/disk/path.jpg')).rejects.toThrow(
        'Failed to save screenshot: Insufficient storage space'
      );
    });

    it('should handle network-related errors for remote videos', async () => {
      const { NativeModules } = require('react-native');
      const networkError = new Error('Network error: Failed to load video from remote source');
      networkError.name = 'NETWORK_ERROR';
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockRejectedValue(networkError);

      await expect(captureScreenshot({ videoId: 'remote-video' })).rejects.toThrow(
        'Network error: Failed to load video from remote source'
      );
    });

    it('should propagate unknown errors', async () => {
      const { NativeModules } = require('react-native');
      const unknownError = new Error('Unknown native module error');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockRejectedValue(unknownError);

      await expect(captureScreenshot(mockVideoRef)).rejects.toThrow('Unknown native module error');
    });
  });

  describe('Edge Cases and Boundary Conditions', () => {
    it('should handle empty video ID', async () => {
      const { NativeModules } = require('react-native');
      const emptyIdError = new Error('Video ID cannot be empty');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockRejectedValue(emptyIdError);

      await expect(captureScreenshot({ videoId: '' })).rejects.toThrow('Video ID cannot be empty');
    });

    it('should handle very large dimension constraints', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(mockScreenshotResult);

      await captureScreenshot(mockVideoRef, {
        maxWidth: 99999,
        maxHeight: 99999,
      });

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        'test-video',
        expect.objectContaining({
          maxWidth: 99999,
          maxHeight: 99999,
        })
      );
    });

    it('should handle zero dimensions', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(mockScreenshotResult);

      await captureScreenshot(mockVideoRef, {
        maxWidth: 0,
        maxHeight: 0,
      });

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        'test-video',
        expect.objectContaining({
          maxWidth: 0,
          maxHeight: 0,
        })
      );
    });

    it('should handle special characters in file paths', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.saveScreenshotToPath.mockResolvedValue(
        mockScreenshotResult
      );

      const specialPath = '/path/with spaces/and-dashes/under_scores/file@#$.jpg';
      await saveScreenshotToPath(mockVideoRef, specialPath);

      expect(NativeModules.VideoScreenshotPlugin.saveScreenshotToPath).toHaveBeenCalledWith(
        'test-video',
        specialPath,
        expect.any(Object)
      );
    });

    it('should handle Unicode video IDs', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(mockScreenshotResult);

      const unicodeVideoId = 'video-测试-🎬-αβγ';
      await captureScreenshot({ videoId: unicodeVideoId });

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        unicodeVideoId,
        expect.any(Object)
      );
    });
  });

  describe('Result Validation', () => {
    it('should validate screenshot result structure', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(mockScreenshotResult);

      const result = await captureScreenshot(mockVideoRef);

      // Check required fields
      expect(result).toHaveProperty('base64');
      expect(result).toHaveProperty('width');
      expect(result).toHaveProperty('height');
      expect(typeof result.base64).toBe('string');
      expect(typeof result.width).toBe('number');
      expect(typeof result.height).toBe('number');

      // Check optional fields
      if (result.timestamp !== undefined) {
        expect(typeof result.timestamp).toBe('number');
      }
      if (result.uri !== undefined) {
        expect(typeof result.uri).toBe('string');
      }
      if (result.size !== undefined) {
        expect(typeof result.size).toBe('number');
      }
    });

    it('should handle results with missing optional fields', async () => {
      const { NativeModules } = require('react-native');
      const minimalResult = {
        base64: 'base64-data',
        width: 1920,
        height: 1080,
      };
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(minimalResult);

      const result = await captureScreenshot(mockVideoRef);

      expect(result.base64).toBe('base64-data');
      expect(result.width).toBe(1920);
      expect(result.height).toBe(1080);
      expect(result.timestamp).toBeUndefined();
      expect(result.uri).toBeUndefined();
    });

    it('should validate module info result structure', async () => {
      const { NativeModules } = require('react-native');
      const mockModuleInfo: ModuleInfo = {
        name: 'VideoScreenshotPlugin',
        version: '1.0.0',
        platform: 'ios',
        playersRegistered: 2,
        availablePlayerIds: ['video-1', 'video-2'],
        timestamp: Date.now(),
      };
      NativeModules.VideoScreenshotPlugin.getModuleInfo.mockResolvedValue(mockModuleInfo);

      const result = await getModuleInfo();

      expect(result).toHaveProperty('name');
      expect(result).toHaveProperty('version');
      expect(result).toHaveProperty('platform');
      expect(result).toHaveProperty('playersRegistered');
      expect(result).toHaveProperty('availablePlayerIds');
      expect(result).toHaveProperty('timestamp');
      expect(Array.isArray(result.availablePlayerIds)).toBe(true);
      expect(typeof result.playersRegistered).toBe('number');
    });
  });

  describe('Platform-Specific Behavior', () => {
    it('should handle platform-specific error messages', async () => {
      const { NativeModules, Platform } = require('react-native');

      // Mock Platform.select to return iOS-specific behavior
      Platform.select.mockReturnValue('iOS specific error message');

      const platformError = new Error('iOS specific error message');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockRejectedValue(platformError);

      await expect(captureScreenshot(mockVideoRef)).rejects.toThrow('iOS specific error message');
    });

    it('should handle platform-specific result fields', async () => {
      const { NativeModules } = require('react-native');
      const platformSpecificResult = {
        ...mockScreenshotResult,
        platform: 'ios',
        iOSSpecificField: 'iOS value',
      };
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(
        platformSpecificResult
      );

      const result = await captureScreenshot(mockVideoRef);

      expect(result.platform).toBe('ios');
      expect((result as any).iOSSpecificField).toBe('iOS value');
    });
  });
});
