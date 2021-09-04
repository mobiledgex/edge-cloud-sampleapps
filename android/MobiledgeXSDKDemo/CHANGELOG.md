# Change Log

All notable changes to the **Android SDK Demo App** project will be documented in this file.

## 2021 releases

### 1.1.17 - Sep 3, 2:40 PM

#### Major Features:  
- EDGECLOUD-4940 Add support for routes, and using the locations to demonstrate EdgeEvents
- EDGECLOUD-5017 Add preferences for EdgeEvents config

#### Improvements and Bug Fixes
- EDGECLOUD-2553 Remove base64 encoding of image data
- EDGECLOUD-5092 Add TLS switch preference for the case of overriding the Edge hostname
- EDGECLOUD-4594 Add option for using SDK's NetTest for cloudlet latency testing.
- EDGECLOUD-3589 HA features with CV activities don't handle non-success cases for FindCloudlet
- EDGECLOUD-4940 Settings update
  - Migrated Settings screens to androidx.
  - Broke Computer Vision Settings into 3 separate pages.
  - Added preferences for driving/flying route animation duration.
- New context/popup menu for the event log viewer: Long press the collapse/expand button to select one of these menu items:
  - Copy all items
  - Clear Logs
  - Auto Expand toggle - Turn off, and viewer doesn't automatically expand when new event occurs.
- Added CSS to "About" HTML to the use MobiledgeX colors instead of stark black and white.

## 2020 releases

### 1.1.12 - Oct 2, 10:40 AM

- EDGECLOUD-2595 - Remove benchmark videos from APK and make them downloadable. Also added support for inputting custom video URL.
- EDGECLOUD-3246 - Move MobiledgeX Samples from MobiledgeX org to MobiledgeX-Samples
- EDGECLOUD-3609 - Setting to override Cloud, Edge, and GPU hostname
- EDGECLOUD-3625 - Add support for app instances with TLS ports
- EDGECLOUD-3671 - Remove defaultedge domain usage

### 1.1.11 - Aug 24, 10:48 AM

- Updated to use latest version of SDK: com.mobiledgex:matchingengine:2.1.2.
- EDGECLOUD-3475 Rework icon coloring logic

### 1.1.10 - July 31, 5:18 PM

- For Pose Detection and Object Detection activities, a findCloudlet is performed at startup and the resulting server is used for inference.
- Added the following to Pose Detection and Object Detection activities (previously available only for Face Detection and Face Recognition):
  - Find Closest Cloudlet
  - Get App Instances
  - Manual Failover
- Backend to connect to is now called “ComputerVision” instead of “MobildegeX SDK Demo”. This preference is automatically updated upon first run, but the user can change it back if needed.

The following improvements/bugs are resolved:
- EDGECLOUD-2956 Add support for setLimit for getAppInstances
- EDGECLOUD-3110 Add failover support to GPU CV activities
- EDGECLOUD-3187 FindCloudlet Performance mode doesn't work on map world activity.
- EDGECLOUD-3228 App should do findcloudlet for GPU-specific app inst for GPU CV activities

### 1.1.9 - July 8, 6:04 PM

- New Matching Engine Setting: "Get App Instances Limit"
- Long press on log view toggle button to clear all logs.
- Bug fix for FindCloudlet mode setting in the map activity.
- Bug fix for using Carrier Name setting in Face Detection activity.
- Fixed a couple of reported crashes.

### 1.1.8 - June 25, 11:52 AM

Fixes the following:

- EDGECLOUD-2855 Modify Sample App to Demo Application HA
- EDGECLOUD-3064 Demo app - Catch DmeHostException and Use WifiOnly when a user has an unsupported mccmnc
- EDGECLOUD-2974 Demo app - Add preference for findCloudlet: performance or proximity

