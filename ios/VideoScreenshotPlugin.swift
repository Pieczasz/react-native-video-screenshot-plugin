import react_native_video
import AVFoundation
import AVKit
import UIKit
import Photos

@objc(VideoScreenshotPlugin)
class VideoScreenshotPlugin: RNVAVPlayerPlugin {
    private var players: [String: AVPlayer] = [:]
    
    /**
     * Create an init function to register the plugin
     */
    override init() {
        super.init()
        ReactNativeVideoManager.shared.registerPlugin(plugin: self)
    }
    
    deinit {
        ReactNativeVideoManager.shared.unregisterPlugin(plugin: self)
    }
    
    /*
     * Handlers called on player creation and destruction
     */
    override func onInstanceCreated(id: String, player: AVPlayer) {
        NSLog("VideoScreenshotPlugin: onInstanceCreated for id: \(id)")
        players[id] = player
    }

    override func onInstanceRemoved(id: String, player: AVPlayer) {
        NSLog("VideoScreenshotPlugin: onInstanceRemoved for id: \(id)")
        players.removeValue(forKey: id)
    }
    
    /**
     * Captures a screenshot from the current video frame
     */
    @objc
    func captureScreenshot(_ videoId: String, 
                          options: [String: Any],
                          resolve: @escaping RCTPromiseResolveBlock,
                          reject: @escaping RCTPromiseRejectBlock) {
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                reject("PLUGIN_ERROR", "Plugin instance not available", nil)
                return
            }
            
            guard let player = self.players[videoId] else {
                reject("PLAYER_NOT_FOUND", "Video player with id '\(videoId)' not found", nil)
                return
            }
            
            guard let playerItem = player.currentItem else {
                reject("NO_CURRENT_ITEM", "No video item is currently playing", nil)
                return
            }
            
