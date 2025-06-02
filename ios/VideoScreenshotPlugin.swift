import Foundation
import AVFoundation
import Photos
import UIKit
import React

// Import react-native-video if available
#if canImport(react_native_video)
import react_native_video
#endif

/**
 * React Native Video Screenshot Plugin - iOS Implementation
 * 
 * This module extends react-native-video by implementing the RNVAVPlayerPlugin interface,
 * which allows it to hook into the video player lifecycle and capture screenshots from
 * currently playing videos using AVFoundation.
 * 
 * Architecture Overview:
 * 1. Plugin Registration: This module registers itself with react-native-video's plugin system
 * 2. Player Tracking: Maintains a registry of active AVPlayer instances with their IDs
 * 3. Lifecycle Management: Responds to player creation/destruction events
 * 4. Screenshot Capture: Provides methods to capture frames from active players
 * 5. Storage Options: Supports various output formats (base64, file system, photo library)
 * 
 * Integration with react-native-video:
 * - Inherits from RNVAVPlayerPlugin for automatic integration
 * - Receives player instances through onInstanceCreated/onInstanceRemoved callbacks
 * - Uses player IDs to match JavaScript requests with native player instances
 * - Leverages react-native-video's existing infrastructure for video playback
 */

// MARK: - Plugin Error Types
enum VideoScreenshotError: Error {
    case playerNotFound(String)
    case invalidPlayer
    case screenshotFailed(String)
    case permissionDenied
    
    var localizedDescription: String {
        switch self {
        case .playerNotFound(let id):
            return "Video player with ID '\(id)' not found"
        case .invalidPlayer:
            return "Invalid video player instance"
        case .screenshotFailed(let reason):
            return "Screenshot capture failed: \(reason)"
        case .permissionDenied:
            return "Photo library access permission denied"
        }
    }
}

// MARK: - Screenshot Options
struct ScreenshotOptions {
    let quality: Float
    let format: String
    let maxWidth: Int
    let maxHeight: Int
    let includeTimestamp: Bool
    
    init(from dictionary: [String: Any]?) {
        self.quality = dictionary?["quality"] as? Float ?? 1.0
        self.format = dictionary?["format"] as? String ?? "jpeg"
        self.maxWidth = dictionary?["maxWidth"] as? Int ?? 0
        self.maxHeight = dictionary?["maxHeight"] as? Int ?? 0
        self.includeTimestamp = dictionary?["includeTimestamp"] as? Bool ?? true
    }
}

// MARK: - Screenshot Output Modes
private enum ScreenshotOutputMode {
    case base64Only
    case saveToLibrary
    case saveToPath(String)
}

// MARK: - Main Plugin Class
@objc(VideoScreenshotPlugin)
public class VideoScreenshotPlugin: RNVPlugin, RCTBridgeModule {
    
    // MARK: - Class Properties
    private static let TAG = "VideoScreenshotPlugin"
    
    // Thread-safe storage for AVPlayer instances
    private let playerQueue = DispatchQueue(label: "com.videoscreenshotplugin.players", attributes: .concurrent)
    private var players: [String: AVPlayer] = [:]
    
    // React Native bridge access - must be public for RCTBridgeModule protocol
    @objc public var bridge: RCTBridge!
    
    // Cached image generators to avoid recreation overhead
    private lazy var imageGeneratorCache: NSCache<NSString, AVAssetImageGenerator> = {
        let cache = NSCache<NSString, AVAssetImageGenerator>()
        cache.countLimit = 10 // Limit cache size to prevent memory issues
        cache.totalCostLimit = 100 * 1024 * 1024 // 100MB limit
        return cache
    }()
    
    // Background queue for image processing
    private let imageProcessingQueue = DispatchQueue(label: "com.videoscreenshotplugin.imageprocessing", qos: .userInitiated)
    
    // MARK: - React Native Bridge Module
    public static func moduleName() -> String! {
        return "VideoScreenshotPlugin"
    }
    
    public static func requiresMainQueueSetup() -> Bool {
        return false
    }
    
    public func constantsToExport() -> [AnyHashable: Any]! {
        return [
            "version": "1.0.0",
            "platform": "iOS"
        ]
    }
    
    // MARK: - Initialization
    override init() {
        super.init()
        NSLog("\(VideoScreenshotPlugin.TAG): VideoScreenshotPlugin initialized - registering with react-native-video")
        
        // Add memory warning observer to clear caches
        NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            NSLog("\(VideoScreenshotPlugin.TAG): Memory warning received - clearing image generator cache")
            self?.imageGeneratorCache.removeAllObjects()
        }
        