List of changes:
- App name, version, organization can all be changed in General Settings.
- “About” dialog updated to show these values.
- HA (High Availability): When Computer Vision server connectivity is lost, failover to next closest cloudlet. Added "Auto Failover" setting to determine if this is automatic or a manual action.
- Added the following to Face Detection and Face Recognition activities:
  - Find Closest Cloudlet
  - Get App Instances
  - Manual Failover
- Added expandable log viewer to Computer Vision activities. Long press to copy all contents to clipboard. If using Vysor or Scrcpy, it will also be copied to your desktop clipboard.
- New Setting to toggle display of public cloud processing output.
- Removed PQoE until TDG restores backend.
- New setting for choosing findCloudlet mode: PROXIMITY or PERFORMANCE.
- Use WifiOnly when a user has an unsupported MCCMNC
- Fixed crash when no SIM card.

### 1.1.7 - June 17, 5:33 PM

Please note that with this update you may not see the list of cloudlets that you’re used to. If you wish to see cloudlets from a particular region or operator, please select those in “General Settings” after unchecking “Use Default”.

- EDGECLOUD-2684 - Computer Vision Server: Face Training data to be stored in Redis
- EDGECLOUD-2776 - Demo App: Should use current MCCMNC as default for "operator" setting
- EDGECLOUD-2929 - Integrate SDK 2.0.7 with Sample Applications
- Migration to AndroidX

### 1.1.6 - May 5, 9:50 AM

- EDGECLOUD-2289 - Support for WebSocket protocol for Computer Vision activities.
- EDGECLOUD-2431 - Benchmarking mode using canned video.
- EDGECLOUD-2424 - Remove local OpenCV libraries from app. Local face detection processing no longer supported.

### 1.1.5 - Mar 27, 11:56 AM

No user-visible updates. Changes are under the hood:
- EDGECLOUD-2242 DME API change: Developer Name to Organization Name.
- Using SDK’s builder methods for creating DME requests.

### 1.1.4 - Mar 12, 4:46 PM

- EDGECLOUD-2001 Add object detection using GPU-accelerated library
- EDGECLOUD-157 Demo app "About" dialog/page with links
- EDGECLOUD-2261 Demo app - Add backend info to "About" dialog

### 1.1.3 - Mar 2, 11:56 AM

- EDGECLOUD-2119 - Reset Find Cloudlet result when applicable settings are changed.
- EDGECLOUD-2160 - Not asking for Camera permission when permission changes 'on the fly'.
- Fixed case where entering Face Recognition training guest subject name could start or end with a space, causing confusing naming on the server.
- Updated icon to latest corporate standard.
- Clarification of cloudlet type in face detection stats dialog.

### 1.1.2 - Jan 30, 3:37 PM

- Combined SDK demo backend with face detection server. New backend is "MobiledgeX SDK Demo" version 2.0.
- Added "UL SPEED TEST" to Cloudlet Details page.
- Removed dynamic/static download preference. Only dynamic will be used.
- New larger download preference values.
- Only allow location simulator update on environments with active simulator
- EDGECLOUD-1840 - Standardize verifyLocation results

## 2019 Releases

### 1.1.1 - Dec 5, 6:59 PM

- Regenerated Google API keys

### 1.1.0 - Dec 5, 4:01 PM

- First public release.
- Default DME is now EU.
- Default OpenPose server is now posedetection.defaultedge.mobiledgex.net

### 1.0.48 - Sep 18, 11:12 AM

- Regenerated API keys.
- Updated to SDK version 1.4.13.
- Update default DME hostname to sdkdemo.dme.mobiledgex.net.
- Fix error that occurred on first time changing DME preference because default value didn't include the port number too.
- Also catch exception when Directions API key isn't valid and display error Toast instead of crashing.

### 1.0.47 - Sep 4, 10:42 AM

- QOS: Added capability to Load CSV file in format compatible with DT's web UI.
- QOS: Menu item to access help file for CSV format.
- QOS: Minor fixes for Grid point mode.

### 1.0.46 - Aug 26, 11:24 AM