            self.captureFrameFromPlayer(player: player, 
                                      playerItem: playerItem,
                                      options: options,
                                      saveToLibrary: false,
                                      customPath: nil,
                                      resolve: resolve,
                                      reject: reject)
        }
    }
    
    /**
     * Captures a screenshot and saves it to the photo library
     */
    @objc
    func saveScreenshotToLibrary(_ videoId: String,
                                options: [String: Any],
                                resolve: @escaping RCTPromiseResolveBlock,
                                reject: @escaping RCTPromiseRejectBlock) {
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                reject("PLUGIN_ERROR", "Plugin instance not available", nil)
                return
            }
            
            guard let player = self.players[videoId] else {
                reject("PLAYER_NOT_FOUND", "Video player with id '\(videoId)' not found", nil)
                return
            }
            
            guard let playerItem = player.currentItem else {
                reject("NO_CURRENT_ITEM", "No video item is currently playing", nil)
                return
            }
            
            // Check photo library permission
            PHPhotoLibrary.requestAuthorization { status in
                if status == .authorized {
                    self.captureFrameFromPlayer(player: player,
                                              playerItem: playerItem,
                                              options: options,
                                              saveToLibrary: true,
                                              customPath: nil,
                                              resolve: resolve,
                                              reject: reject)
                } else {
                    reject("PERMISSION_DENIED", "Photo library access permission denied", nil)
                }
            }
        }
    }
    
    /**
     * Captures a screenshot and saves it to a custom path
     */
    @objc
    func saveScreenshotToPath(_ videoId: String,
                             filePath: String,
                             options: [String: Any],
                             resolve: @escaping RCTPromiseResolveBlock,
                             reject: @escaping RCTPromiseRejectBlock) {
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                reject("PLUGIN_ERROR", "Plugin instance not available", nil)
                return
            }
            
            guard let player = self.players[videoId] else {
                reject("PLAYER_NOT_FOUND", "Video player with id '\(videoId)' not found", nil)
                return
            }
            
            guard let playerItem = player.currentItem else {
                reject("NO_CURRENT_ITEM", "No video item is currently playing", nil)
                return
            }
            
            self.captureFrameFromPlayer(player: player,
                                      playerItem: playerItem,
                                      options: options,
                                      saveToLibrary: false,
                                      customPath: filePath,
                                      resolve: resolve,
                                      reject: reject)
        }
    }
    
    /**
     * Checks if screenshot capture is supported for the current video
     */
    @objc
    func isScreenshotSupported(_ videoId: String,
                              resolve: @escaping RCTPromiseResolveBlock,
                              reject: @escaping RCTPromiseRejectBlock) {
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                reject("PLUGIN_ERROR", "Plugin instance not available", nil)
                return
            }
            
            guard let player = self.players[videoId] else {
                resolve(false)
                return
            }
            
            guard let playerItem = player.currentItem,
                  let videoTrack = playerItem.tracks.first(where: { $0.assetTrack?.mediaType == .video })?.assetTrack else {
                resolve(false)
                return
            }
            
            // Check if video track is readable
            resolve(videoTrack.isPlayable && playerItem.status == .readyToPlay)
        }
    }
    
    /**
     * Gets the current video dimensions
     */
    @objc
    func getVideoDimensions(_ videoId: String,
                           resolve: @escaping RCTPromiseResolveBlock,
                           reject: @escaping RCTPromiseRejectBlock) {
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                reject("PLUGIN_ERROR", "Plugin instance not available", nil)
                return
            }
            
            guard let player = self.players[videoId] else {
                reject("PLAYER_NOT_FOUND", "Video player with id '\(videoId)' not found", nil)
                return
            }
            
            guard let playerItem = player.currentItem,
                  let videoTrack = playerItem.tracks.first(where: { $0.assetTrack?.mediaType == .video })?.assetTrack else {
                reject("NO_VIDEO_TRACK", "No video track found", nil)
                return
            }
            
            let naturalSize = videoTrack.naturalSize
            let transform = videoTrack.preferredTransform
            
            // Apply transform to get correct dimensions
            let size = naturalSize.applying(transform)
            let dimensions = [
                "width": abs(size.width),
                "height": abs(size.height)
            ]
            
            resolve(dimensions)
        }
    }
    
    // MARK: - Private Methods
    
    private func captureFrameFromPlayer(player: AVPlayer,
                                       playerItem: AVPlayerItem,
                                       options: [String: Any],
                                       saveToLibrary: Bool,
                                       customPath: String?,
                                       resolve: @escaping RCTPromiseResolveBlock,
                                       reject: @escaping RCTPromiseRejectBlock) {
        
        guard let videoTrack = playerItem.tracks.first(where: { $0.assetTrack?.mediaType == .video })?.assetTrack else {
            reject("NO_VIDEO_TRACK", "No video track found", nil)
            return
        }
        
        let currentTime = player.currentTime()
        let imageGenerator = AVAssetImageGenerator(asset: playerItem.asset)
        imageGenerator.appliesPreferredTrackTransform = true
        imageGenerator.requestedTimeToleranceAfter = .zero
        imageGenerator.requestedTimeToleranceBefore = .zero
        
        // Configure maximum size if provided
        if let maxWidth = options["maxWidth"] as? CGFloat,
           let maxHeight = options["maxHeight"] as? CGFloat {
            imageGenerator.maximumSize = CGSize(width: maxWidth, height: maxHeight)
        } else if let maxWidth = options["maxWidth"] as? CGFloat {
            imageGenerator.maximumSize = CGSize(width: maxWidth, height: CGFloat.greatestFiniteMagnitude)
        } else if let maxHeight = options["maxHeight"] as? CGFloat {
            imageGenerator.maximumSize = CGSize(width: CGFloat.greatestFiniteMagnitude, height: maxHeight)
        }
        
        imageGenerator.generateCGImagesAsynchronously(forTimes: [NSValue(time: currentTime)]) { [weak self] _, cgImage, _, result, error in
            
            if let error = error {
                reject("CAPTURE_FAILED", "Failed to capture screenshot: \(error.localizedDescription)", error)
                return
            }
            
            guard let cgImage = cgImage else {
                reject("CAPTURE_FAILED", "Failed to generate image from video frame", nil)
                return
            }
            
            let image = UIImage(cgImage: cgImage)
            let format = options["format"] as? String ?? "jpeg"
            let quality = options["quality"] as? CGFloat ?? 0.9
            let includeTimestamp = options["includeTimestamp"] as? Bool ?? true
            
            var imageData: Data?
            var mimeType: String
            
            if format.lowercased() == "png" {
                imageData = image.pngData()
                mimeType = "image/png"
            } else {
                imageData = image.jpegData(compressionQuality: quality)
                mimeType = "image/jpeg"
            }
            
            guard let data = imageData else {
                reject("ENCODE_FAILED", "Failed to encode image data", nil)
                return
            }
            
            let base64String = data.base64EncodedString()
            let timestamp = includeTimestamp ? CMTimeGetSeconds(currentTime) : nil
            
            var result: [String: Any] = [
                "base64": base64String,
                "width": image.size.width,
                "height": image.size.height
            ]
            
            if let timestamp = timestamp {
                result["timestamp"] = timestamp
            }
            
            // Handle saving
            if saveToLibrary {
                self?.saveImageToPhotoLibrary(image: image, 
                                            result: &result, 
                                            resolve: resolve, 
                                            reject: reject)
            } else if let customPath = customPath {
                self?.saveImageToPath(data: data, 
                                    path: customPath, 
                                    result: &result, 
                                    resolve: resolve, 
                                    reject: reject)
            } else {
                resolve(result)
            }
        }
    }
    
    private func saveImageToPhotoLibrary(image: UIImage,
                                        result: inout [String: Any],
                                        resolve: @escaping RCTPromiseResolveBlock,
                                        reject: @escaping RCTPromiseRejectBlock) {
        
        PHPhotoLibrary.shared().performChanges({
            PHAssetChangeRequest.creationRequestForAsset(from: image)
        }) { success, error in
            if success {
                result["uri"] = "ph://saved-to-library"
                resolve(result)
            } else {
                reject("SAVE_FAILED", "Failed to save screenshot to photo library: \(error?.localizedDescription ?? "Unknown error")", error)
            }
        }
    }
    
    private func saveImageToPath(data: Data,
                                path: String,
                                result: inout [String: Any],
                                resolve: @escaping RCTPromiseResolveBlock,
                                reject: @escaping RCTPromiseRejectBlock) {
        
        do {
            let url = URL(fileURLWithPath: path)
            
            // Create directory if it doesn't exist
            let directory = url.deletingLastPathComponent()
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true, attributes: nil)
            
            try data.write(to: url)
            
            result["uri"] = url.absoluteString
            result["size"] = data.count
            resolve(result)
            
        } catch {
            reject("SAVE_FAILED", "Failed to save screenshot to path: \(error.localizedDescription)", error)
        }
    }
}