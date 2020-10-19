# Magic Leap Unity Project Template

## Project

MobiledgeX ComputerVision POC test client for ComputerVision FaceDetecion on Magic Leap One. This project was originally based on the Magic Leap template app.
This is the standalone version of the code.

## Versions

### Unity

2019.3.15f1

### MLSDK

v0.24.1

### LuminOS

0.98.11

## Downloading

1) git clone https://github.com/mobiledgex/edge-cloud-sampleapps.git
2) cd edge-cloud-sampleapps/unity/MagicLeapFaceDetectionPOC

## Instructions After Downloading

1) Using Unity Hub, download Unity 2019.3.15f1 and make sure Lumin support is checked during installation
2) Launch the MobiledgeX Computer Vision project.
3) Install Magic Leap assets: https://developer.magicleap.com/en-us/learn/guides/unity-setup
4) If not already open, Open the `DeviceSetup` Scene from `Assets`>`Scenes`>`DeviceSetup`
5) Import the MobiledgeX SDK for Unity SDK:
   Window --> Package Manager --> + --> url --> https://github.com/mobiledgex/edge-cloud-sdk-unity.git
6) Run initial setup to point the sample at a running Sample AppInst:
   Main Menu --> MobiledgeX --> Setup:

   Organization: MoibledgeX-Samples
   AppName: ComputerVision-GPU
   AppVersion: 2.2
  
   Click setup to test the connection. It shoould say FIND_FOUND for the nearest edge server app
   installation, if one exists for your region.
7) Ensure some permissions are set:
   Shift + Command + B --> Select Lumin and set as target platform if not already.
8) Player Settings, tick some minimum permissions. This app does not use the permisison plugin.
   MagicLeap
     --> Manifest Settings --> Reality, Check CameraCapture, ComputerVision
     --> Sensitive, Check CoarseLocation (note, this does not seem to work right now, indoors).
     --> Autogranted, Check Internet (needed to reach the edge server)
9) Device side, after build and install
   Settings --> Applications --> ComputerVision. Check everything the app needs. This app does not use
   the permisison plugin.
10) FindClouldlet seems to take a long while to get a result once the app starts (this is likely a bug).
    Wait about 20 seconds, and it should discover either the fallback location (In San Jose, CA), or
    a real MLLocation provided location.
11) Point it at a set of faces once connected to the face detection server to see rectanges. There is more
    to do in this Proof of Concept to improve this code.
12) ./mldb log to monitor some verbose log output on which AppInst and the state of CompanionDeviceManager.cs/GameObject.
