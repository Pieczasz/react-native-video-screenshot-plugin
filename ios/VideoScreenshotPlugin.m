#import <React/RCTBridgeModule.h>

// Export the Swift VideoScreenshotPlugin class
@interface RCT_EXTERN_MODULE(VideoScreenshotPlugin, NSObject)

// Core screenshot functionality - matches Android exactly
RCT_EXTERN_METHOD(captureScreenshot:(NSString *)videoId
                  screenshotOptions:(NSDictionary *)screenshotOptions
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(saveScreenshotToLibrary:(NSString *)videoId
                  screenshotOptions:(NSDictionary *)screenshotOptions
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(saveScreenshotToPath:(NSString *)videoId
                  filePath:(NSString *)filePath
                  screenshotOptions:(NSDictionary *)screenshotOptions
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

// Utility methods - matches Android exactly
RCT_EXTERN_METHOD(isScreenshotSupported:(NSString *)videoId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getVideoDimensions:(NSString *)videoId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(listAvailableVideos:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(debugListPlayers:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

// Optional: Legacy support methods (can be removed later)
RCT_EXTERN_METHOD(testMethod:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getModuleInfo:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

// Photo library permission methods
RCT_EXTERN_METHOD(checkPhotoLibraryPermission:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(requestPhotoLibraryPermission:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

// Manual registration method (for testing)
RCT_EXTERN_METHOD(registerPlayer:(NSString *)playerId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

@end 