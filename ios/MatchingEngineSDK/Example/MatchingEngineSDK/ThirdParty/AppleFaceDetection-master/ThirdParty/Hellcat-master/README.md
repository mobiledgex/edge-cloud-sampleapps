# Hellcat
![Hellcat: Seamlessly create images from video](https://raw.githubusercontent.com/seanmcneil/Hellcat/master/hellcats.jpg)

[![CI Status](http://img.shields.io/travis/seanmcneil/Hellcat.svg?style=flat)](https://travis-ci.org/seanmcneil/Hellcat)
[![Version](https://img.shields.io/cocoapods/v/Hellcat.svg?style=flat)](http://cocoapods.org/pods/Hellcat)
[![License](https://img.shields.io/cocoapods/l/Hellcat.svg?style=flat)](http://cocoapods.org/pods/Hellcat)
[![Platform](https://img.shields.io/cocoapods/p/Hellcat.svg?style=flat)](http://cocoapods.org/pods/Hellcat)
[![CocoaPods](https://img.shields.io/cocoapods/dt/Hellcat.svg)](http://cocoapods.org/pods/Hellcat)

## What it does

Hellcat will take a URL for a video and return an array of UIImages that it extracts from each frame.

## Features

-- Tracks the progress of each video being processed via progress callback
-- Returns an array of UIImage once complete
-- Will return an error that complies with the HellcatError if something goes wrong

## Example

To run the example project, clone the repo, and run `pod install` from the Example directory first.

The following snippet will send the URL for a video to Hellcat for processing:

```swift
let hellcat = Hellcat()
hellcat.imageFrames(for: url, progress: { (progress) in
    // Display progress
}, success: { (images) in
    // Handle images
}) { (error) in
    // Handle error
}
```

## Requirements
- iOS 8.3+
- Xcode 8.0+
- Swift 3.0+

## Installation

Hellcat is available through [CocoaPods](http://cocoapods.org). To install
it, simply add the following line to your Podfile:

```ruby
pod "Hellcat"
```

## Errors

Hellcat provides a relatively rich set of errors via an enum that should address all potential failures within the app. These are:

```swift
public enum HellcatError: Error {
    case FailureToObtainAssetTrack
    case FailureToCreateAssetReader(Error)
    case FailureToObtainImage
}
```

## Changelog

[Changelog](https://github.com/seanmcneil/Hellcat/blob/master/CHANGELOG.md) | See the changes introduced in each version.

## Credit

The use of a [sample video](https://www.pond5.com/stock-footage/44516099/charlie-chaplin-kid-auto-races-venice-1914.html#1) in the example app is in the public domain.

## Author

seanmcneil, mcneilsean@icloud.com

## License

Hellcat is available under the MIT license. See the LICENSE file for more info.
