package com.videoscreenshotplugin

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

class VideoScreenshotPluginPackage : ReactPackage {
    @OptIn(UnstableApi::class)
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        val module = VideoScreenshotPluginModule(reactContext)
        
        Log.d("VideoScreenshotPlugin", "Creating plugin module...")
        
        // Try to register the plugin with react-native-video's plugin system
        try {
            // Using reflection to access ReactNativeVideoManager since it might not be available at compile time
            val clazz = Class.forName("com.breastbone.react.ReactNativeVideoManager")
            val getInstance = clazz.getMethod("getInstance")
            val instance = getInstance.invoke(null)
            val registerPlugin = clazz.getMethod("registerPlugin", Class.forName("com.breastbone.exoplayer.RNVExoplayerPlugin"))
            registerPlugin.invoke(instance, module)
            Log.d("VideoScreenshotPlugin", "Successfully registered plugin with ReactNativeVideoManager using reflection")
            
            // Let's also try to get information about the video manager
            val getVersion = clazz.getMethod("getVersion")
            val version = getVersion.invoke(instance)
            Log.d("VideoScreenshotPlugin", "ReactNativeVideoManager version: $version")
            
        } catch (e: Exception) {
            Log.e("VideoScreenshotPlugin", "Failed to register plugin using reflection: ${e.message}")
            Log.e("VideoScreenshotPlugin", "Exception details: ${e.javaClass.simpleName}: ${e.localizedMessage}")
            // Try direct approach as fallback
            try {
                val videoManager = com.brentvatne.react.ReactNativeVideoManager.getInstance()
                videoManager.registerPlugin(module)
                Log.d("VideoScreenshotPlugin", "Successfully registered plugin with ReactNativeVideoManager using direct call")
                
                // Try to get registered plugins count
                try {
                    val getPluginsMethod = videoManager.javaClass.getMethod("getRegisteredPlugins")
                    val plugins = getPluginsMethod.invoke(videoManager)
                    Log.d("VideoScreenshotPlugin", "Registered plugins: $plugins")
                } catch (e2: Exception) {
                    Log.d("VideoScreenshotPlugin", "Could not get registered plugins list: ${e2.message}")
                }
                
            } catch (e2: Exception) {
                Log.e("VideoScreenshotPlugin", "Failed to register plugin using direct call: ${e2.message}")
                Log.e("VideoScreenshotPlugin", "Final registration failure. Plugin may not work correctly.")
            }
        }
        
        Log.d("VideoScreenshotPlugin", "Plugin package creation completed")
        return listOf(module)
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}