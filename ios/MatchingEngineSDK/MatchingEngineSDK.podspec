#
# Be sure to run `pod lib lint MatchingEngineSDK.podspec' to ensure this is a
# valid spec before submitting.
#
# Any lines starting with a # are optional, but their use is encouraged
# To learn more about a Podspec see https://guides.cocoapods.org/syntax/podspec.html
#

Pod::Spec.new do |s|
  s.name             = 'MatchingEngineSDK'
  s.version          = '0.1.45'
  s.summary          = 'A short description of MatchingEngineSDK.'

# This description is used to generate tags and improve search results.
#   * Think: What does it do? Why did you write it? What is the focus?
#   * Try to keep it short, snappy and to the point.
#   * Write the description between the DESC delimiters below.
#   * Finally, don't worry about the indent, CocoaPods strips it!

  s.description      = <<-DESC
TODO: Add long description of the pod here.
                       DESC

  s.homepage         = 'https://github.com/metaartisan/MatchingEngineSDK'
  # s.screenshots     = 'www.example.com/screenshots_1', 'www.example.com/screenshots_2'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'metaartisan' => 'github@metatheory.com' }
  s.source           = { :git => 'https://github.com/metaartisan/MatchingEngineSDK.git', :tag => s.version.to_s }
  # s.social_media_url = 'https://twitter.com/<TWITTER_USERNAME>'

  s.ios.deployment_target = '11.4'

  s.source_files = 'MatchingEngineSDK/Classes/**/*'
  
  # s.resource_bundles = {
  #   'MatchingEngineSDK' => ['MatchingEngineSDK/Assets/*.png']
  # }

  # s.public_header_files = 'Pod/Classes/**/*.h'
  # s.frameworks = 'UIKit', 'MapKit'
  
  s.dependency 'NSLogger/Swift'
  s.dependency 'Alamofire'
  s.dependency 'SwiftyJSON'

  ##  s.dependency 'GoogleMaps', '= 2.7.0'

  
  
end
