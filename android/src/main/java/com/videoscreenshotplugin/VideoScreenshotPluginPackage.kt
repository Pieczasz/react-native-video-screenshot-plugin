package com.videoscreenshotplugin

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * React Native Video Screenshot Plugin Package
 * 
 * This package class is responsible for registering the VideoScreenshotPlugin with React Native
 * and integrating it with the react-native-video library's plugin system.
 * 
 * Integration Flow:
 * 1. React Native calls createNativeModules() during app initialization
 * 2. We create our VideoScreenshotPluginModule instance
 * 3. We register this module with react-native-video's ReactNativeVideoManager
 * 4. The video manager will then call our plugin's lifecycle methods when players are created/destroyed
 * 
 * Plugin Registration Strategy:
 * - Primary: Use direct import and registration (fastest, most reliable)
 * - Fallback: Use reflection for cases where react-native-video might not be available at compile time
 * - This dual approach ensures maximum compatibility across different project configurations
 */
class VideoScreenshotPluginPackage : ReactPackage {

    companion object {
        private const val TAG = "VideoScreenshotPluginPackage"
    }

    /**
     * Creates the native modules that this package provides
     * This is called by React Native during app initialization
     */
    @OptIn(UnstableApi::class)
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        Log.d(TAG, "Creating VideoScreenshotPlugin native module...")
        
        // Create our main plugin module
        val pluginModule = VideoScreenshotPluginModule(reactContext)
        
        // Register the plugin with react-native-video's plugin system
        registerPluginWithVideoManager(pluginModule)
        
        Log.d(TAG, "VideoScreenshotPlugin package creation completed successfully")
        return listOf(pluginModule)
    }

    /**
     * This package doesn't provide any UI components (ViewManagers)
     * The screenshot functionality is purely imperative (method calls)
     */
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }

    /**
     * Registers our plugin with react-native-video's plugin management system
     * Uses multiple registration strategies for maximum compatibility
     * 
     * @param pluginModule The VideoScreenshotPluginModule instance to register
     */
    private fun registerPluginWithVideoManager(pluginModule: VideoScreenshotPluginModule) {
        // Strategy 1: Direct registration (preferred method)
        if (attemptDirectRegistration(pluginModule)) {
            return
        }
        
        // Strategy 2: Reflection-based registration (fallback)
        if (attemptReflectionRegistration(pluginModule)) {
            return
        }
        
        // If both methods fail, log the issue but don't crash the app
        Log.e(TAG, "Failed to register plugin with react-native-video. " +
                   "Screenshot functionality may not work correctly. " +
                   "Please ensure react-native-video is properly installed and configured.")
    }

    /**
     * Attempts to register the plugin using direct method calls
     * This is the preferred approach as it's faster and more reliable
     * 
     * @param pluginModule The plugin module to register
     * @return true if registration succeeded, false otherwise
     */
    private fun attemptDirectRegistration(pluginModule: VideoScreenshotPluginModule): Boolean {
        return try {
            Log.d(TAG, "Attempting direct plugin registration...")
            
            // Get the singleton instance of ReactNativeVideoManager
            val videoManager = com.brentvatne.react.ReactNativeVideoManager.getInstance()
            
            // Register our plugin
            videoManager.registerPlugin(pluginModule)
            
            Log.d(TAG, "✅ Direct plugin registration successful")
            
            logVideoManagerInfo(videoManager)
            
            true
        } catch (e: Exception) {
            Log.w(TAG, "❌ Direct plugin registration failed: ${e.message}")
            Log.d(TAG, "Exception details: ${e.javaClass.simpleName}")
            false
        }
    }

    /**
     * Attempts to register the plugin using reflection
     * This fallback method handles cases where react-native-video classes might not be
     * available at compile time or when there are classpath issues
     * 
     * @param pluginModule The plugin module to register
     * @return true if registration succeeded, false otherwise
     */
    private fun attemptReflectionRegistration(pluginModule: VideoScreenshotPluginModule): Boolean {
        return try {
            Log.d(TAG, "Attempting reflection-based plugin registration...")
            
            // Locate the ReactNativeVideoManager class using reflection
            val videoManagerClass = Class.forName("com.brentvatne.react.ReactNativeVideoManager")
            
            // Get the singleton instance
            val getInstanceMethod = videoManagerClass.getMethod("getInstance")
            val videoManagerInstance = getInstanceMethod.invoke(null)
                ?: throw IllegalStateException("ReactNativeVideoManager.getInstance() returned null")
            
            // Find the plugin registration method
            val pluginInterfaceClass = Class.forName("com.brentvatne.exoplayer.RNVExoplayerPlugin")
            val registerPluginMethod = videoManagerClass.getMethod("registerPlugin", pluginInterfaceClass)
            
            // Register our plugin
            registerPluginMethod.invoke(videoManagerInstance, pluginModule)
            
            Log.d(TAG, "✅ Reflection-based plugin registration successful")
            
            // Optional: Try to get version info via reflection
            tryLogVideoManagerInfoViaReflection(videoManagerClass, videoManagerInstance)
            
            true
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "❌ Reflection registration failed: react-native-video classes not found")
            Log.d(TAG, "This usually means react-native-video is not installed or not properly linked")
            false
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "❌ Reflection registration failed: required methods not found")
            Log.d(TAG, "This might indicate a version mismatch with react-native-video")
            false
        } catch (e: Exception) {
            Log.w(TAG, "❌ Reflection registration failed: ${e.message}")
            Log.d(TAG, "Exception details: ${e.javaClass.simpleName}: ${e.localizedMessage}")
            false
        }
    }

    /**
     * Logs information about the video manager for debugging purposes
     * Only called when direct registration is successful
     * 
     * @param videoManager The ReactNativeVideoManager instance
     */
    private fun logVideoManagerInfo(videoManager: com.brentvatne.react.ReactNativeVideoManager) {
        try {
            // Try to get version information using reflection (safer approach)
            try {
                val getVersionMethod = videoManager.javaClass.getMethod("getVersion")
                val version = getVersionMethod.invoke(videoManager)
                Log.d(TAG, "ReactNativeVideoManager version: $version")
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "❌ Version method not available - this is normal for some react-native-video versions")
            }
            
            // Try to get plugin count (if such method exists)
            try {
                val getPluginsMethod = videoManager.javaClass.getMethod("getRegisteredPlugins")
                val plugins = getPluginsMethod.invoke(videoManager)
                Log.d(TAG, "Registered plugins after our registration: $plugins")
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Plugin count method not available (this is normal)")
            }
            
        } catch (e: Exception) {
            Log.d(TAG, "❌ Could not retrieve video manager info: ${e.message}")
        }
    }

    /**
     * Attempts to log video manager information using reflection
     * Used as a fallback when reflection registration is successful
     * 
     * @param videoManagerClass The Class object for ReactNativeVideoManager
     * @param videoManagerInstance The actual instance
     */
    private fun tryLogVideoManagerInfoViaReflection(
        videoManagerClass: Class<*>, 
        videoManagerInstance: Any
    ) {
        try {
            // Try to get version information via reflection
            val getVersionMethod = videoManagerClass.getMethod("getVersion")
            val version = getVersionMethod.invoke(videoManagerInstance)
            Log.d(TAG, "ReactNativeVideoManager version (via reflection): $version")
            
        } catch (e: Exception) {
            Log.d(TAG, "❌ Could not retrieve video manager info via reflection: ${e.message}")
        }
    }
}