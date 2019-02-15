#
# Be sure to run `pod lib lint MatchingEngineSDK.podspec' to ensure this is a
# valid spec before submitting.
#
# Any lines starting with a # are optional, but their use is encouraged
# To learn more about a Podspec see https://guides.cocoapods.org/syntax/podspec.html
#

Pod::Spec.new do |s|
  s.name             = 'MatchingEngineSDK'
  s.version          = '0.1.47'
  s.summary          = 'The MobiledgeX SDK for iOS Swift provides Swift APIs that allows developers to communicate to MobiledgeX infrastructure and to utilize its services.'

# This description is used to generate tags and improve search results.
#   * Think: What does it do? Why did you write it? What is the focus?
#   * Try to keep it short, snappy and to the point.
#   * Write the description between the DESC delimiters below.
#   * Finally, don't worry about the indent, CocoaPods strips it!

  s.description      = <<-DESC
Are you excited to connect to MobiledgeX Cloudlet Infrastructure and leverage the power that Mobile Edge Cloud offers? The MobiledgeX SDK for iOS Swift exposes various services that MobiledgeX offers such as finding the nearest MobiledgeX Cloudlet Infrastructure for client-server communication or workload processing offload.
                       DESC

  s.homepage         = 'https://github.com/mobiledgex/MatchingEngineSDK'
  # s.screenshots     = 'www.example.com/screenshots_1', 'www.example.com/screenshots_2'
  s.license          = { :type => 'Apache.LICENSE-2.0', :file => 'LICENSE' }
  s.author           = { 'mobiledgex' => 'github@metatheory.com' }
  s.source           = { :git => 'https://github.com/mobiledgex/MatchingEngineSDK.git', :tag => s.version.to_s }
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

    s.swift_version = '4.2'
  
  
end