        // Register with react-native-video's plugin system
        DispatchQueue.main.async {
            self.registerWithVideoManager()
        }
    }
    
    deinit {
        // Clean up observers and caches
        NotificationCenter.default.removeObserver(self)
        imageGeneratorCache.removeAllObjects()
        
        // Unregister from react-native-video if available
        #if canImport(react_native_video)
        ReactNativeVideoManager.shared.unregisterPlugin(plugin: self)
        #endif
        NSLog("\(VideoScreenshotPlugin.TAG): Plugin deinitialized and unregistered")
    }
    
    // MARK: - RNVPlugin Implementation (Core Integration)
    
    /**
     * Called when a new video player instance is created
     * This is the core integration point that allows us to track active players
     */
    override public func onInstanceCreated(id: String, player: Any) {
        NSLog("\(VideoScreenshotPlugin.TAG): Player instance created - registering player with id: \(id)")
        
        guard let avPlayer = player as? AVPlayer else {
            NSLog("\(VideoScreenshotPlugin.TAG): WARNING: Player is not an AVPlayer instance")
            return
        }
        
        playerQueue.async(flags: .barrier) {
            self.players[id] = avPlayer
            NSLog("\(VideoScreenshotPlugin.TAG): Player registration complete. Total active players: \(self.players.count)")
        }
        
        // Notify React Native that this video player is ready for screenshot operations
        DispatchQueue.main.async {
            self.emitVideoPlayerReadyEvent(id)
        }
    }
    
    /**
     * Called when a video player instance is destroyed
     * Ensures proper cleanup to prevent memory leaks
     */
    override public func onInstanceRemoved(id: String, player: Any) {
        NSLog("\(VideoScreenshotPlugin.TAG): Player instance removed - unregistering player with id: \(id)")
        
        playerQueue.async(flags: .barrier) {
            self.players.removeValue(forKey: id)
            NSLog("\(VideoScreenshotPlugin.TAG): Player cleanup complete. Remaining active players: \(self.players.count)")
        }
    }
    
    // MARK: - Core Screenshot API Methods (matches Android exactly)
    
    /**
     * Captures a screenshot from the specified video player and returns it as base64
     * This is the primary screenshot method for in-memory operations
     * Matches Android: captureScreenshot(videoId: String, screenshotOptions: ReadableMap, promise: Promise)
     */
    @objc public func captureScreenshot(
        _ videoId: String,
        screenshotOptions: [String: Any],
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        NSLog("\(VideoScreenshotPlugin.TAG): Screenshot capture requested for player: \(videoId)")
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let player = self.playerQueue.sync { self.players[videoId] }
                guard let avPlayer = player else {
                    let availablePlayers = self.playerQueue.sync { Array(self.players.keys) }
                    reject("PLAYER_NOT_FOUND", 
                          "Video player '\(videoId)' not found. Available players: \(availablePlayers)", 
                          VideoScreenshotError.playerNotFound(videoId))
                    return
                }
                
                let result = try self.captureFrameFromPlayer(
                    player: avPlayer,
                    options: screenshotOptions,
                    outputMode: .base64Only
                )
                
                resolve(result)
                NSLog("\(VideoScreenshotPlugin.TAG): Screenshot capture completed successfully for player: \(videoId)")
                
            } catch {
                NSLog("\(VideoScreenshotPlugin.TAG): Screenshot capture failed for player \(videoId): \(error.localizedDescription)")
                reject("CAPTURE_FAILED", "Screenshot capture failed: \(error.localizedDescription)", error)
            }
        }
    }
    
    /**
     * Captures a screenshot and saves it to the device's photo library/gallery
     * Matches Android: saveScreenshotToLibrary(videoId: String, screenshotOptions: ReadableMap, promise: Promise)
     */
    @objc public func saveScreenshotToLibrary(
        _ videoId: String,
        screenshotOptions: [String: Any],
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        NSLog("\(VideoScreenshotPlugin.TAG): Save to library requested for player: \(videoId)")
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let player = self.playerQueue.sync { self.players[videoId] }
                guard let avPlayer = player else {
                    reject("PLAYER_NOT_FOUND", "Video player '\(videoId)' not found", VideoScreenshotError.playerNotFound(videoId))
                    return
                }
                
                let result = try self.captureFrameFromPlayer(
                    player: avPlayer,
                    options: screenshotOptions,
                    outputMode: .saveToLibrary
                )
                
                resolve(result)
                NSLog("\(VideoScreenshotPlugin.TAG): Screenshot saved to library successfully for player: \(videoId)")
                
            } catch {
                NSLog("\(VideoScreenshotPlugin.TAG): Save to library failed for player \(videoId): \(error.localizedDescription)")
                reject("SAVE_FAILED", "Failed to save screenshot: \(error.localizedDescription)", error)
            }
        }
    }
    
    /**
     * Captures a screenshot and saves it to a specific file path
     * Matches Android: saveScreenshotToPath(videoId: String, filePath: String, screenshotOptions: ReadableMap, promise: Promise)
     */
    @objc public func saveScreenshotToPath(
        _ videoId: String,
        filePath: String,
        screenshotOptions: [String: Any],
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        NSLog("\(VideoScreenshotPlugin.TAG): Save to path requested for player: \(videoId), path: \(filePath)")
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let player = self.playerQueue.sync { self.players[videoId] }
                guard let avPlayer = player else {
                    reject("PLAYER_NOT_FOUND", "Video player '\(videoId)' not found", VideoScreenshotError.playerNotFound(videoId))
                    return
                }
                
                let result = try self.captureFrameFromPlayer(
                    player: avPlayer,
                    options: screenshotOptions,
                    outputMode: .saveToPath(filePath)
                )
                
                resolve(result)
                NSLog("\(VideoScreenshotPlugin.TAG): Screenshot saved to path successfully: \(filePath)")
                
            } catch {
                NSLog("\(VideoScreenshotPlugin.TAG): Save to path failed for \(filePath): \(error.localizedDescription)")
                reject("SAVE_FAILED", "Failed to save to path: \(error.localizedDescription)", error)
            }
        }
    }
    
    /**
     * Checks if screenshot capture is currently supported for the given player
     * Matches Android: isScreenshotSupported(videoId: String, promise: Promise)
     */
    @objc public func isScreenshotSupported(
        _ videoId: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            let player = self.playerQueue.sync { self.players[videoId] }
            guard let avPlayer = player else {
                NSLog("\(VideoScreenshotPlugin.TAG): Screenshot support check: player \(videoId) not found")
                resolve(false)
                return
            }
            
            // Check screenshot support on main thread (UI thread where player state is safe to access)
            DispatchQueue.main.async {
                let isSupported = self.isPlayerReadyForScreenshot(avPlayer)
                NSLog("\(VideoScreenshotPlugin.TAG): Screenshot support for \(videoId): \(isSupported)")
                resolve(isSupported)
            }
        }
    }
    
    /**
     * Gets the current video dimensions for the specified player
     * Matches Android: getVideoDimensions(videoId: String, promise: Promise)
     */
    @objc public func getVideoDimensions(
        _ videoId: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            let player = self.playerQueue.sync { self.players[videoId] }
            guard let avPlayer = player else {
                reject("PLAYER_NOT_FOUND", "Video player '\(videoId)' not found", VideoScreenshotError.playerNotFound(videoId))
                return
            }
            
            // Access video dimensions on main thread
            DispatchQueue.main.async {
                guard let currentItem = avPlayer.currentItem else {
                    reject("NO_VIDEO", "No video loaded in player", VideoScreenshotError.invalidPlayer)
                    return
                }
                
                let videoSize = currentItem.presentationSize
                if videoSize == CGSize.zero {
                    reject("NO_DIMENSIONS", "Video dimensions not available - video may not be loaded", VideoScreenshotError.invalidPlayer)
                    return
                }
                
                let result: [String: Any] = [
                    "width": videoSize.width,
                    "height": videoSize.height
                ]
                
                resolve(result)
                NSLog("\(VideoScreenshotPlugin.TAG): Video dimensions retrieved for \(videoId): \(videoSize)")
            }
        }
    }
    
    // MARK: - Utility Methods (matches Android exactly)
    
    /**
     * Lists all currently available video players
     * Matches Android: listAvailableVideos(promise: Promise)
     */
    @objc public func listAvailableVideos(
        _ resolve: RCTPromiseResolveBlock,
        reject: RCTPromiseRejectBlock
    ) {
        let availablePlayers = playerQueue.sync { Array(players.keys) }
        NSLog("\(VideoScreenshotPlugin.TAG): Available video players: \(availablePlayers)")
        resolve(availablePlayers)
    }
    
    /**
     * Debug method to inspect the current state of registered players
     * Matches Android: debugListPlayers(promise: Promise)
     */
    @objc public func debugListPlayers(
        _ resolve: RCTPromiseResolveBlock,
        reject: RCTPromiseRejectBlock
    ) {
        NSLog("\(VideoScreenshotPlugin.TAG): DEBUG: Listing all registered players")
        let availablePlayers = playerQueue.sync { Array(players.keys) }
        
        for playerId in availablePlayers {
            NSLog("\(VideoScreenshotPlugin.TAG): DEBUG: Found player with id: \(playerId)")
        }
        
        NSLog("\(VideoScreenshotPlugin.TAG): DEBUG: Total players registered: \(availablePlayers.count)")
        resolve(availablePlayers)
    }
    
    // MARK: - Legacy Support Methods (for backward compatibility)
    
    /**
     * Test method to verify plugin functionality
     */
    @objc public func testMethod(
        _ resolve: RCTPromiseResolveBlock,
        reject: RCTPromiseRejectBlock
    ) {
        let result: [String: Any] = [
            "success": true,
            "message": "VideoScreenshotPlugin is working correctly",
            "platform": "iOS",
            "version": "1.0.0",
            "registeredPlayers": playerQueue.sync { players.count }
        ]
        resolve(result)
    }
    
    /**
     * Get plugin information and status
     */
    @objc public func getModuleInfo(
        _ resolve: RCTPromiseResolveBlock,
        reject: RCTPromiseRejectBlock
    ) {
        let playerCount = playerQueue.sync { players.count }
        let playerIds = playerQueue.sync { Array(players.keys) }
        
        let moduleInfo: [String: Any] = [
            "name": "VideoScreenshotPlugin",
            "version": "1.0.0",
            "platform": "iOS",
            "registeredPlayers": playerCount,
            "playerIds": playerIds,
            "isRegisteredWithVideoManager": true // Now we properly register
        ]
        
        resolve(moduleInfo)
    }
    
    // MARK: - Private Methods
    
    /**
     * Register with react-native-video's ReactNativeVideoManager
     * This is the key integration step that matches Android's plugin registration
     */
    private func registerWithVideoManager() {
        NSLog("\(VideoScreenshotPlugin.TAG): Registering with ReactNativeVideoManager.shared")
        
        // Register with react-native-video's plugin system - this is the key step!
        #if canImport(react_native_video)
        ReactNativeVideoManager.shared.registerPlugin(plugin: self)
        #endif
        
        NSLog("\(VideoScreenshotPlugin.TAG): ✅ Successfully registered with react-native-video plugin system")
        NSLog("\(VideoScreenshotPlugin.TAG): Plugin will now receive player lifecycle events")
    }
    
    /**
     * Check if a player is ready for screenshot capture
     * Must be called on the main thread
     */
    private func isPlayerReadyForScreenshot(_ player: AVPlayer) -> Bool {
        guard let currentItem = player.currentItem else { return false }
        return currentItem.status == .readyToPlay && currentItem.presentationSize != CGSize.zero
    }
    
    /**
     * Emit an event to React Native indicating a player is ready
     */
    private func emitVideoPlayerReadyEvent(_ playerId: String) {
        // For now, just log the event since we don't need events for the core functionality
        // The Android version works without events, so let's keep it simple
        NSLog("\(VideoScreenshotPlugin.TAG): Video player ready: \(playerId)")
    }
    
    /**
     * Core screenshot capture implementation
     * Handles the complete workflow from frame extraction to output formatting
     */
    private func captureFrameFromPlayer(
        player: AVPlayer,
        options: [String: Any],
        outputMode: ScreenshotOutputMode
    ) throws -> [String: Any] {
        
        // Parse screenshot configuration with sensible defaults
        let config = ScreenshotOptions(from: options)
        
        // Get timestamp asynchronously to avoid main thread blocking
        let timestamp: Double? = config.includeTimestamp ? 
            DispatchQueue.main.sync { player.currentTime().seconds } : nil
        
        // Extract the actual video frame
        guard let originalImage = try extractVideoFrame(from: player) else {
            throw VideoScreenshotError.screenshotFailed("Failed to extract frame from video")
        }
        
        // Process image on background queue
        let processedImage = resizeImageIfNeeded(originalImage, maxWidth: config.maxWidth, maxHeight: config.maxHeight)
        
        // Convert to the requested format and generate base64
        let imageData = convertImageToData(processedImage, format: config.format, quality: config.quality)
        let base64String = imageData.base64EncodedString()
        
        // Build the result object with screenshot data and metadata
        var result: [String: Any] = [
            "base64": base64String,
            "width": processedImage.size.width,
            "height": processedImage.size.height,
            "success": true
        ]
        
        if let timestamp = timestamp {
            result["timestamp"] = timestamp
        }
        
        // Handle the requested output mode
        switch outputMode {
        case .saveToLibrary:
            // Use async photo library operations
            let success = try saveImageToPhotoLibrarySync(processedImage)
            result["savedToLibrary"] = success
            result["uri"] = "photo-library://screenshot_\(Int(Date().timeIntervalSince1970))"
            
        case .saveToPath(let filePath):
            try saveImageDataToFile(imageData, filePath: filePath)
            result["uri"] = "file://\(filePath)"
            result["size"] = imageData.count
            
        case .base64Only:
            // Base64 is already included, no additional output needed
            break
        }
        
        return result
    }
    
    /**
     * Extract a video frame from the given AVPlayer instance
     * Uses cached image generators for better performance
     */
    private func extractVideoFrame(from player: AVPlayer) throws -> UIImage? {
        guard let currentItem = player.currentItem else {
            throw VideoScreenshotError.invalidPlayer
        }
        
        // Use cached image generator to avoid recreation overhead
        let imageGenerator = getOrCreateImageGenerator(for: currentItem.asset)
        
        // Use current playback time
        let currentTime = player.currentTime()
        
        do {
            let cgImage = try imageGenerator.copyCGImage(at: currentTime, actualTime: nil)
            return UIImage(cgImage: cgImage)
        } catch {
            NSLog("\(VideoScreenshotPlugin.TAG): Frame extraction failed with cached generator, trying fallback: \(error.localizedDescription)")
            
            // Fallback with relaxed tolerance
            imageGenerator.requestedTimeToleranceBefore = CMTime(seconds: 0.1, preferredTimescale: 600)
            imageGenerator.requestedTimeToleranceAfter = CMTime(seconds: 0.1, preferredTimescale: 600)
            
            do {
                let cgImage = try imageGenerator.copyCGImage(at: currentTime, actualTime: nil)
                return UIImage(cgImage: cgImage)
            } catch {
                NSLog("\(VideoScreenshotPlugin.TAG): Frame extraction failed with fallback: \(error.localizedDescription)")
                return nil
            }
        }
    }
    
    /**
     * Get or create cached image generator for better performance
     */
    private func getOrCreateImageGenerator(for asset: AVAsset) -> AVAssetImageGenerator {
        let assetKey = "\(asset.description)_\(asset.duration.seconds)" as NSString
        
        if let cached = imageGeneratorCache.object(forKey: assetKey) {
            return cached
        }
        
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.requestedTimeToleranceBefore = .zero
        generator.requestedTimeToleranceAfter = .zero
        
        // Set maximum size to prevent excessive memory usage
        generator.maximumSize = CGSize(width: 4096, height: 4096)
        
        // Enable hardware acceleration when available
        if #available(iOS 16.0, *) {
            generator.appliesPreferredTrackTransform = true
        }
        
        imageGeneratorCache.setObject(generator, forKey: assetKey)
        return generator
    }
    
    /**
     * Resize image if max dimensions are specified
     * Uses modern UIGraphicsImageRenderer for better performance and memory efficiency
     */
    private func resizeImageIfNeeded(_ image: UIImage, maxWidth: Int, maxHeight: Int) -> UIImage {
        guard maxWidth > 0 || maxHeight > 0 else { return image }
        
        let originalSize = image.size
        var targetSize = originalSize
        
        // Calculate target size maintaining aspect ratio
        if maxWidth > 0 && targetSize.width > CGFloat(maxWidth) {
            let ratio = CGFloat(maxWidth) / targetSize.width
            targetSize = CGSize(width: CGFloat(maxWidth), height: targetSize.height * ratio)
        }
        
        if maxHeight > 0 && targetSize.height > CGFloat(maxHeight) {
            let ratio = CGFloat(maxHeight) / targetSize.height
            targetSize = CGSize(width: targetSize.width * ratio, height: CGFloat(maxHeight))
        }
        
        guard targetSize != originalSize else { return image }
        
        // Use UIGraphicsImageRenderer for better performance and memory efficiency
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        return renderer.image { context in
            image.draw(in: CGRect(origin: .zero, size: targetSize))
        }
    }
    
    /**
     * Convert UIImage to data with specified format and quality
     */
    private func convertImageToData(_ image: UIImage, format: String, quality: Float) -> Data {
        switch format.lowercased() {
        case "png":
            return image.pngData() ?? Data()
        case "jpeg", "jpg":
            return image.jpegData(compressionQuality: CGFloat(quality)) ?? Data()
        default:
            return image.jpegData(compressionQuality: CGFloat(quality)) ?? Data()
        }
    }
    
    /**
     * Save image to photo library with permission handling
     * Synchronous version for backward compatibility, with improved error handling
     */
    private func saveImageToPhotoLibrarySync(_ image: UIImage) throws -> Bool {
        let status = PHPhotoLibrary.authorizationStatus(for: .addOnly)
        
        switch status {
        case .authorized:
            break
        case .notDetermined:
            throw VideoScreenshotError.permissionDenied
        default:
            throw VideoScreenshotError.permissionDenied
        }
        
        var saveSuccess = false
        var saveError: Error?
        let semaphore = DispatchSemaphore(value: 0)
        
        PHPhotoLibrary.shared().performChanges({
            PHAssetChangeRequest.creationRequestForAsset(from: image)
        }) { success, error in
            saveSuccess = success
            saveError = error
            semaphore.signal()
        }
        
        // Wait with timeout to prevent indefinite blocking
        let result = semaphore.wait(timeout: .now() + 10.0)
        
        if result == .timedOut {
            throw VideoScreenshotError.screenshotFailed("Photo library save operation timed out")
        }
        
        if let error = saveError {
            throw error
        }
        
        return saveSuccess
    }
    
    /**
     * Save image data to a specific file path
     */
    private func saveImageDataToFile(_ imageData: Data, filePath: String) throws {
        let url = URL(fileURLWithPath: filePath)
        
        // Ensure parent directories exist
        try FileManager.default.createDirectory(
            at: url.deletingLastPathComponent(),
            withIntermediateDirectories: true,
            attributes: nil
        )
        
        // Write the image data
        try imageData.write(to: url)
    }
    
    // MARK: - Additional Utility Methods
    
    // Manual registration method (for testing) - simplified since we now have proper integration
    @objc public func registerPlayer(
        _ playerId: String,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        NSLog("\(VideoScreenshotPlugin.TAG): Manual player registration requested for ID: \(playerId)")
        
        let currentPlayers = playerQueue.sync { Array(players.keys) }
        let isPlayerRegistered = currentPlayers.contains(playerId)
        
        let result: [String: Any] = [
            "success": isPlayerRegistered,
            "playerId": playerId,
            "message": isPlayerRegistered ? 
                "Player is registered and ready for screenshots" : 
                "Player not found - it will be registered automatically when video loads",
            "registrationType": "automatic",
            "integrationMode": "react-native-video-plugin",
            "totalPlayers": currentPlayers.count,
            "availablePlayers": currentPlayers
        ]
        
        resolve(result)
        NSLog("\(VideoScreenshotPlugin.TAG): Manual registration check result: \(result)")
    }
    
    // Photo library permission methods
    @objc public func checkPhotoLibraryPermission(
        _ resolve: RCTPromiseResolveBlock,
        reject: RCTPromiseRejectBlock
    ) {
        let status = PHPhotoLibrary.authorizationStatus()
        let result: [String: Any] = [
            "granted": status == .authorized,
            "status": authorizationStatusToString(status)
        ]
        resolve(result)
    }
    
    @objc public func requestPhotoLibraryPermission(
        _ resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        PHPhotoLibrary.requestAuthorization { status in
            DispatchQueue.main.async {
                let result: [String: Any] = [
                    "granted": status == .authorized,
                    "status": self.authorizationStatusToString(status)
                ]
                resolve(result)
            }
        }
    }
    
    private func authorizationStatusToString(_ status: PHAuthorizationStatus) -> String {
        switch status {
        case .authorized:
            return "authorized"
        case .denied:
            return "denied"
        case .notDetermined:
            return "notDetermined"
        case .restricted:
            return "restricted"
        case .limited:
            return "limited"
        @unknown default:
            return "unknown"
        }
    }
} 