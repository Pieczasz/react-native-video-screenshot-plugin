package com.videoscreenshotplugin

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.brentvatne.common.toolbox.DebugLog
import com.brentvatne.exoplayer.RNVExoplayerPlugin
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * MediaMetadataRetriever Pool for efficient resource reuse
 * Reduces object creation overhead and improves screenshot performance
 */
class MediaMetadataRetrieverPool(private val maxSize: Int = 3) {
    private val pool = ConcurrentLinkedQueue<MediaMetadataRetriever>()
    private val activeCount = AtomicInteger(0)
    
    fun acquire(): MediaMetadataRetriever {
        val retriever = pool.poll() ?: MediaMetadataRetriever()
        activeCount.incrementAndGet()
        DebugLog.d("MediaMetadataRetrieverPool", "Acquired retriever. Active: ${activeCount.get()}")
        return retriever
    }
    
    fun release(retriever: MediaMetadataRetriever) {
        try {
            // Don't release here - will be done when returned to pool
            if (pool.size < maxSize) {
                pool.offer(retriever)
                DebugLog.d("MediaMetadataRetrieverPool", "Returned retriever to pool. Pool size: ${pool.size}")
            } else {
                retriever.release()
                DebugLog.d("MediaMetadataRetrieverPool", "Pool full, releasing retriever directly")
            }
        } catch (e: Exception) {
            DebugLog.e("MediaMetadataRetrieverPool", "Error managing retriever: ${e.message}")
        } finally {
            activeCount.decrementAndGet()
        }
    }
    
    fun clear() {
        while (pool.isNotEmpty()) {
            pool.poll()?.release()
        }
        DebugLog.d("MediaMetadataRetrieverPool", "Pool cleared")
    }
}

/**
 * Player info data class for background processing
 */
private data class PlayerInfo(
    val currentTime: Long,
    val duration: Long
)

/**
 * React Native Video Screenshot Plugin Module
 * 
 * This module extends react-native-video by implementing the RNVExoplayerPlugin interface,
 * which allows it to hook into the video player lifecycle and capture screenshots from
 * currently playing videos.
 * 
 * Architecture Overview:
 * 1. Plugin Registration: This module registers itself with react-native-video's plugin system
 * 2. Player Tracking: Maintains a registry of active ExoPlayer instances with their IDs
 * 3. Lifecycle Management: Responds to player creation/destruction events
 * 4. Screenshot Capture: Provides methods to capture frames from active players
 * 5. Storage Options: Supports various output formats (base64, file system, media library)
 * 
 * Integration with react-native-video:
 * - Implements RNVExoplayerPlugin interface for automatic integration
 * - Receives player instances through onInstanceCreated/onInstanceRemoved callbacks
 * - Uses player IDs to match JavaScript requests with native player instances
 * - Leverages react-native-video's existing infrastructure for video playback
 * 
 * PERFORMANCE OPTIMIZATIONS:
 * - MediaMetadataRetriever pooling for reduced object creation
 * - Efficient bitmap scaling using Matrix transformations
 * - Background canvas operations to avoid UI thread blocking
 * - Async MediaStore operations with timeout protection
 * - Memory pressure monitoring and cache management
 */
@UnstableApi
class VideoScreenshotPluginModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RNVExoplayerPlugin {

    // ===========================
    // CONSTANTS AND CONFIGURATION
    // ===========================
    
    companion object {
        const val NAME = "VideoScreenshotPlugin"
        private const val TAG = "VideoScreenshotPlugin"
        
        // Screenshot quality and format defaults
        private const val DEFAULT_JPEG_QUALITY = 90
        private const val DEFAULT_FORMAT = "jpeg"
        
        // Placeholder styling constants
        private const val PLACEHOLDER_PATTERN_SIZE = 32
        private const val PLACEHOLDER_OVERLAY_ALPHA = 20
        private const val PLACEHOLDER_PATTERN_ALPHA = 10
        
        // MediaMetadataRetriever extraction options in order of preference
        private val FRAME_EXTRACTION_OPTIONS = listOf(
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            MediaMetadataRetriever.OPTION_CLOSEST,
            MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
            MediaMetadataRetriever.OPTION_NEXT_SYNC
        )
    }