- EDGECLOUD-1058 - Demo app - Replace OkSocket library with local socket code.
- EDGECLOUD-1089 - Demo app - Update to PQoE activity to use SDK's implementation
- Grid point collection mode.
- Test JSON data can be loaded from the options menu.
- Score calculations match https://predictive-qos.t-systems-service.com/.

### 1.0.45 - Aug 3, 7:27 PM

- Updated to SDK 1.4.8 to support connecting to DME using public certs instead of self-signed.
- EDGECLOUD-1091 - List of DMEs now comes from dme-inventory.mobiledgex
- Changing either Region Selection or Operator Name automatically repopulates appInstances on the map.

### 1.0.44 - Jul 2, 1:15 PM

- EDGECLOUD-835 Demo app - Permissions issue causing crash on Android 6.0 phone
- EDGECLOUD-658 Demo app - All DME calls fail when network switching is enabled on SIM-less device

### 1.0.43 - Jun 24, 12:52 PM

- Fixed bugs in training mode introduced with Persistent TCP connection mode.

### 1.0.42 - Jun 14, 2:36 PM

- New connection mode for Computer Vision activities: Persistent TCP. This is selectable through a new preference. The new mode provides a lower Full Process latency. The lower the network latency, the better the improvement will be.

### 1.0.41 - Jun 3, 5:47 PM

- Updating to SDK version 1.4.0, to match DME updates.

### 1.0.40 - Apr 28, 8:43 PM

- Reset Location now resets closest cloudlet for image detection too.
- Find Closest Cloudlet now uses correct FQDN for image detection.
- New preference added for OpenPose hostname.

### 1.0.39 - Apr 24, 7:00 pm

- Support for new centralized training data for face detection.
- Added support to remove training data from the server.
- User must be logged in in order to do training or data removal. They can only remove guest data that they themselves added (enforced by the server).
- If "Find Closest Cloudlet" is performed from the map activity, that cloudlet's hostname will be used for "Edge" in the detection/recognition activities.
- Computer Vision activities now have separate LatencyTestMethod setting not tied to Speed Test Settings.
- Fixed Pose Detection, front camera, landscape mode skeleton coordinates not aligned.
- Under the hood: All camera-related code is separated into new MobiledgeX Computer Vision library.

### 1.0.38 - Mar 14, 5:59 pm

- To match back-end updates, changed internal developer name from "MobiledgeX SDK Demo" to "MobiledgeX".

### 1.0.37 - Feb 23, 3:10 pm

- Support for Face Detection/Recognition video playback in portrait or landscape mode.
- Removed Face Detection/Recognition startup delay in case that ICMP ping isn't supported on a server.

### 1.0.36 - Feb 22, 6:39 pm

- Fixed "Latency = 0 ms" bug when rolling average setting turned off.

### 1.0.35 - Feb 22, 5:10 pm

- Added "Play Video" menu item to play a pre-packaged video, which can be used in any of the computer vision activities.
- Removed canned images option.
- Aspect ratio tweaks to help with distorted images sometimes seen on Samsung Galaxy S4.
- Fixed "Use Rolling Average" setting not being honored.
- Updated face detection server to return less false positives.

### 1.0.34 - Feb 19, 7:45 pm

- Added camera debug info collector.
- Added canned images to use in place of camera.

### 1.0.33 - Feb 19, 3:37 pm

- Reverting default camera operation to use normal Camera2 API with ImageReader.
- Added new "Legacy Camera" preference which, when enabled, pulls the camera image from the preview texture instead of the ImageReader. This allows better FPS on older phones.
- Fixed incorrectly repeating fade animation for face coordinates rectangle.

### 1.0.32 - Feb 18, 7:41 pm

Face Recognition/Detection updates:
- Support for older devices: minSdkVersion API level 21, Android 5.0.
- Camera optimizations for better frame rate on slower devices.
- Standardized image sizes sent to Computer Vision server.
- Fixed brightness issue with Samsung Galaxy S5.

