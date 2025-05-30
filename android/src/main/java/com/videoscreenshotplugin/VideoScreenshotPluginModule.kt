package com.videoscreenshotplugin

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class VideoScreenshotPluginModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), RNVExoplayerPlugin {

    private val players = mutableMapOf<String, ExoPlayer>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    fun captureScreenshot(videoId: String, options: ReadableMap, promise: Promise) {
        coroutineScope.launch {
            try {
                val player = players[videoId]
                if (player == null) {
                    promise.reject("PLAYER_NOT_FOUND", "Video player with id '$videoId' not found")
                    return@launch
                }

                val screenshotResult = captureFrameFromPlayer(player, options, false, null)
                promise.resolve(screenshotResult)
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error capturing screenshot: ${e.message}")
                promise.reject("CAPTURE_FAILED", "Failed to capture screenshot: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    fun saveScreenshotToLibrary(videoId: String, options: ReadableMap, promise: Promise) {
        coroutineScope.launch {
            try {
                val player = players[videoId]
                if (player == null) {
                    promise.reject("PLAYER_NOT_FOUND", "Video player with id '$videoId' not found")
                    return@launch
                }

                val screenshotResult = captureFrameFromPlayer(player, options, true, null)
                promise.resolve(screenshotResult)
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error saving screenshot to library: ${e.message}")
                promise.reject("SAVE_FAILED", "Failed to save screenshot to library: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    fun saveScreenshotToPath(videoId: String, filePath: String, options: ReadableMap, promise: Promise) {
        coroutineScope.launch {
            try {
                val player = players[videoId]
                if (player == null) {
                    promise.reject("PLAYER_NOT_FOUND", "Video player with id '$videoId' not found")
                    return@launch
                }

                val screenshotResult = captureFrameFromPlayer(player, options, false, filePath)
                promise.resolve(screenshotResult)
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error saving screenshot to path: ${e.message}")
                promise.reject("SAVE_FAILED", "Failed to save screenshot to path: ${e.message}", e)
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    @ReactMethod
    fun isScreenshotSupported(videoId: String, promise: Promise) {
        try {
            val player = players[videoId]
            if (player == null) {
                promise.resolve(false)
                return
            }

            val isSupported = player.playbackState == Player.STATE_READY && 
                             player.videoFormat != null
            promise.resolve(isSupported)
        } catch (e: Exception) {
            DebugLog.e(TAG, "Error checking screenshot support: ${e.message}")
            promise.resolve(false)
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    @ReactMethod
    fun getVideoDimensions(videoId: String, promise: Promise) {
        try {
            val player = players[videoId]
            if (player == null) {
                promise.reject("PLAYER_NOT_FOUND", "Video player with id '$videoId' not found")
                return
            }

            val videoSize = player.videoSize
            if (videoSize == VideoSize.UNKNOWN) {
                promise.reject("NO_VIDEO_SIZE", "Video dimensions not available")
                return
            }

            val dimensions = Arguments.createMap().apply {
                putDouble("width", videoSize.width.toDouble())
                putDouble("height", videoSize.height.toDouble())
            }

            promise.resolve(dimensions)
        } catch (e: Exception) {
            DebugLog.e(TAG, "Error getting video dimensions: ${e.message}")
            promise.reject("GET_DIMENSIONS_FAILED", "Failed to get video dimensions: ${e.message}", e)
        }
    }

    override fun onInstanceCreated(id: String, player: ExoPlayer) {
        DebugLog.d(TAG, "VideoScreenshotPlugin: onInstanceCreated for id: $id")
        players[id] = player
    }

    override fun onInstanceRemoved(id: String, player: ExoPlayer) {
        DebugLog.d(TAG, "VideoScreenshotPlugin: onInstanceRemoved for id: $id")
        players.remove(id)
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

        // For ExoPlayer, we need to use a different approach since direct frame extraction is complex
        // We'll use MediaMetadataRetriever as a fallback approach
        val bitmap = extractFrameUsingMetadataRetriever(player)
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
            
            if (includeTimestamp) {
                putDouble("timestamp", player.currentPosition / 1000.0)
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

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    private fun extractFrameUsingMetadataRetriever(player: ExoPlayer): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            // Get the current media item's URI
            val mediaItem = player.currentMediaItem
            val uri = mediaItem?.localConfiguration?.uri
            
            if (uri != null) {
                retriever.setDataSource(reactApplicationContext, uri)
                val timeUs = player.currentPosition * 1000
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                null
            }
        } catch (e: Exception) {
            DebugLog.e(TAG, "Error extracting frame using MediaMetadataRetriever: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                DebugLog.e(TAG, "Error releasing MediaMetadataRetriever: ${e.message}")
            }
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

    companion object {
        const val NAME = "VideoScreenshotPlugin"
        const val TAG = "VideoScreenshotPlugin"
    }
}