# MatchingEngineSDK

[![Version](https://img.shields.io/cocoapods/v/MatchingEngineSDK.svg?style=flat)](https://cocoapods.org/pods/MatchingEngineSDK)
[![License](https://img.shields.io/cocoapods/l/MatchingEngineSDK.svg?style=flat)](https://cocoapods.org/pods/MatchingEngineSDK)
[![Platform](https://img.shields.io/cocoapods/p/MatchingEngineSDK.svg?style=flat)](https://cocoapods.org/pods/MatchingEngineSDK)

## Example

To run the example project, clone the repo, and run `pod install` from the Example directory first.

## What this API does
 Allows access to network of edge cloudlets Azure/TDG
 	Register client
 	Get list of cloudlets
 	Find closest cloudlet
 	Verify location of client
 
## Requirements

### Build

Xcode 10, iOS 12 SDK

### Runtime

iOS 11.4

## Installation

MatchingEngineSDK is available through [CocoaPods](https://cocoapods.org). To install
it, simply add the following line to your Podfile:

```ruby
pod 'MatchingEngineSDK'
```

This will add both the Examlpe and SDK in your pods folder

## API

MexRegisterClient.createRegisterClientRequest(appName: "your app name", devNameL "your dev name", appVers: "1.0")

	 listen for  NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "Client registered"), object: nil, queue: nil)
	 // sets api variables used by the below functions: sessioncookie and tokenserveruri
	       
MexGetAppInst.createGetAppInstListRequest()

	listen for:  NotificationCenter.default.post(name: NSNotification.Name(rawValue: "processAppInstList"), object: result)   // Where 

MexVerifyLocation.createVerifyLocationRequest(location) // Where location is [String;Any]

	listen for:   NotificationCenter.default.post(name: NSNotification.Name(rawValue: "Verifylocation success"), object: result)   

MexFindNearestCloudlet.createFindCloudletRequest(location)	// closest

	 listen for: NotificationCenter.default.post(name: NSNotification.Name(rawValue: "processFindCloudletResult"), object: result)  
	
postRequest
 
## Quickstart - 

To get started with the MatchingEngineSDK follow these steps. 
Steps 1-4 install the SDK and it dependencies.
 The remaining steps 5-6 show how to use the SDK to RegisterClient


Steps:

1) Add the following to your project pod file (or add them  to your project directly)

	pod 'MatchingEngineSDK'

	pod 'Alamofire'
	pod 'NSLogger/Swift'

2) pod install


3) Add the following Sources to your project
	https://github.com/kean/FutureX

4) add your certs in .der format to your xcode project


5) Your first API command RegisterClient

	MexRegisterClient.registerClientNow(appName: “your app name”, devName:  “your dev name”,  appVers: “1.0”)
	//completion: NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "Client registered"), object: nil, queue: nil)


6) run

## Author

mobiledgex, MatchingEngineSDK@mobiledgex.com

## License

MatchingEngineSDK is available under the ??? license. See the LICENSE file for more info.

Copyright (C) 2019 MobiledgeX

