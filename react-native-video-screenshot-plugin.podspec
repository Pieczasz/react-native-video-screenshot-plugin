require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-video-screenshot-plugin"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "11.0" }
  s.source       = { :git => "https://github.com/pieczasz/react-native-video-screenshot-plugin.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.requires_arc = true

  # Dependencies
  s.dependency "React-Core"
  s.dependency "react-native-video"

  # Enable C++ features
  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES",
    "SWIFT_OBJC_INTERFACE_HEADER_NAME" => "VideoScreenshotPlugin-Swift.h"
  }

  # Support for new architecture
  install_modules_dependencies(s)

  s.compiler_flags = '-DRCT_NEW_ARCH_ENABLED=1' if ENV['RCT_NEW_ARCH_ENABLED'] == '1'
end