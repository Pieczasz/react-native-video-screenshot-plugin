#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(VideoScreenshotPlugin, NSObject)

RCT_EXTERN_METHOD(captureScreenshot:(NSString *)videoId
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(saveScreenshotToLibrary:(NSString *)videoId
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(saveScreenshotToPath:(NSString *)videoId
                  filePath:(NSString *)filePath
                  options:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(isScreenshotSupported:(NSString *)videoId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getVideoDimensions:(NSString *)videoId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)

@end