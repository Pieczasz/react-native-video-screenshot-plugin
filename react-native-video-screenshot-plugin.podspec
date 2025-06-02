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
  s.source       = { :git => "https://github.com/Pieczasz/react-native-video-screenshot-plugin.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.requires_arc = true

  # Dependencies
  s.dependency "React-Core"
  s.dependency "react-native-video"

  # We are using only old bridge architecture for now - both for the iOS and Android implementations
  # TurboModule support can be added later
end