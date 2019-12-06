## MobiledgeX SDK Demo App How-To

### Features
- Display world map showing MobiledgeX cloudlets where the app's backend is running.
- Allows calling individual MobiledgeX APIs:
    - Register Client
    - Get App Instances
    - Verify Location
    - Find Closest Cloudlet
- Cloudlet Latency Test
- Cloudlet Download Speed Test
- GPS Spoofing Demo (see below)
- Face Detection
- Face Recognition
- Pose Detection
- Predictive Quality of Service

### Cloudlet Map
The app's startup page is a world map showing MobiledgeX cloudlets where the app's backend is running. If no cloudlets are showing, or you don't see the set cloudlets you expect to see, click the main menu "hawkinser" icon, and select **Settings**, then **General Settings**. From there, select the **Region** and **Operator Name** you're interested in.

Tap on any of the cloudlets, and a panel with the cloudlet's name will appear. On that panel, you can "click for details", to see cloudlet information like latitude, longitude, and distance from your location.

#### Cloudlet Details
In addition to showing details about the cloudlet, this page also lets you initiate a latency test, or a download speed test. The settings for this page let you control things like the number of packets for the latency test, or the size of the download. Click the "Gear" icon to access these settings.

You can also choose this cloudlet to be used as the **Edge** server for Face Detection and Face Recognition. Tap the 3-dot menu and select "Use as Face Recognition Edge Host". The **Cloud** server can be set in the same way.

### SDK Calls
#### Register Client
This will call [RegisterClient](http://swagger.mobiledgex.net/client/#operation/RegisterClient "RegisterClient") with information that identifies this app's backend software. The session cookie is shown to verify that the call was successful.

#### Get App Instances
This will call [GetAppInstList](http://swagger.mobiledgex.net/client/#operation/GetAppInstList "GetAppInstList") to find everywhere that our backend is running, and will draw a cloudlet icon for every location found.

#### Verify Location
This will call [VerifyLocation](http://swagger.mobiledgex.net/client/#operation/VerifyLocation "VerifyLocation") with our current GPS coordinates to verify that our mobile device is really where it claims to be. If successful, the mobile phone icon is colored green, and the accuracy information is shown briefly. If location verification fails, the icon will be colored red and the result code will be briefly shown. Tapping the icon will show the result code of the call.

#### Find Closest Cloudlet
This will call [FindCloudlet](http://swagger.mobiledgex.net/client/#operation/FindCloudlet "FindCloudlet") with our current GPS coordinates to determine which cloudlet running our backend is the closest. That cloudlet icon will turn green, and a line will be drawn between it and our location.

#### Perform All
Select the red button in the lower right to perform all of these API calls in succession.

### GPS Spoofing Demo
The app uses the MobiledgeX [VerifyLocation](http://swagger.mobiledgex.net/client/#operation/VerifyLocation "VerifyLocation") API call to verify that the GPS coordinates reported are where the device actually is. 

When you start the app, you will see your location on the map represented by a mobile phone icon. 

In the real world, the location verification is implemented by the cellular network operator. For this demo, we use a simulator that stores location data, and is keyed on your current public IP address. To get an initial entry in the simulator database, tap the 3-dot menu, and select "Reset Location". Now the simulator considers your true location to be wherever your current GPS coordinates are. If your IP address stays the same, you won't need to repeat this step next time you run the app.

You can move the mobile phone icon by long-pressing on it and dragging it to a new location, or long-pressing anywhere on the map. When you release the icon, you will be presented with two options:
- Spoof GPS at this location
- Update location in GPS database.

If you select "Spoof", this new location will be used for subsequent **Find Closest Cloudlet** and **Verify Location** calls. Depending on how close this location is to the location stored in the simulator, your next **Verify Location** call may return a different "accuracy" value, or return a failure. Tapping the mobile phone icon will the distance from the actual location, and any result code available.

If you select "Update location", this new location will be sent to the location simulator and will be used until another location is sent, or "Reset Location" is performed. This allows you to simulate having your real location be anywhere in the world, and you can verify that the expected cloudlet is found by **Find Closest Cloudlet**.

### Computer Vision Demos
Select one of the **Detection** or **Recognition** activities from the main menu. You can control several options, like which stats are displayed by selecting the "gear" icon to access **Computer Vision Settings**. 

Things to try:
- You may switch between the front and rear camera by tapping the camera icon.
- Tap the 3-dot menu, and select "Play Video" to process a canned video instead of images from the camera. This is useful for unattended demos or benchmarking.
- Go to settings and turn on "Show Latency Stats after session" to get a stats summary that can be copy/pasted for additional use.

#### Face Detection
The Face Detection activity provides a visual comparison of the latency provided by an Edge cloudlet vs. that of a server in the public cloud. The Edge cloudlet can be determined in a few ways, in this order of priority: 
1. The result of "Find Closest Cloudlet", if performed.
1. Selected from within the Cloudlet Details page.
1. "Edge Server" entry in the **Computer Vision Settings**, if updated by the user.
1. The default hostname: facedetection.defaultedge.mobiledgex.net.

Images from the camera are sent to a server which uses OpenCV to detect faces in the image. The server returns coordinates of any faces, and the app draws rectangles around them. 

#### Face Recognition
Face Recognition is similar, but instead of only detecting the presense of a face in an image, the name of the subject will also be returned if the person is part of the training data. To add your own face to the training data, tap the 3-dot menu and select "Training Mode". You must be signed in to your Google account via the main menu, so that your name can be associated with the face images. "Guest Training Mode" is also available to add images of another person, whose name you must enter. You own your own images and any guest images you add, and you can remove them at any time, by selecting the appropriate menu entry.

Note that for Recognition, only a single face is supported at a time. If more than one face appears in the given image, only one of them will be detected and recognized.

#### Pose Detection
Pose Detection uses OpenPose on a GPU-enabled cloudlet to detect the pose of human bodies. Like the other activities, images from the camera are sent to the cloudlet for processing. Instead of rectangular coordinates, points representing the bones of the pose(s) are received and rendered.

There is no Edge/Cloud comparison for this activity. A single cloudlet is used for the image processing.

Note that Pose Detection does not use the result of "Find Closest Cloudlet", because there are a limited number of GPU-enabled cloudlets available.

### Predictive Quality of Service
This activity shows a map with two endpoints. The beginning and ending locations can be dragged around on the map to change the routes shown. PQoS data is retrieved for each point along the routes and is displayed in a color-coded style.

You can change the Date and Time of the predicted data by tapping the appropriate button. There are also menu entries to change the point mode (Route or Grid), and to change the map type. There is also support to load a CSV file containing position data to be rendered. For details, see Menu > CSV File > CVS File Help.

Currently, PQoS data is available only in Germany.