### 1.0.31 - Jan 23, 4:11 pm

- Switched to JPEG encoding for uploaded images, for significant latency speedup.
- "Face Detection Settings" page now called "Computer Vision Settings".
- New preference to allow displaying of latency stats at the end of a camera session.

### 1.0.30 - Jan 15, 3:35 pm

- Fixed crash in OpenPose pose detection.
- Automatically use socket test for latency for pose detection to avoid initial delay for ping to fail. (ICMP not supported on server.)

### 1.0.29 - Jan 15, 2:11 pm

- OpenPose pose detection now uses DNS entry and public IP for server.
- Network-only latency stats included.

### 1.0.28 - Jan 11, 6:52 pm

- EDGECLOUD-322 - OpenPose pose detection support. Server is hard-coded to Bonn GPU-enabled server.
- Refactoring camera and network classes for better modularity (ongoing).

## 2018 Releases

### 1.0.27 - Dec 21, 4:28 pm

- Bug fix where selecting "Use as Face server host" on Azure-based cloudlet failed.
- Crash fixes.

### 1.0.26 - Dec 18, 7:04 pm

- Internal updates to support updated Demo environment (FQDN prefix, ports, etc.), and new default values for the Face server hostnames.
- Resolved IP address of FQDN is displayed in Cloudlet Details.
- Direct link to speed test settings from Cloudlet Details.
- New "automationbonn" environment added to preferences.

### 1.0.25 - Nov 29, 4:27 pm

- EDGECLOUD-291 - Support for all locationVerify categories on non-demo environment.
- EDGECLOUD-297 - Support static files for download test
- Fix for GPS spoof distance on user icon.
- Time and Date pickers for Predictive Quality of Experience (PQoE).

### 1.0.24 - Nov 21, 4:44 pm

- Face detection/recognition hosts now defined by FQDN instead of IP addresses.
- New "Reset hosts to default" preference in Face Detection Settings.
- Added Legend for Predictive Quality of Experience (PQoE) map colors.

### 1.0.23 - Nov 13, 4:56 pm

- New version of Predictive Quality of Experience (PQoE) demo.
- Handles multiple routes.
- GRPC stability improvements, including ignoring stale data received after moving markers.
- Custom "Silver" map styling makes routes more visible, because default map style included greens and yellows similar to route colors.
- Improvements to cert handling to allow PQoE and regular DME demo to coexist happily.

### 1.0.22 - Nov 6, 8:02 pm

- Removed mexint.dme.mobiledgex.net DME selection.
- EDGECLOUD-257 - Remove location simulator update on non-demo environments.
- Backing out Predictive QoS demo.

### 1.0.21 - Nov 6, 2:05 pm

Preliminary Predictive QoS demo. Expect instability!

### 1.0.20 - Nov 5, 7:40 pm

- Standardizing naming convention for URIs and hostnames.
- Select Cloud or Edge Face Recognition server from Cloudlet Details screen.
- Error detection when new Face Recognition server is selected. If connection fails, value is reset to default.
- Support for Microsoft cloudlets ("M" badge).

### 1.0.19 - Nov 4, 9:07 pm

- Updates for DME API reorg.
- Customizable server preference for Face Recognition servers.
- During Face Recognition, if ping failure is detected, switch to socket latency test method.

### 1.0.18 - Oct 23, 10:28 am

- Guest Training Mode for Face Recognition.

### 1.0.16 - Oct 15, 10:48 pm

- EDGECLOUD-212 - Face recognition, including training mode.
- Supports "Sign in with Google". Sign-in carries over through multiple invocations of the app. "Sign out" is also available when you are signed in.
- Proper Action Bar in Face Detection and Recognition activities, instead of floating action buttons.
- Lots of duplicate Edge/Cloud code has been refactored to a shared ImageSender class.
- Encoded image is now sent as a post parameter instead of the body. This allows other parameters to be included, like subject name.
- Standardized icon colors.

