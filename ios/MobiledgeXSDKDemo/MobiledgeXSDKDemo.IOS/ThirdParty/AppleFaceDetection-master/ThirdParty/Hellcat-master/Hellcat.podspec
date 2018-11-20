Pod::Spec.new do |s|
  s.name             = 'Hellcat'
  s.version          = '0.4.0'
  s.summary          = 'Products an array of UIImages from a video file'
  s.description      = <<-DESC
Hellcat is a light weight tool for breaking a video file up into an array of images.
                       DESC

  s.homepage         = 'https://github.com/seanmcneil/Hellcat'
  s.license          = { :type => 'MIT', :file => 'LICENSE' }
  s.author           = { 'seanmcneil' => 'mcneilsean@icloud.com' }
  s.source           = { :git => 'https://github.com/seanmcneil/Hellcat.git', :tag => s.version.to_s }
  s.social_media_url = 'https://twitter.com/sean_mcneil'
  s.ios.deployment_target = '8.3'
  s.source_files = 'Hellcat/Classes/**/*'
end
