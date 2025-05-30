import {
  captureScreenshot,
  saveScreenshotToLibrary,
  saveScreenshotToPath,
  isScreenshotSupported,
  getVideoDimensions,
} from '../index';

// Mock the native module
jest.mock('react-native', () => ({
  NativeModules: {
    VideoScreenshotPlugin: {
      captureScreenshot: jest.fn(),
      saveScreenshotToLibrary: jest.fn(),
      saveScreenshotToPath: jest.fn(),
      isScreenshotSupported: jest.fn(),
      getVideoDimensions: jest.fn(),
    },
  },
  Platform: {
    select: jest.fn(),
  },
}));

describe('VideoScreenshotPlugin', () => {
  const mockVideoRef = { videoId: 'test-video' };
  const mockOptions = {
    format: 'jpeg' as const,
    quality: 0.8,
    maxWidth: 1920,
    includeTimestamp: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('API exports', () => {
    it('should export all required functions', () => {
      expect(typeof captureScreenshot).toBe('function');
      expect(typeof saveScreenshotToLibrary).toBe('function');
      expect(typeof saveScreenshotToPath).toBe('function');
      expect(typeof isScreenshotSupported).toBe('function');
      expect(typeof getVideoDimensions).toBe('function');
    });
  });

  describe('captureScreenshot', () => {
    it('should call native module with correct parameters', async () => {
      const mockResult = {
        base64: 'mock-base64',
        width: 1920,
        height: 1080,
        timestamp: 10.5,
      };

      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(mockResult);

      const result = await captureScreenshot(mockVideoRef, mockOptions);

      expect(NativeModules.VideoScreenshotPlugin.captureScreenshot).toHaveBeenCalledWith(
        'test-video',
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
          includeTimestamp: true,
        }
      );
      expect(result).toEqual(mockResult);
    });

    it('should use default options when none provided', async () => {
      const mockResult = {
        base64: 'mock-base64',
        width: 1920,
        height: 1080,
      };

      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockResolvedValue(mockResult);

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
  });

  describe('saveScreenshotToLibrary', () => {
    it('should call native module with correct parameters', async () => {
      const mockResult = {
        base64: 'mock-base64',
        uri: 'file://mock-path',
        width: 1920,
        height: 1080,
      };

      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.saveScreenshotToLibrary.mockResolvedValue(mockResult);

      const result = await saveScreenshotToLibrary(mockVideoRef, mockOptions);

      expect(NativeModules.VideoScreenshotPlugin.saveScreenshotToLibrary).toHaveBeenCalledWith(
        'test-video',
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
          includeTimestamp: true,
        }
      );
      expect(result).toEqual(mockResult);
    });
  });

  describe('saveScreenshotToPath', () => {
    it('should call native module with correct parameters', async () => {
      const mockFilePath = '/path/to/screenshot.jpg';
      const mockResult = {
        base64: 'mock-base64',
        uri: mockFilePath,
        width: 1920,
        height: 1080,
        size: 12345,
      };

      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.saveScreenshotToPath.mockResolvedValue(mockResult);

      const result = await saveScreenshotToPath(mockVideoRef, mockFilePath, mockOptions);

      expect(NativeModules.VideoScreenshotPlugin.saveScreenshotToPath).toHaveBeenCalledWith(
        'test-video',
        mockFilePath,
        {
          format: 'jpeg',
          quality: 0.8,
          maxWidth: 1920,
          includeTimestamp: true,
        }
      );
      expect(result).toEqual(mockResult);
    });
  });

  describe('isScreenshotSupported', () => {
    it('should call native module and return boolean result', async () => {
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.isScreenshotSupported.mockResolvedValue(true);

      const result = await isScreenshotSupported(mockVideoRef);

      expect(NativeModules.VideoScreenshotPlugin.isScreenshotSupported).toHaveBeenCalledWith(
        'test-video'
      );
      expect(result).toBe(true);
    });
  });

  describe('getVideoDimensions', () => {
    it('should call native module and return dimensions', async () => {
      const mockDimensions = { width: 1920, height: 1080 };
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.getVideoDimensions.mockResolvedValue(mockDimensions);

      const result = await getVideoDimensions(mockVideoRef);

      expect(NativeModules.VideoScreenshotPlugin.getVideoDimensions).toHaveBeenCalledWith(
        'test-video'
      );
      expect(result).toEqual(mockDimensions);
    });
  });

  describe('error handling', () => {
    it('should propagate native module errors', async () => {
      const mockError = new Error('Native module error');
      const { NativeModules } = require('react-native');
      NativeModules.VideoScreenshotPlugin.captureScreenshot.mockRejectedValue(mockError);

      await expect(captureScreenshot(mockVideoRef)).rejects.toThrow('Native module error');
    });
  });
});