To be added to the face recognition trained data on the cloudlet/server, you must be signed in to your Google account in the app. From the main menu, choose "Sign in with Google", then follow the on-screen instructions. From the Face Recognition screen, select the 3-dot menu, then "Training Mode". A progress bar is shown as 10 images are collected, then the server will be trained using these new images. Once training is complete, you should see your name associated with the live image of your face on the screen.

### 1.0.15 - Oct 3, 10:36 am

- Fixed bug where autostart preference was used even for manual latency test.
- Updates for EDGECLOUD-196 - Change GetCloudlets to GetAppInstList
- Added support for local OpenCV Face Detection processing. Can be turned on in preferences.

Note: Adding the OpenCV libraries to the app increased the size of the APK from 4.5 MB to 85 MB.

### 1.0.14 - Sep 22, 4:25 pm

- Multiple faces tracked in Face Detection
- Button to switch between front and back facing cameras.
- Several new preferences to customize the Face Detection screen (whether to show latency, etc.)
- Smoother camera preview from optimizing where image resize happens in the process
- 100-sample rolling average for less jumpy latency numbers. Can be turned off in preferences.
- Added standard deviation for network-only latency. Visibility controllable in preferences.

### 1.0.13 - Sep 19, 11:55 am

- Updates to support new URI scheme for TDG Cloudlets in the demo environment.
- Fixed bug where recognition rectangles couldn't move to bottom of portrait screen, or far right of landscape screen.
- Fixed back arrow button in Settings for Android version 6.
- Fixed cert loading on Android version 6. This was causing all DME requests to fail.
- Fixed crash when performing "Reset Location" before receiving cloudlet list.
- Fixed incorrectly turning on network manager based on Enhanced Network Location Services preference.
- Clear "Ping failed" message on reattempt.

### 1.0.12 - Sep 17, 11:51 PM

- No longer creates 2 icons upon install
- EDGECLOUD-164 Consent Flow: First Time Use dialogs
- Support system ping for Face Detection background latency testing. Uses speed test preference.
- Ping is now the default Latency Test method. Changeable in Settings.
- If ping method selected, Azure cloudlets automatically use socket anyway. "Socket test forced" message shown by Latency Test button.

### 1.0.11 - Sep 15, 7:04 PM

- Removed location verification status codes. Now shows only verified or failed.
- Background network-only latency measurement on Face Detection screen. Rearranged measurement text.
- Turned off network manager by default. May be enabled in Settings.
- Summary text added for all settings.
- EDGECLOUD-156 Separate thread for location verification, find closest cloudlet, so these can be executed without having to wait for all latency tests to complete.
- Fade-out animation on Face Detection rectangles.

### 1.0.10 - Sep 14, 3:23 PM

- Configurable DME hostname
- Enabled network manager
- Improved layout of latency readings in Face Detection
- Removed some noisy logs

### 1.0.7 - Sep 13, 11:43 PM

- EDGECLOUD-161 New Face Detection activity.

### 1.0.6 - Sep 10, 5:46 PM

- Added support for both ping and socket latency test methods.
- "Caution" color mobile icon is now green for LOC_ROAMING_COUNTRY_MATCH.

### 1.0.5 - Sep 7, 5:29 PM

- EDGECLOUD-151 Ping replacement allows latency testing of all cloudlets, including Azure.

### 1.0.4 - Sep 6, 6:06 PM

- EDGECLOUD-154 Shows new location verification codes when clicking on the mobile marker.
- EDGECLOUD-148 New "caution" amber color for LOC_ROAMING_COUNTRY_MATCH.
- Menu cleanup.

### 1.0.3 - Sep 6, 11:31 AM

- Fix for Google Maps API key.

### 1.0.1 - Sep 6, 12:00 AM

EDGECLOUD-131 Initial release.
