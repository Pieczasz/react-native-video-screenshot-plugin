/* eslint-env jest */
// Mock react-native modules
jest.mock('react-native', () => {
  const RN = jest.requireActual('react-native');

  return {
    ...RN,
    NativeModules: {
      ...RN.NativeModules,
      VideoScreenshotPlugin: {
        captureScreenshot: jest.fn(),
        saveScreenshotToLibrary: jest.fn(),
        saveScreenshotToPath: jest.fn(),
        isScreenshotSupported: jest.fn(),
        getVideoDimensions: jest.fn(),
      },
    },
    Platform: {
      ...RN.Platform,
      OS: 'ios', // or 'android' depending on test
      select: jest.fn(platforms => platforms.ios || platforms.default),
    },
  };
});

// Mock react-native-video
jest.mock('react-native-video', () => {
  const React = require('react');
  return {
    __esModule: true,
    default: React.forwardRef((props, ref) => {
      return React.createElement('VideoMock', { ...props, ref });
    }),
  };
});

// Global test setup
global.__DEV__ = true;

// Silence the warning about deprecated lifecycle methods
const originalConsoleWarn = console.warn;
console.warn = (...args) => {
  if (
    typeof args[0] === 'string' &&
    args[0].includes('Warning: componentWillReceiveProps has been renamed')
  ) {
    return;
  }
  originalConsoleWarn.call(console, ...args);
};

// Set up fake timers
beforeEach(() => {
  jest.useFakeTimers();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
  jest.clearAllMocks();
});
