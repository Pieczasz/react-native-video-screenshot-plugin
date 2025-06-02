//
//  VideoScreenshotPlugin-Bridging-Header.h
//  React Native Video Screenshot Plugin
//
//  This bridging header enables Swift code to access Objective-C APIs
//  from React Native and react-native-video frameworks.
//

#ifndef VideoScreenshotPlugin_Bridging_Header_h
#define VideoScreenshotPlugin_Bridging_Header_h

// React Native Core
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTLog.h>
#import <React/RCTUtils.h>
#import <React/RCTConvert.h>

// React Native UI Manager (for main thread operations)
#import <React/RCTUIManager.h>

// Foundation and UIKit (usually imported automatically, but explicit for clarity)
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <Photos/Photos.h>

// react-native-video imports (required for plugin functionality)
// Import the bridging header to get access to Swift classes
#if __has_include(<react-native-video/RCTVideo-Bridging-Header.h>)
#import <react-native-video/RCTVideo-Bridging-Header.h>
#endif

#if __has_include("RCTVideo-Bridging-Header.h")
#import "RCTVideo-Bridging-Header.h"
#endif

#endif /* VideoScreenshotPlugin_Bridging_Header_h */ 