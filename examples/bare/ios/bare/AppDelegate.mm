#import "AppDelegate.h"

#import <React/RCTBundleURLProvider.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  self.moduleName = @"bare";
  // You can add your custom initial props in the dictionary below.
  // They will be passed down to the ViewController used by React Native.
  self.initialProps = @{};

  // Force legacy bridge mode to ensure proper module compatibility
  self.bridgelessEnabled = NO;

  return [super application:application didFinishLaunchingWithOptions:launchOptions];
}

@end 