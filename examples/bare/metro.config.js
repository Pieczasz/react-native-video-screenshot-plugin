const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');
const path = require('path');

// Get the root directory of the plugin
const root = path.resolve(__dirname, '../..');

/**
 * Metro configuration
 * https://facebook.github.io/metro/docs/configuration
 */
const config = {
  projectRoot: __dirname,
  watchFolders: [
    // Watch the plugin source directory
    path.resolve(root, 'src'),
    // Watch the compiled lib directory
    path.resolve(root, 'lib'),
    // Watch the root node_modules
    path.resolve(root, 'node_modules'),
  ],
  resolver: {
    alias: {
      'react-native-video-screenshot-plugin': path.resolve(root, 'lib'),
    },
    // Include the plugin's node_modules
    nodeModulesPaths: [
      path.resolve(__dirname, 'node_modules'),
      path.resolve(root, 'node_modules'),
    ],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
