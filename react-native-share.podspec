require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = package["name"]
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
  react-native-share
                   DESC
  s.homepage     = "https://github.com/eleduck/react-native-share"
  s.license      = "MIT"
  s.authors      = { "PBK-B" => "eleduck" }
  s.platforms    = { :ios => "9.0" }
  s.source       = { :git => "https://github.com/eleduck/react-native-share", :tag => "#{s.version}" }
  
  s.frameworks = 'ImageIO', 'SystemConfiguration', 'Security', 'CoreTelephony', 'CoreText'
  s.libraries = 'sqlite3', 'z'
  s.resource = "ios/*/*.bundle"
  s.vendored_libraries = 'ios/**/*.a'
  s.vendored_frameworks = [
    'ios/**/*.framework'
  ]
  s.source_files = "ios/**/*.{h,m,swift}"
  s.requires_arc = true
  s.dependency "React"
  
end