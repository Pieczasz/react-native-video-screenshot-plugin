module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android/',
        packageImportPath: 'import com.videoscreenshotplugin.VideoScreenshotPluginPackage;',
      },
      // iOS auto-linking will work automatically with the podspec
    },
  },
};