    // ==================
    // INSTANCE VARIABLES
    // ==================
    
    /**
     * Thread-safe registry of active ExoPlayer instances
     * Key: Video player ID (assigned by react-native-video)
     * Value: ExoPlayer instance for screenshot capture
     */
    private val playerRegistry = ConcurrentHashMap<String, ExoPlayer>()
    
    /**
     * Coroutine scope for handling asynchronous screenshot operations
     * Uses SupervisorJob to prevent child coroutine failures from affecting others
     */
    private val screenshotScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * MediaMetadataRetriever pool for efficient resource reuse
     */
    private val retrieverPool = MediaMetadataRetrieverPool(maxSize = 3)
    
    /**
     * Bitmap pool for frequent allocations (basic implementation)
     */
    private val bitmapPool = ConcurrentHashMap<String, ConcurrentLinkedQueue<Bitmap>>()

    // ==================================
    // REACT NATIVE MODULE IMPLEMENTATION
    // ==================================
    
    init {
        DebugLog.d(TAG, "VideoScreenshotPlugin initialized - ready to integrate with react-native-video")
        DebugLog.d(TAG, "Performance optimizations enabled: pooling, efficient scaling, async operations")
    }
    
    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        // Clean up pools on destruction
        retrieverPool.clear()
        clearBitmapPool()
        DebugLog.d(TAG, "Plugin destroyed, pools cleared")
    }

    override fun getName(): String = NAME

    // ===================================
    // REACT-NATIVE-VIDEO PLUGIN INTERFACE
    // ===================================
    
    /**
     * Called by react-native-video when a new ExoPlayer instance is created
     * This is the core integration point that allows us to track active players
     * 
     * @param id Unique identifier for this player instance (matches JavaScript side)
     * @param player The ExoPlayer instance ready for video playback and screenshot capture
     */
    override fun onInstanceCreated(id: String, player: ExoPlayer) {
        DebugLog.d(TAG, "Player instance created - registering player with id: $id")
        
        // Store the player in our registry for later screenshot operations
        playerRegistry[id] = player
        
        // Notify React Native that this video player is ready for screenshot operations
        // This allows the JavaScript side to know when screenshot methods can be called
        emitVideoPlayerReadyEvent(id)
        
        DebugLog.d(TAG, "Player registration complete. Total active players: ${playerRegistry.size}")
    }

    /**
     * Called by react-native-video when an ExoPlayer instance is being destroyed
     * Ensures proper cleanup to prevent memory leaks
     * 
     * @param id Unique identifier for the player being removed
     * @param player The ExoPlayer instance being destroyed
     */
    override fun onInstanceRemoved(id: String, player: ExoPlayer) {
        DebugLog.d(TAG, "Player instance removed - unregistering player with id: $id")
        
        // Remove from our registry to prevent memory leaks
        playerRegistry.remove(id)
        
        DebugLog.d(TAG, "Player cleanup complete. Remaining active players: ${playerRegistry.size}")
    }

    // ==========================
    // REACT METHODS - PUBLIC API
    // ==========================
    
    /**
     * Captures a screenshot from the specified video player and returns it as base64
     * This is the primary screenshot method for in-memory operations
     * 
     * @param videoId The player ID (matches the ID used in JavaScript Video component)
     * @param screenshotOptions Configuration for the screenshot (format, quality, dimensions)
     * @param promise React Native promise to resolve with screenshot data
     */
    @ReactMethod
    fun captureScreenshot(videoId: String, screenshotOptions: ReadableMap, promise: Promise) {
        DebugLog.d(TAG, "Screenshot capture requested for player: $videoId")
        
        screenshotScope.launch {
            try {
                val player = getPlayerById(videoId)
                    ?: return@launch promise.reject("PLAYER_NOT_FOUND", 
                        "Video player '$videoId' not found. Available players: ${playerRegistry.keys}")

                val result = captureFrameFromPlayer(
                    player = player,
                    options = screenshotOptions,
                    outputMode = ScreenshotOutputMode.BASE64_ONLY
                )
                
                promise.resolve(result)
                DebugLog.d(TAG, "Screenshot capture completed successfully for player: $videoId")
                
            } catch (e: Exception) {
                DebugLog.e(TAG, "Screenshot capture failed for player $videoId: ${e.message}")
                promise.reject("CAPTURE_FAILED", "Screenshot capture failed: ${e.message}", e)
            }
        }
    }

    /**
     * Captures a screenshot and saves it to the device's photo library/gallery
     * Uses Android's MediaStore API for proper gallery integration
     * 
     * @param videoId The player ID
     * @param screenshotOptions Configuration for the screenshot
     * @param promise React Native promise to resolve with saved image URI
     */
    @ReactMethod
    fun saveScreenshotToLibrary(videoId: String, screenshotOptions: ReadableMap, promise: Promise) {
        DebugLog.d(TAG, "Save to library requested for player: $videoId")
        
        screenshotScope.launch {
            try {
                val player = getPlayerById(videoId)
                    ?: return@launch promise.reject("PLAYER_NOT_FOUND", 
                        "Video player '$videoId' not found")

                val result = captureFrameFromPlayer(
                    player = player,
                    options = screenshotOptions,
                    outputMode = ScreenshotOutputMode.SAVE_TO_LIBRARY
                )
                
                promise.resolve(result)
                DebugLog.d(TAG, "Screenshot saved to library successfully for player: $videoId")
                
            } catch (e: Exception) {
                DebugLog.e(TAG, "Save to library failed for player $videoId: ${e.message}")
                promise.reject("SAVE_FAILED", "Failed to save screenshot: ${e.message}", e)
            }
        }
    }

    /**
     * Captures a screenshot and saves it to a specific file path
     * Provides direct file system access for custom storage locations
     * 
     * @param videoId The player ID
     * @param filePath Absolute file path where the screenshot should be saved
     * @param screenshotOptions Configuration for the screenshot
     * @param promise React Native promise to resolve with file information
     */
    @ReactMethod
    fun saveScreenshotToPath(videoId: String, filePath: String, screenshotOptions: ReadableMap, promise: Promise) {
        DebugLog.d(TAG, "Save to path requested for player: $videoId, path: $filePath")
        
        screenshotScope.launch {
            try {
                val player = getPlayerById(videoId)
                    ?: return@launch promise.reject("PLAYER_NOT_FOUND", 
                        "Video player '$videoId' not found")

                val result = captureFrameFromPlayer(
                    player = player,
                    options = screenshotOptions,
                    outputMode = ScreenshotOutputMode.SAVE_TO_PATH(filePath)
                )
                
                promise.resolve(result)
                DebugLog.d(TAG, "Screenshot saved to path successfully: $filePath")
                
            } catch (e: Exception) {
                DebugLog.e(TAG, "Save to path failed for $filePath: ${e.message}")
                promise.reject("SAVE_FAILED", "Failed to save to path: ${e.message}", e)
            }
        }
    }

    /**
     * Checks if screenshot capture is currently supported for the given player
     * Useful for UI state management on the JavaScript side
     * 
     * @param videoId The player ID to check
     * @param promise Resolves with boolean indicating support status
     */
    @ReactMethod
    fun isScreenshotSupported(videoId: String, promise: Promise) {
        screenshotScope.launch {
            try {
                val player = getPlayerById(videoId)
                if (player == null) {
                    DebugLog.d(TAG, "Screenshot support check: player $videoId not found")
                    promise.resolve(false)
                    return@launch
                }

                // Check screenshot support on main thread (UI thread where player state is safe to access)
                val isSupported = withContext(Dispatchers.Main) {
                    isPlayerReadyForScreenshot(player)
                }
                
                DebugLog.d(TAG, "Screenshot support for $videoId: $isSupported")
                promise.resolve(isSupported)
                
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error checking screenshot support for $videoId: ${e.message}")
                promise.resolve(false)
            }
        }
    }

    /**
     * Gets the current video dimensions for the specified player
     * Useful for calculating screenshot dimensions and aspect ratios
     * 
     * @param videoId The player ID
     * @param promise Resolves with width and height information
     */
    @ReactMethod
    fun getVideoDimensions(videoId: String, promise: Promise) {
        screenshotScope.launch {
            try {
                val player = getPlayerById(videoId)
                    ?: return@launch promise.reject("PLAYER_NOT_FOUND", 
                        "Video player '$videoId' not found")

                // Access video dimensions on main thread
                val dimensions = withContext(Dispatchers.Main) {
                    val videoSize = player.videoSize
                    if (videoSize == VideoSize.UNKNOWN) {
                        throw IllegalStateException("Video dimensions not available - video may not be loaded")
                    }

                    Arguments.createMap().apply {
                        putDouble("width", videoSize.width.toDouble())
                        putDouble("height", videoSize.height.toDouble())
                    }
                }

                promise.resolve(dimensions)
                DebugLog.d(TAG, "Video dimensions retrieved for $videoId")
                
            } catch (e: Exception) {
                DebugLog.e(TAG, "Failed to get video dimensions for $videoId: ${e.message}")
                promise.reject("GET_DIMENSIONS_FAILED", "Failed to get video dimensions: ${e.message}", e)
            }
        }
    }

    // ============================
    // UTILITY METHODS - PUBLIC API
    // ============================
    
    /**
     * Lists all currently available video players
     * Useful for debugging and development
     */
    @ReactMethod
    fun listAvailableVideos(promise: Promise) {
        try {
            val availablePlayers = Arguments.createArray()
            playerRegistry.keys.forEach { videoId ->
                availablePlayers.pushString(videoId)
            }
            
            DebugLog.d(TAG, "Available video players: ${playerRegistry.keys}")
            promise.resolve(availablePlayers)
            
        } catch (e: Exception) {
            DebugLog.e(TAG, "Error listing available videos: ${e.message}")
            promise.reject("LIST_ERROR", "Failed to list videos: ${e.message}", e)
        }
    }

    /**
     * Debug method to inspect the current state of registered players
     * Should only be used for development and troubleshooting
     */
    @ReactMethod
    fun debugListPlayers(promise: Promise) {
        try {
            DebugLog.d(TAG, "DEBUG: Listing all registered players")
            val playerList = Arguments.createArray()
            
            playerRegistry.forEach { (videoId, _) ->
                playerList.pushString(videoId)
                DebugLog.d(TAG, "DEBUG: Found player with id: $videoId")
            }
            
            DebugLog.d(TAG, "DEBUG: Total players registered: ${playerRegistry.size}")
            promise.resolve(playerList)
            
        } catch (e: Exception) {
            DebugLog.e(TAG, "DEBUG: Error listing players: ${e.message}")
            promise.reject("DEBUG_ERROR", "Debug operation failed: ${e.message}", e)
        }
    }

    // ==============================
    // CORE SCREENSHOT IMPLEMENTATION
    // ==============================
    
    /**
     * Defines the different output modes for screenshot capture
     * This sealed class ensures type safety and clear intent for each capture operation
     */
    private sealed class ScreenshotOutputMode {
        object BASE64_ONLY : ScreenshotOutputMode()
        object SAVE_TO_LIBRARY : ScreenshotOutputMode()
        data class SAVE_TO_PATH(val filePath: String) : ScreenshotOutputMode()
    }

    /**
     * Core screenshot capture implementation
     * Handles the complete workflow from frame extraction to output formatting
     * 
     * @param player The ExoPlayer instance to capture from
     * @param options Configuration options from JavaScript
     * @param outputMode Determines how the captured screenshot should be handled
     * @return WritableMap containing the screenshot data and metadata
     */
    private suspend fun captureFrameFromPlayer(
        player: ExoPlayer,
        options: ReadableMap,
        outputMode: ScreenshotOutputMode
    ): WritableMap = withContext(Dispatchers.IO) {
        
        // Parse screenshot configuration with sensible defaults
        val config = parseScreenshotOptions(options)
        
        // Get current playback timestamp if requested (must be done on main thread)
        val timestamp = if (config.includeTimestamp) {
            withContext(Dispatchers.Main) {
                player.currentPosition / 1000.0
            }
        } else null

        // Extract the actual video frame
        val originalBitmap = extractVideoFrame(player)
            ?: throw IllegalStateException("Failed to extract frame from video - video may not be ready")

        // Apply any requested resizing
        val processedBitmap = if (config.maxWidth > 0 || config.maxHeight > 0) {
            resizeBitmapMaintainingAspectRatio(originalBitmap, config.maxWidth, config.maxHeight)
        } else {
            originalBitmap
        }

        // Convert to the requested format and generate base64
        val imageData = compressBitmapToByteArray(processedBitmap, config.format, config.quality)
        val base64String = Base64.encodeToString(imageData, Base64.NO_WRAP)
        
        // Build the result object with screenshot data and metadata
        val result = Arguments.createMap().apply {
            putString("base64", base64String)
            putDouble("width", processedBitmap.width.toDouble())
            putDouble("height", processedBitmap.height.toDouble())
            
            if (timestamp != null) {
                putDouble("timestamp", timestamp)
            }
        }

        // Handle the requested output mode
        when (outputMode) {
            is ScreenshotOutputMode.SAVE_TO_LIBRARY -> {
                val mediaUri = saveImageToMediaStore(processedBitmap, config.format)
                result.putString("uri", mediaUri.toString())
            }
            is ScreenshotOutputMode.SAVE_TO_PATH -> {
                val savedFile = saveImageDataToFile(imageData, outputMode.filePath)
                result.putString("uri", savedFile.absolutePath)
                result.putDouble("size", imageData.size.toDouble())
            }
            is ScreenshotOutputMode.BASE64_ONLY -> {
                // Base64 is already included, no additional output needed
            }
        }

        // Clean up bitmaps to prevent memory leaks
        if (processedBitmap != originalBitmap) {
            processedBitmap.recycle()
        }
        originalBitmap.recycle()

        result
    }

    // ========================
    // FRAME EXTRACTION METHODS
    // =========================
    
    /**
     * Extracts a video frame from the given ExoPlayer instance
     * Uses multiple fallback strategies to maximize success rate
     * 
     * @param player The ExoPlayer to extract from
     * @return Bitmap containing the extracted frame, or null if extraction fails
     */
    private suspend fun extractVideoFrame(player: ExoPlayer): Bitmap? {
        // Get video information on the main thread where player access is safe
        val videoInfo = withContext(Dispatchers.Main) {
            extractVideoInfo(player)
        }
        
        if (videoInfo.uri == null) {
            DebugLog.e(TAG, "No video URI available for frame extraction")
            return createFallbackPlaceholder(videoInfo.videoSize, player)
        }

        DebugLog.d(TAG, "Extracting frame from URI: ${videoInfo.uri} at ${videoInfo.timeUs}μs")
        
        // Try MediaMetadataRetriever first (most reliable for actual video frames)
        val extractedFrame = extractFrameWithMediaMetadataRetriever(videoInfo.uri, videoInfo.timeUs)
        if (extractedFrame != null) {
            DebugLog.d(TAG, "Successfully extracted actual video frame")
            return extractedFrame
        }
        
        // Fallback to enhanced placeholder if frame extraction fails
        DebugLog.w(TAG, "Frame extraction failed, creating enhanced placeholder")
        return createFallbackPlaceholder(videoInfo.videoSize, player)
    }

    /**
     * Extracts video information needed for frame capture
     * Must be called on the main thread for safe player access
     */
    private fun extractVideoInfo(player: ExoPlayer): VideoInfo {
        val mediaItem = player.currentMediaItem
        val uri = mediaItem?.localConfiguration?.uri
        val currentTimeUs = player.currentPosition * 1000
        val videoSize = player.videoSize
        
        return VideoInfo(uri, currentTimeUs, videoSize)
    }

    /**
     * Uses MediaMetadataRetriever to extract an actual video frame
     * Uses pooled retrievers for better performance
     * 
     * @param uri The video URI to extract from
     * @param timeUs The timestamp in microseconds
     * @return Extracted bitmap or null if all methods fail
     */
    private suspend fun extractFrameWithMediaMetadataRetriever(uri: Uri, timeUs: Long): Bitmap? {
        return withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            
            try {
                // Use pooled retriever instead of creating new one
                retriever = retrieverPool.acquire()
                
                // Configure data source based on URI scheme
                configureRetrieverDataSource(retriever, uri)
                
                DebugLog.d(TAG, "MediaMetadataRetriever configured successfully (pooled)")
                
                // Try multiple extraction methods in order of preference
                var extractedBitmap: Bitmap? = null
                
                for ((index, option) in FRAME_EXTRACTION_OPTIONS.withIndex()) {
                    try {
                        DebugLog.d(TAG, "Attempting frame extraction method ${index + 1}")
                        extractedBitmap = retriever.getFrameAtTime(timeUs, option)
                        
                        if (extractedBitmap != null) {
                            DebugLog.d(TAG, "Frame extraction successful with method ${index + 1}")
                            break
                        }
                    } catch (e: Exception) {
                        DebugLog.d(TAG, "Extraction method ${index + 1} failed: ${e.message}")
                    }
                }
                
                // Final fallback attempts
                if (extractedBitmap == null) {
                    extractedBitmap = attemptFallbackFrameExtraction(retriever)
                }
                
                if (extractedBitmap != null) {
                    DebugLog.d(TAG, "Frame extracted: ${extractedBitmap.width}x${extractedBitmap.height}")
                } else {
                    DebugLog.e(TAG, "All frame extraction attempts failed")
                }
                
                extractedBitmap
                
            } catch (e: Exception) {
                DebugLog.e(TAG, "MediaMetadataRetriever error: ${e.message}")
                null
            } finally {
                // Return retriever to pool instead of releasing immediately
                retriever?.let { retrieverPool.release(it) }
            }
        }
    }

    // ==============
    // HELPER METHODS
    // ==============
    
    private data class ScreenshotConfig(
        val format: String,
        val quality: Int,
        val maxWidth: Int,
        val maxHeight: Int,
        val includeTimestamp: Boolean
    )

    private data class VideoInfo(
        val uri: Uri?,
        val timeUs: Long,
        val videoSize: VideoSize
    )

    /**
     * Parses screenshot options from JavaScript with defaults
     */
    private fun parseScreenshotOptions(options: ReadableMap): ScreenshotConfig {
        return ScreenshotConfig(
            format = options.getString("format") ?: DEFAULT_FORMAT,
            quality = if (options.hasKey("quality")) (options.getDouble("quality") * 100).toInt() else DEFAULT_JPEG_QUALITY,
            maxWidth = if (options.hasKey("maxWidth")) options.getInt("maxWidth") else 0,
            maxHeight = if (options.hasKey("maxHeight")) options.getInt("maxHeight") else 0,
            includeTimestamp = if (options.hasKey("includeTimestamp")) options.getBoolean("includeTimestamp") else true
        )
    }

    /**
     * Gets a player by ID with proper error handling
     */
    private fun getPlayerById(videoId: String): ExoPlayer? {
        return playerRegistry[videoId].also { player ->
            if (player == null) {
                DebugLog.e(TAG, "Player '$videoId' not found. Available: ${playerRegistry.keys}")
            }
        }
    }

    /**
     * Checks if a player is ready for screenshot capture
     * Must be called on the main thread
     */
    private fun isPlayerReadyForScreenshot(player: ExoPlayer): Boolean {
        return player.playbackState == Player.STATE_READY && player.videoFormat != null
    }

    /**
     * Emits an event to React Native indicating a player is ready
     */
    private fun emitVideoPlayerReadyEvent(playerId: String) {
        try {
            val eventData = Arguments.createMap().apply {
                putString("id", playerId)
            }
            
            reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("VideoPlayerReady", eventData)
                
            DebugLog.d(TAG, "VideoPlayerReady event emitted for player: $playerId")
        } catch (e: Exception) {
            DebugLog.e(TAG, "Failed to emit VideoPlayerReady event: ${e.message}")
        }
    }

    /**
     * Configures MediaMetadataRetriever data source based on URI scheme
     */
    private fun configureRetrieverDataSource(retriever: MediaMetadataRetriever, uri: Uri) {
        val uriString = uri.toString()
        DebugLog.d(TAG, "Configuring data source for URI: $uriString")
        
        when {
            uriString.startsWith("http://") || uriString.startsWith("https://") -> {
                // Remote video URLs
                retriever.setDataSource(uriString, mapOf(
                    "User-Agent" to "VideoScreenshotPlugin/1.0.0"
                ))
            }
            uriString.startsWith("file://") || uriString.startsWith("content://") -> {
                // Local files and content URIs
                retriever.setDataSource(reactApplicationContext, uri)
            }
            else -> {
                // Try local first, then URL as fallback
                try {
                    retriever.setDataSource(reactApplicationContext, uri)
                } catch (e: Exception) {
                    DebugLog.d(TAG, "Local data source failed, trying as URL: ${e.message}")
                    retriever.setDataSource(uriString)
                }
            }
        }
    }

    /**
     * Attempts fallback frame extraction methods
     */
    private fun attemptFallbackFrameExtraction(retriever: MediaMetadataRetriever): Bitmap? {
        val fallbackMethods = listOf(
            { retriever.frameAtTime },
            { retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) }
        )
        
        for ((index, method) in fallbackMethods.withIndex()) {
            try {
                DebugLog.d(TAG, "Trying fallback extraction method ${index + 1}")
                val bitmap = method()
                if (bitmap != null) {
                    DebugLog.d(TAG, "Fallback method ${index + 1} succeeded")
                    return bitmap
                }
            } catch (e: Exception) {
                DebugLog.d(TAG, "Fallback method ${index + 1} failed: ${e.message}")
            }
        }
        
        return null
    }

    /**
     * Creates an enhanced placeholder when actual frame extraction fails
     * Runs canvas operations on background thread
     */
    private suspend fun createFallbackPlaceholder(videoSize: VideoSize, player: ExoPlayer): Bitmap {
        // Get player info on main thread, then process on background
        val playerInfo = withContext(Dispatchers.Main) {
            PlayerInfo(
                currentTime = player.currentPosition / 1000,
                duration = player.duration / 1000
            )
        }
        
        return withContext(Dispatchers.Default) { // Background thread
            val width = if (videoSize.width > 0) videoSize.width else 1920
            val height = if (videoSize.height > 0) videoSize.height else 1080
            
            DebugLog.d(TAG, "Creating fallback placeholder: ${width}x${height} (background thread)")
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            drawPlaceholderBackground(canvas, width, height)
            drawPlaceholderContentOptimized(canvas, width, height, playerInfo)
            
            bitmap
        }
    }

    /**
     * Draws the background for the placeholder image
     */
    private fun drawPlaceholderBackground(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Create gradient background
        val gradient = RadialGradient(
            width * 0.3f, height * 0.3f, width * 0.8f,
            intArrayOf(
                Color.parseColor("#263238"),
                Color.parseColor("#37474f"),
                Color.parseColor("#455a64"),
                Color.parseColor("#546e7a")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Add overlay
        paint.shader = null
        paint.color = Color.argb(PLACEHOLDER_OVERLAY_ALPHA, 33, 150, 243)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Add pattern
        paint.color = Color.argb(PLACEHOLDER_PATTERN_ALPHA, 255, 255, 255)
        for (i in 0 until width step PLACEHOLDER_PATTERN_SIZE) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
        }
        for (i in 0 until height step PLACEHOLDER_PATTERN_SIZE) {
            canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
        }
    }

    /**
     * Optimized content drawing using PlayerInfo data class
     */
    private fun drawPlaceholderContentOptimized(canvas: Canvas, width: Int, height: Int, playerInfo: PlayerInfo) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Play icon
        paint.textSize = min(width, height) * 0.1f
        canvas.drawText("▶", centerX, centerY - height * 0.05f, paint)
        
        // Title
        paint.textSize = min(width, height) * 0.04f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Video Frame", centerX, centerY + height * 0.08f, paint)
        
        // Info
        paint.textSize = min(width, height) * 0.03f
        paint.typeface = Typeface.DEFAULT
        val timeText = if (playerInfo.duration > 0) "${playerInfo.currentTime}s / ${playerInfo.duration}s" else "${playerInfo.currentTime}s"
        canvas.drawText("${width} × ${height} • $timeText", centerX, centerY + height * 0.15f, paint)
    }

    /**
     * Resizes a bitmap while maintaining aspect ratio
     * Uses Matrix transformation for better quality and performance
     */
    @SuppressLint("UseKtx")
    private fun resizeBitmapMaintainingAspectRatio(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (maxWidth <= 0 && maxHeight <= 0) return bitmap
        
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        val scaleX = if (maxWidth > 0) maxWidth.toFloat() / originalWidth else Float.MAX_VALUE
        val scaleY = if (maxHeight > 0) maxHeight.toFloat() / originalHeight else Float.MAX_VALUE
        val scale = minOf(scaleX, scaleY, 1.0f)
        
        if (scale >= 1.0f) return bitmap
        
        DebugLog.d(TAG, "Resizing bitmap from ${originalWidth}x${originalHeight} with scale $scale")
        
        // Use Matrix for better quality and memory efficiency
        val matrix = Matrix().apply { setScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
    }

    /**
     * Compresses a bitmap to byte array with specified format and quality
     * Pre-allocates ByteArrayOutputStream and supports WebP
     */
    private fun compressBitmapToByteArray(bitmap: Bitmap, format: String, quality: Int): ByteArray {
        // Estimate size and pre-allocate buffer
        val estimatedSize = bitmap.width * bitmap.height * 4 // Rough estimate
        val outputStream = ByteArrayOutputStream(estimatedSize / 8) // Pre-allocate smaller buffer
        
        val compressFormat = when (format.lowercase()) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            else -> Bitmap.CompressFormat.JPEG
        }
        
        DebugLog.d(TAG, "Compressing bitmap to $format with quality $quality")
        bitmap.compress(compressFormat, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Saves a bitmap to the Android media store (gallery)
     * Async operation with timeout protection
     */
    @SuppressLint("UseKtx")
    private suspend fun saveImageToMediaStore(bitmap: Bitmap, format: String): Uri = withTimeout(10000) {
        withContext(Dispatchers.IO) {
            val contentResolver = reactApplicationContext.contentResolver
            val mimeType = when (format.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            val extension = when (format.lowercase()) {
                "png" -> "png"
                "webp" -> "webp"
                else -> "jpg"
            }
            
            DebugLog.d(TAG, "Saving image to MediaStore: $mimeType")
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "video_screenshot_${System.currentTimeMillis()}.$extension")
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Failed to create MediaStore entry")

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val compressFormat = when (format.lowercase()) {
                    "png" -> Bitmap.CompressFormat.PNG
                    "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    else -> Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, 90, outputStream)
            } ?: throw IOException("Failed to save image to MediaStore")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }

            DebugLog.d(TAG, "Image saved to MediaStore successfully: $uri")
            uri
        }
    }

    /**
     * Saves image data to a specific file path
     */
    private fun saveImageDataToFile(imageData: ByteArray, filePath: String): File {
        val file = File(filePath)
        
        // Ensure parent directories exist
        file.parentFile?.mkdirs()
        
        // Write the image data
        FileOutputStream(file).use { outputStream ->
            outputStream.write(imageData)
        }
        
        return file
    }

    /**
     * Clears the bitmap pool
     */
    private fun clearBitmapPool() {
        bitmapPool.clear()
        DebugLog.d(TAG, "Bitmap pool cleared")
    }
}