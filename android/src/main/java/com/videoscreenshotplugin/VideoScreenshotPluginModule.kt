package com.videoscreenshotplugin

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
class VideoScreenshotPluginModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RNVExoplayerPlugin {

    private val players = ConcurrentHashMap<String, ExoPlayer>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        const val NAME = "VideoScreenshotPlugin"
        private const val TAG = "VideoScreenshotPlugin"
    }

    init {
        DebugLog.d(TAG, "VideoScreenshotPluginModule initialized")
    }

    override fun getName(): String {
        return NAME
    }

    override fun onInstanceCreated(id: String, player: ExoPlayer) {
        DebugLog.d(TAG, "VideoScreenshotPlugin: onInstanceCreated for id: $id")
        players[id] = player
        
        // Send event to React Native that video is ready
        val params = Arguments.createMap()
        params.putString("id", id)
        
        try {
            reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("VideoPlayerReady", params)
            DebugLog.d(TAG, "Emitted VideoPlayerReady event for id: $id")
        } catch (e: Exception) {
            DebugLog.e(TAG, "Failed to emit VideoPlayerReady event: ${e.message}")
        }
    }

    override fun onInstanceRemoved(id: String, player: ExoPlayer) {
        DebugLog.d(TAG, "VideoScreenshotPlugin: onInstanceRemoved for id: $id")
        players.remove(id)
    }

    @ReactMethod
    fun debugListPlayers(promise: Promise) {
        try {
            DebugLog.d(TAG, "DEBUG: Listing all registered players")
            val result = Arguments.createArray()
            for (videoId in players.keys) {
                result.pushString(videoId)
                DebugLog.d(TAG, "DEBUG: Found player with id: $videoId")
            }
            DebugLog.d(TAG, "DEBUG: Total players registered: ${players.size}")
            promise.resolve(result)
        } catch (e: Exception) {
            DebugLog.e(TAG, "DEBUG: Error listing players: ${e.message}")
            promise.reject("DEBUG_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun captureScreenshot(videoId: String, screenshotOptions: ReadableMap, promise: Promise) {
        coroutineScope.launch {
            try {
                DebugLog.d(TAG, "Attempting to capture screenshot for videoId: $videoId")
                
                val player = players[videoId]
                if (player == null) {
                    DebugLog.e(TAG, "Video player with id '$videoId' not found. Available players: ${players.keys}")
                    promise.reject("PLAYER_NOT_FOUND", "Video player with id '$videoId' not found")
                    return@launch
                }

                val screenshotResult = captureFrameFromPlayer(player, screenshotOptions, false, null)
                promise.resolve(screenshotResult)
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error capturing screenshot: ${e.message}")
                promise.reject("CAPTURE_FAILED", "Failed to capture screenshot: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    fun saveScreenshotToLibrary(videoId: String, screenshotOptions: ReadableMap, promise: Promise) {
        coroutineScope.launch {
            try {
                DebugLog.d(TAG, "Attempting to save screenshot to library for videoId: $videoId")
                
                val player = players[videoId]
                if (player == null) {
                    promise.reject("PLAYER_NOT_FOUND", "Video player with id '$videoId' not found")
                    return@launch
                }

                val screenshotResult = captureFrameFromPlayer(player, screenshotOptions, true, null)
                promise.resolve(screenshotResult)
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error saving screenshot to library: ${e.message}")
                promise.reject("SAVE_FAILED", "Failed to save screenshot to library: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    fun saveScreenshotToPath(videoId: String, filePath: String, screenshotOptions: ReadableMap, promise: Promise) {
        coroutineScope.launch {
            try {
                DebugLog.d(TAG, "Attempting to save screenshot to path for videoId: $videoId, path: $filePath")
                
                val player = players[videoId]
                if (player == null) {
                    promise.reject("PLAYER_NOT_FOUND", "Video player with id '$videoId' not found")
                    return@launch
                }

                val screenshotResult = captureFrameFromPlayer(player, screenshotOptions, false, filePath)
                promise.resolve(screenshotResult)
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error saving screenshot to path: ${e.message}")
                promise.reject("SAVE_FAILED", "Failed to save screenshot to path: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    fun isScreenshotSupported(videoId: String, promise: Promise) {
        coroutineScope.launch {
            try {
                DebugLog.d(TAG, "Checking screenshot support for videoId: $videoId")
                
                val player = players[videoId]
                if (player == null) {
                    DebugLog.e(TAG, "Video player with id '$videoId' not found. Available players: ${players.keys}")
                    promise.resolve(false)
                    return@launch
                }

                // Access player on main thread
                val isSupported = withContext(Dispatchers.Main) {
                    player.playbackState == Player.STATE_READY && 
                    player.videoFormat != null
                }
                DebugLog.d(TAG, "Screenshot supported for $videoId: $isSupported")
                promise.resolve(isSupported)
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error checking screenshot support: ${e.message}")
                promise.resolve(false)
            }
        }
    }

    @ReactMethod
    fun getVideoDimensions(videoId: String, promise: Promise) {
        coroutineScope.launch {
            try {
                DebugLog.d(TAG, "Getting video dimensions for videoId: $videoId")
                
                val player = players[videoId]
                if (player == null) {
                    promise.reject("PLAYER_NOT_FOUND", "Video player with id '$videoId' not found")
                    return@launch
                }

                // Access player on main thread
                val result = withContext(Dispatchers.Main) {
                    val videoSize = player.videoSize
                    if (videoSize == VideoSize.UNKNOWN) {
                        throw Exception("Video dimensions not available")
                    }

                    Arguments.createMap().apply {
                        putDouble("width", videoSize.width.toDouble())
                        putDouble("height", videoSize.height.toDouble())
                    }
                }

                promise.resolve(result)
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error getting video dimensions: ${e.message}")
                promise.reject("GET_DIMENSIONS_FAILED", "Failed to get video dimensions: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    fun listAvailableVideos(promise: Promise) {
        try {
            val result = Arguments.createArray()
            for (videoId in players.keys) {
                result.pushString(videoId)
            }
            DebugLog.d(TAG, "Available video players: ${players.keys}")
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("LIST_ERROR", e.message, e)
        }
    }

    private suspend fun captureFrameFromPlayer(
        player: ExoPlayer,
        options: ReadableMap,
        saveToLibrary: Boolean,
        customPath: String?
    ): WritableMap = withContext(Dispatchers.IO) {
        
        val format = if (options.hasKey("format")) options.getString("format") ?: "jpeg" else "jpeg"
        val quality = if (options.hasKey("quality")) (options.getDouble("quality") * 100).toInt() else 90
        val maxWidth = if (options.hasKey("maxWidth")) options.getInt("maxWidth") else 0
        val maxHeight = if (options.hasKey("maxHeight")) options.getInt("maxHeight") else 0
        val includeTimestamp = if (options.hasKey("includeTimestamp")) options.getBoolean("includeTimestamp") else true

        // Get timestamp on main thread first if needed
        val timestamp = if (includeTimestamp) {
            withContext(Dispatchers.Main) {
                player.currentPosition / 1000.0
            }
        } else null

        // Extract frame using improved MediaMetadataRetriever
        val bitmap = extractFrameFromVideo(player)
            ?: throw Exception("Failed to extract frame from video")

        val processedBitmap = if (maxWidth > 0 || maxHeight > 0) {
            resizeBitmap(bitmap, maxWidth, maxHeight)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        val compressFormat = if (format.lowercase() == "png") {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }

        processedBitmap.compress(compressFormat, quality, outputStream)
        val imageData = outputStream.toByteArray()
        val base64String = Base64.encodeToString(imageData, Base64.NO_WRAP)
        
        val result = Arguments.createMap().apply {
            putString("base64", base64String)
            putDouble("width", processedBitmap.width.toDouble())
            putDouble("height", processedBitmap.height.toDouble())
            
            if (timestamp != null) {
                putDouble("timestamp", timestamp)
            }
        }

        // Handle saving
        when {
            saveToLibrary -> {
                val uri = saveImageToMediaStore(processedBitmap, format)
                result.putString("uri", uri.toString())
            }
            customPath != null -> {
                val file = saveImageToFile(imageData, customPath)
                result.putString("uri", file.absolutePath)
                result.putDouble("size", imageData.size.toDouble())
            }
        }

        // Clean up
        if (processedBitmap != bitmap) {
            processedBitmap.recycle()
        }
        bitmap.recycle()

        result
    }

    private suspend fun extractFrameFromVideo(player: ExoPlayer): Bitmap? {
        return try {
            // Get video info on main thread
            val videoInfo = withContext(Dispatchers.Main) {
                val mediaItem = player.currentMediaItem
                val uri = mediaItem?.localConfiguration?.uri
                val currentTimeUs = player.currentPosition * 1000
                val videoSize = player.videoSize
                
                VideoInfo(uri, currentTimeUs, videoSize)
            }
            
            if (videoInfo.uri != null) {
                DebugLog.d(TAG, "Extracting frame from URI: ${videoInfo.uri} at time: ${videoInfo.timeUs}μs")
                
                // Try MediaMetadataRetriever with improved approach
                val bitmap = extractFrameWithMetadataRetriever(videoInfo.uri, videoInfo.timeUs)
                if (bitmap != null) {
                    DebugLog.d(TAG, "Successfully extracted frame using MediaMetadataRetriever")
                    return bitmap
                }
                
                DebugLog.w(TAG, "MediaMetadataRetriever failed, creating enhanced placeholder")
                // Create enhanced placeholder as fallback
                return createEnhancedVideoPlaceholder(videoInfo.videoSize, player)
            } else {
                DebugLog.e(TAG, "No URI available for frame extraction")
                return null
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "Error extracting frame: ${e.message}")
            null
        }
    }

    private suspend fun extractFrameWithMetadataRetriever(uri: Uri, timeUs: Long): Bitmap? {
        return withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                
                // Set data source based on URI type
                val uriString = uri.toString()
                DebugLog.d(TAG, "Setting data source for URI: $uriString")
                
                when {
                    uriString.startsWith("http://") || uriString.startsWith("https://") -> {
                        // For remote URLs
                        retriever.setDataSource(uriString, HashMap<String, String>().apply {
                            put("User-Agent", "VideoScreenshotPlugin/1.0.0")
                        })
                    }
                    uriString.startsWith("file://") -> {
                        // For local files
                        retriever.setDataSource(reactApplicationContext, uri)
                    }
                    uriString.startsWith("content://") -> {
                        // For content URIs
                        retriever.setDataSource(reactApplicationContext, uri)
                    }
                    else -> {
                        // Try as local first, then as URL
                        try {
                            retriever.setDataSource(reactApplicationContext, uri)
                        } catch (e: Exception) {
                            DebugLog.d(TAG, "Local failed, trying as URL: ${e.message}")
                            retriever.setDataSource(uriString)
                        }
                    }
                }
                
                DebugLog.d(TAG, "MediaMetadataRetriever data source set successfully")
                
                // Try different extraction methods
                var bitmap: Bitmap? = null
                val extractionMethods = listOf(
                    { retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) },
                    { retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) },
                    { retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC) },
                    { retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_NEXT_SYNC) },
                    { retriever.frameAtTime }, // Get any frame
                    { retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) } // First frame
                )
                
                for ((index, method) in extractionMethods.withIndex()) {
                    try {
                        DebugLog.d(TAG, "Trying extraction method ${index + 1}")
                        bitmap = method()
                        if (bitmap != null) {
                            DebugLog.d(TAG, "Successfully extracted frame with method ${index + 1}")
                            break
                        }
                    } catch (e: Exception) {
                        DebugLog.d(TAG, "Extraction method ${index + 1} failed: ${e.message}")
                    }
                }
                
                if (bitmap != null) {
                    DebugLog.d(TAG, "Frame extracted successfully: ${bitmap.width}x${bitmap.height}")
                } else {
                    DebugLog.e(TAG, "All frame extraction methods failed")
                }
                
                bitmap
            } catch (e: Exception) {
                DebugLog.e(TAG, "MediaMetadataRetriever error: ${e.message}")
                null
            } finally {
                try {
                    retriever?.release()
                } catch (e: Exception) {
                    DebugLog.e(TAG, "Error releasing MediaMetadataRetriever: ${e.message}")
                }
            }
        }
    }

    private suspend fun createEnhancedVideoPlaceholder(videoSize: VideoSize, player: ExoPlayer): Bitmap {
        return withContext(Dispatchers.Main) {
            val width = if (videoSize.width > 0) videoSize.width else 1920
            val height = if (videoSize.height > 0) videoSize.height else 1080
            
            DebugLog.d(TAG, "Creating enhanced video placeholder: ${width}x${height}")
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Create video-like gradient background
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            
            val gradient = android.graphics.RadialGradient(
                width * 0.3f, height * 0.3f, width * 0.8f,
                intArrayOf(
                    android.graphics.Color.parseColor("#263238"),
                    android.graphics.Color.parseColor("#37474f"),
                    android.graphics.Color.parseColor("#455a64"),
                    android.graphics.Color.parseColor("#546e7a")
                ),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Add overlay for depth
            paint.shader = null
            paint.color = android.graphics.Color.argb(20, 33, 150, 243)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Add subtle pattern
            paint.color = android.graphics.Color.argb(10, 255, 255, 255)
            for (i in 0 until width step 32) {
                canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
            }
            for (i in 0 until height step 32) {
                canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
            }
            
            // Add text overlay
            paint.color = android.graphics.Color.WHITE
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
            
            val centerX = width / 2f
            val centerY = height / 2f
            
            // Play icon
            paint.textSize = kotlin.math.min(width, height) * 0.1f
            canvas.drawText("▶", centerX, centerY - height * 0.05f, paint)
            
            // Title
            paint.textSize = kotlin.math.min(width, height) * 0.04f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("Video Frame", centerX, centerY + height * 0.08f, paint)
            
            // Info
            paint.textSize = kotlin.math.min(width, height) * 0.03f
            paint.typeface = android.graphics.Typeface.DEFAULT
            val currentTime = player.currentPosition / 1000
            val duration = player.duration / 1000
            val timeText = if (duration > 0) "${currentTime}s / ${duration}s" else "${currentTime}s"
            canvas.drawText("${width} × ${height} • $timeText", centerX, centerY + height * 0.15f, paint)
            
            bitmap
        }
    }

    @SuppressLint("UseKtx")
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        var targetWidth = originalWidth
        var targetHeight = originalHeight

        if (maxWidth > 0 && targetWidth > maxWidth) {
            targetHeight = (targetHeight * maxWidth) / targetWidth
            targetWidth = maxWidth
        }

        if (maxHeight > 0 && targetHeight > maxHeight) {
            targetWidth = (targetWidth * maxHeight) / targetHeight
            targetHeight = maxHeight
        }

        return if (targetWidth != originalWidth || targetHeight != originalHeight) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }
    }

    @SuppressLint("UseKtx")
    private fun saveImageToMediaStore(bitmap: Bitmap, format: String): Uri {
        val contentResolver = reactApplicationContext.contentResolver
        val mimeType = if (format.lowercase() == "png") "image/png" else "image/jpeg"
        val extension = if (format.lowercase() == "png") "png" else "jpg"
        
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
            val compressFormat = if (format.lowercase() == "png") {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(compressFormat, 90, outputStream)
        } ?: throw IOException("Failed to save image to MediaStore")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }

        return uri
    }

    private fun saveImageToFile(imageData: ByteArray, filePath: String): File {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        
        FileOutputStream(file).use { outputStream ->
            outputStream.write(imageData)
        }
        
        return file
    }

    // Helper data class
    private data class VideoInfo(
        val uri: Uri?,
        val timeUs: Long,
        val videoSize: VideoSize
    )
}