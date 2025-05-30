const path = require('path');

module.exports = {
  presets: ['module:@react-native/babel-preset'],
  plugins: [
    [
      'module-resolver',
      {
        root: ['./src'],
        extensions: ['.ios.js', '.android.js', '.js', '.ts', '.tsx', '.json'],
        alias: {
          'react-native-video-screenshot-plugin': path.resolve(
            __dirname,
            '../../lib',
          ),
        },
      },
    ],
  ],
};
