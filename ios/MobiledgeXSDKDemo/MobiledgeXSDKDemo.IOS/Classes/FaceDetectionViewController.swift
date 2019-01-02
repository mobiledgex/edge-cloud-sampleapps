//
//  FaceDetectionViewController.swift
//  VisionDetection
//
//  Created by Wei Chieh Tseng on 09/06/2017.
//  Copyright © 2017 Willjay. All rights reserved.
//

import AVFoundation
import UIKit
import Vision
// import PlainPing

var doAFaceDetection = true // JT 18.11.26 todo refactor patterned
var doAFaceRecognition = false // JT 18.12.10 one at a time

enum MexKind: Int // JT 18.12.14
{
    case mexCloud
    case mexEdge
}

class FaceDetectionViewController: UIViewController
{
    // VNRequest: Either Retangles or Landmarks
    var faceDetectionRequest: VNRequest!

    var sentImageSize = CGSize(width: 0, height: 0) // JT 18.11.28
    @IBOutlet var latencyCloudLabel: UILabel! // JT 18.12.11
    @IBOutlet var latencyEdgeLabel: UILabel! //

    @IBOutlet var faceRecognitionLatencyCloudLabel: UILabel! // JT 18.12.13
    @IBOutlet var faceRecognitionLatencyEdgeLabel: UILabel! // JT 18.12.13

    @IBOutlet var networkLatencyCloudLabel: UILabel! //
    @IBOutlet var networkLatencyEdgeLabel: UILabel! //

    // @IBOutlet weak var faceRecognitonNameLabel: UILabel! // todo remove
    @IBOutlet var faceRecognitonNameCloudLabel: UILabel! //
    @IBOutlet var faceRecognitonNameEdgeLabel: UILabel! //

    @IBOutlet var stddevCloudLabel: UILabel! // // JT 18.12.18
    @IBOutlet var stddevEdgeLabel: UILabel! //  // JT 18.12.18

    var futureEdge: Future<[String: AnyObject], Error>? // async result (captured by async?) // JT 18.12.13
    var futureCloud: Future<[String: AnyObject], Error>? // async result (captured by async?)    // JT 18.12.13

    let faceDetectionEdge = MexFaceRecognition() // JT 18.12.15
    let faceDetectionCloud = MexFaceRecognition() // JT 18.12.15
    
    var calcRollingAverageCloud = MovingAverage()    // JT 18.12.17
    var rollingAverageCloud = 0.0    // JT 18.12.17

    var calcRollingAverageEdge = MovingAverage()    // JT 18.12.17
    var rollingAverageEdge = 0.0    // JT 18.12.17

    override func viewDidLoad()
    {
        super.viewDidLoad()

        // Set up the video preview view.
        previewView.session = session

        // Set up Vision Request
        faceDetectionRequest = VNDetectFaceRectanglesRequest(completionHandler: handleFaces) // Default
        setupVision()

        /*
         Check video authorization status. Video access is required and audio
         access is optional. If audio access is denied, audio is not recorded
         during movie recording.
         */
        switch AVCaptureDevice.authorizationStatus(for: AVMediaType.video) {
        case .authorized:
            // The user has previously granted access to the camera.
            break

        case .notDetermined:
            /*
             The user has not yet been presented with the option to grant
             video access. We suspend the session queue to delay session
             setup until the access request has completed.
             */
            sessionQueue.suspend()
            AVCaptureDevice.requestAccess(for: AVMediaType.video, completionHandler: { [unowned self] granted in
                if !granted
                {
                    self.setupResult = .notAuthorized
                }
                self.sessionQueue.resume()
            })

        default:
            // The user has previously denied access.
            setupResult = .notAuthorized
        }

        /*
         Setup the capture session.
         In general it is not safe to mutate an AVCaptureSession or any of its
         inputs, outputs, or connections from multiple threads at the same time.

         Why not do all of this on the main queue?
         Because AVCaptureSession.startRunning() is a blocking call which can
         take a long time. We dispatch session setup to the sessionQueue so
         that the main queue isn't blocked, which keeps the UI responsive.
         */

        sessionQueue.async
        { [unowned self] in
            self.configureSession()
        }

        /// --- tmp

        let barButtonItem = UIBarButtonItem(title: "StopTmp", style: .plain, target: self, action: #selector(FaceDetectionViewController.stopIt(sender:))) // JT 18.12.14 todo toggle?

        navigationItem.rightBarButtonItem = barButtonItem // JT 18.11.28 tmp

        // ---

        let tv = UserDefaults.standard.bool(forKey: "doFaceRecognition") //

        title = tv ? "Face Recognition" : "Face Dectection" // JT 18.12.13

        // ---

        let latencyCloud = UserDefaults.standard.string(forKey: "latencyCloud") // JT 18.12.14
        networkLatencyCloudLabel.text = "Cloud: \(latencyCloud!) ms" // JT 18.12.11

        let latencyEdge = UserDefaults.standard.string(forKey: "latencyEdge") // JT 18.12.14
        networkLatencyEdgeLabel.text = "Edge: \(latencyEdge!) ms" // JT 18.12.11
        // ---

        doAFaceDetection = true // JT 18.12.14

        let localProcessing = UserDefaults.standard.bool(forKey: "Show full process latency") // JT 18.12.17
        if localProcessing == true // JT 18.12.17
        {
            latencyCloudLabel.isHidden = false
            latencyEdgeLabel.isHidden = false
        }

        if true // JT 18.12.17 todo
        {
            faceRecognitionLatencyCloudLabel.isHidden = false
            faceRecognitionLatencyCloudLabel.isHidden = false
        }

        let showNetworkLantency = UserDefaults.standard.bool(forKey: "Show network latency") // JT 18.12.17

        if showNetworkLantency // JT 18.12.17 todo
        {
            networkLatencyCloudLabel.isHidden = false
            networkLatencyEdgeLabel.isHidden = false
        }
        else
        {
            networkLatencyCloudLabel.isHidden = true
            networkLatencyEdgeLabel.isHidden = true
        }
        
        let showStddev = UserDefaults.standard.bool(forKey: "Show Stddev") // JT 18.12.17

        if showStddev   // JT 18.12.18
        {
            stddevCloudLabel.isHidden = false
            stddevEdgeLabel.isHidden = false
        }
        else
        {
            stddevCloudLabel.isHidden = true
            stddevEdgeLabel.isHidden = true
        }
    }

    @objc public func stopIt(sender _: UIBarButtonItem) // JT 18.11.28
    {
        Swift.print("stop") // JT 18.11.28
        session.stopRunning() // JT 18.11.28
    }

    override func viewWillAppear(_ animated: Bool)
    {
        super.viewWillAppear(animated)

        sessionQueue.async
        { [unowned self] in
            switch self.setupResult
            {
            case .success:
                // Only setup observers and start the session running if setup succeeded.
                self.addObservers()
                self.session.startRunning()
                self.isSessionRunning = self.session.isRunning

            case .notAuthorized:
                DispatchQueue.main.async
                { [unowned self] in
                    let message = NSLocalizedString("AVCamBarcode doesn't have permission to use the camera, please change privacy settings",
                                                    comment: "Alert message when the user has denied access to the camera")
                    let alertController = UIAlertController(title: "AppleFaceDetection", message: message, preferredStyle: .alert)
                    alertController.addAction(UIAlertAction(title: NSLocalizedString("OK", comment: "Alert OK button"), style: .cancel, handler: nil))
                    alertController.addAction(UIAlertAction(title: NSLocalizedString("Settings", comment: "Alert button to open Settings"), style: .default, handler: { _ in
                        UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!, options: [:], completionHandler: nil)
                    }))

                    self.present(alertController, animated: true, completion: nil)
                }

            case .configurationFailed:
                DispatchQueue.main.async
                { [unowned self] in
                    let message = NSLocalizedString("Unable to capture media", comment: "Alert message when something goes wrong during capture session configuration")
                    let alertController = UIAlertController(title: "AppleFaceDetection", message: message, preferredStyle: .alert)
                    alertController.addAction(UIAlertAction(title: NSLocalizedString("OK", comment: "Alert OK button"), style: .cancel, handler: nil))

                    self.present(alertController, animated: true, completion: nil)
                }
            }
        }
    }

    override func viewWillDisappear(_ animated: Bool)
    {
        sessionQueue.async
        { [unowned self] in
            if self.setupResult == .success
            {
                self.session.stopRunning()
                self.isSessionRunning = self.session.isRunning
                self.removeObservers()
            }
        }

        super.viewWillDisappear(animated)
    }

    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator)
    {
        super.viewWillTransition(to: size, with: coordinator)

        if let videoPreviewLayerConnection = previewView.videoPreviewLayer.connection
        {
            let deviceOrientation = UIDevice.current.orientation
            guard let newVideoOrientation = deviceOrientation.videoOrientation, deviceOrientation.isPortrait || deviceOrientation.isLandscape else
            {
                return
            }

            videoPreviewLayerConnection.videoOrientation = newVideoOrientation
        }
    }

    @IBAction func UpdateDetectionType(_ sender: UISegmentedControl)
    {
        // use segmentedControl to switch over VNRequest
        faceDetectionRequest = sender.selectedSegmentIndex == 0 ? VNDetectFaceRectanglesRequest(completionHandler: handleFaces) : VNDetectFaceLandmarksRequest(completionHandler: handleFaceLandmarks)

        setupVision()
    }

    @IBOutlet var previewView: PreviewView!

    // MARK: Session Management

    private enum SessionSetupResult
    {
        case success
        case notAuthorized
        case configurationFailed
    }

    private var devicePosition: AVCaptureDevice.Position = .back

    private let session = AVCaptureSession()
    private var isSessionRunning = false

    private let sessionQueue = DispatchQueue(label: "session queue", attributes: [], target: nil) // Communicate with the session and other session objects on this queue.

    private var setupResult: SessionSetupResult = .success

    private var videoDeviceInput: AVCaptureDeviceInput!

    private var videoDataOutput: AVCaptureVideoDataOutput!
    private var videoDataOutputQueue = DispatchQueue(label: "VideoDataOutputQueue")

    private var requests = [VNRequest]()

    private func configureSession()
    {
        if setupResult != .success
        {
            return
        }

        session.beginConfiguration()
        session.sessionPreset = .high

        // Add video input.
        do
        {
            var defaultVideoDevice: AVCaptureDevice?

            // Choose the back dual camera if available, otherwise default to a wide angle camera.
            if let dualCameraDevice = AVCaptureDevice.default(.builtInDualCamera, for: AVMediaType.video, position: .back)
            {
                defaultVideoDevice = dualCameraDevice
            }

            else if let backCameraDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: AVMediaType.video, position: .back)
            {
                defaultVideoDevice = backCameraDevice
            }

            else if let frontCameraDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: AVMediaType.video, position: .front)
            {
                defaultVideoDevice = frontCameraDevice
            }

            let videoDeviceInput = try AVCaptureDeviceInput(device: defaultVideoDevice!)

            if session.canAddInput(videoDeviceInput)
            {
                session.addInput(videoDeviceInput)
                self.videoDeviceInput = videoDeviceInput
                DispatchQueue.main.async
                {
                    /*
                     Why are we dispatching this to the main queue?
                     Because AVCaptureVideoPreviewLayer is the backing layer for PreviewView and UIView
                     can only be manipulated on the main thread.
                     Note: As an exception to the above rule, it is not necessary to serialize video orientation changes
                     on the AVCaptureVideoPreviewLayer’s connection with other session manipulation.

                     Use the status bar orientation as the initial video orientation. Subsequent orientation changes are
                     handled by CameraViewController.viewWillTransition(to:with:).
                     */
                    let statusBarOrientation = UIApplication.shared.statusBarOrientation
                    var initialVideoOrientation: AVCaptureVideoOrientation = .portrait
                    if statusBarOrientation != .unknown
                    {
                        if let videoOrientation = statusBarOrientation.videoOrientation
                        {
                            initialVideoOrientation = videoOrientation
                        }
                    }
                    self.previewView.videoPreviewLayer.connection!.videoOrientation = initialVideoOrientation
                }
            }

            else
            {
                print("Could not add video device input to the session")
                setupResult = .configurationFailed
                session.commitConfiguration()
                return
            }
        }
        catch
        {
            print("Could not create video device input: \(error)")
            setupResult = .configurationFailed
            session.commitConfiguration()
            return
        }

        // add output
        videoDataOutput = AVCaptureVideoDataOutput()
        videoDataOutput.videoSettings = [(kCVPixelBufferPixelFormatTypeKey as String): Int(kCVPixelFormatType_32BGRA)]

        if session.canAddOutput(videoDataOutput)
        {
            videoDataOutput.alwaysDiscardsLateVideoFrames = true
            videoDataOutput.setSampleBufferDelegate(self, queue: videoDataOutputQueue)
            session.addOutput(videoDataOutput)
        }
        else
        {
            print("Could not add metadata output to the session")
            setupResult = .configurationFailed
            session.commitConfiguration()
            return
        }

        session.commitConfiguration()
    }

    private func availableSessionPresets() -> [String]
    {
        let allSessionPresets = [AVCaptureSession.Preset.photo,
                                 AVCaptureSession.Preset.low,
                                 AVCaptureSession.Preset.medium,
                                 AVCaptureSession.Preset.high,
                                 AVCaptureSession.Preset.cif352x288,
                                 AVCaptureSession.Preset.vga640x480,
                                 AVCaptureSession.Preset.hd1280x720,
                                 AVCaptureSession.Preset.iFrame960x540,
                                 AVCaptureSession.Preset.iFrame1280x720,
                                 AVCaptureSession.Preset.hd1920x1080,
                                 AVCaptureSession.Preset.hd4K3840x2160]

        var availableSessionPresets = [String]()
        for sessionPreset in allSessionPresets
        {
            if session.canSetSessionPreset(sessionPreset)
            {
                availableSessionPresets.append(sessionPreset.rawValue)
            }
        }

        return availableSessionPresets
    }

    func exifOrientationFromDeviceOrientation() -> UInt32
    {
        enum DeviceOrientation: UInt32
        {
            case top0ColLeft = 1
            case top0ColRight = 2
            case bottom0ColRight = 3
            case bottom0ColLeft = 4
            case left0ColTop = 5
            case right0ColTop = 6
            case right0ColBottom = 7
            case left0ColBottom = 8
        }
        var exifOrientation: DeviceOrientation

        switch UIDevice.current.orientation {
        case .portraitUpsideDown:
            exifOrientation = .left0ColBottom
        case .landscapeLeft:
            exifOrientation = devicePosition == .front ? .bottom0ColRight : .top0ColLeft
        case .landscapeRight:
            exifOrientation = devicePosition == .front ? .top0ColLeft : .bottom0ColRight
        default:
            exifOrientation = .right0ColTop
        }
        return exifOrientation.rawValue
    }
}

extension FaceDetectionViewController
{
    private func addObservers()
    {
        /*
         Observe the previewView's regionOfInterest to update the AVCaptureMetadataOutput's
         rectOfInterest when the user finishes resizing the region of interest.
         */
        NotificationCenter.default.addObserver(self, selector: #selector(sessionRuntimeError), name: Notification.Name("AVCaptureSessionRuntimeErrorNotification"), object: session)

        /*
         A session can only run when the app is full screen. It will be interrupted
         in a multi-app layout, introduced in iOS 9, see also the documentation of
         AVCaptureSessionInterruptionReason. Add observers to handle these session
         interruptions and show a preview is paused message. See the documentation
         of AVCaptureSessionWasInterruptedNotification for other interruption reasons.
         */
        NotificationCenter.default.addObserver(self, selector: #selector(sessionWasInterrupted), name: Notification.Name("AVCaptureSessionWasInterruptedNotification"), object: session)
        NotificationCenter.default.addObserver(self, selector: #selector(sessionInterruptionEnded), name: Notification.Name("AVCaptureSessionInterruptionEndedNotification"), object: session)

        if true // JT 18.11.26
        {
            NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "FaceDetection"), object: nil, queue: nil) // JT 18.11.26
            { notification in
                // Swift.print("RegisterClient \(notification)")

                let d = notification.object as! [[Int]]

                // SKToast.show(withMessage: "FaceDetection raw result\(d)")

                // Swift.print("FaceDetection\n\(d)") // JT 18.11.27

                let a = d[0] // get face rect

                // let r = CGRect(CGFloat(a[0]), CGFloat(a[1]), CGFloat(a[2] - a[0]), CGFloat(a[3] - a[1])) // face rect
                let r = convertPointsToRect(a) // JT 18.12.13

                self.previewView.drawFaceboundingBox2(rect: r, hint: self.sentImageSize) // JT 18.11.28 blue

                Swift.print("face r= \(r)") // JT 18.11.28
                SKToast.show(withMessage: "FaceDetection result: \(r)")

                Swift.print("---------") // JT 18.12.14
                doAFaceDetection = true // JT 18.11.26
            }
        }

        NotificationCenter.default.addObserver(self, selector: #selector(FaceDetectionLatencyCloud), name: Notification.Name("FaceDetectionLatencyCloud"), object: nil)

        NotificationCenter.default.addObserver(self, selector: #selector(FaceDetectionLatencyEdge), name: Notification.Name("FaceDetectionLatencyEdge"), object: nil)

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "faceRecognitionLatencyEdge"), object: nil, queue: nil) // updateNetworkLatencies
        { [weak self] notification in
            guard let _ = self else { return }

            let v = notification.object as! String

            Swift.print("updateNetworkLatenciesEdge: \(v)")

            DispatchQueue.main.async
            {
                self!.faceRecognitionLatencyEdgeLabel.text = "Edge: \(v) ms" // JT 18.12.11
            }
        } // JT 18.12.13

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "faceRecognitionLatencyCloud"), object: nil, queue: nil) // updateNetworkLatencies
        { [weak self] notification in
            guard let _ = self else { return }

            let v = notification.object as! String

    //        Swift.print("faceRecognitionLatencyCloud: \(v)")  // JT 18.12.20

            DispatchQueue.main.async
            {
                self!.faceRecognitionLatencyCloudLabel.text = "cloud: \(v) ms" // JT 18.12.11

            }
        } // JT 18.12.13

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "updateNetworkLatenciesCloud"), object: nil, queue: nil) // updateNetworkLatencies
        { [weak self] notification in
            guard let _ = self else { return } // canceled

            let v = notification.object as! String // JT 18.11.09

          //  Swift.print("updateNetworkLatenciesCloud")
            //Swift.print("cloud: \(v)")    // JT 18.12.20

            DispatchQueue.main.async
            {
                self!.networkLatencyCloudLabel.text = "Cloud: \(v) ms" // JT 18.12.11
            }
        }

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "updateNetworkLatenciesEdge"), object: nil, queue: nil) // updateNetworkLatencies
        { [weak self] notification in
            guard let _ = self else { return }

            let v = notification.object as! String

            Swift.print("updateNetworkLatenciesEdge")
            Swift.print("edge: \(v)")

            DispatchQueue.main.async
            {
                self!.networkLatencyEdgeLabel.text = "Edge: \(v) ms" // JT 18.12.11
            }
        }

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "faceRecognizedCloud"), object: nil, queue: nil)
        { [weak self] notification in
            guard let _ = self else { return }

            let d = notification.object as! [String: Any] // JT 18.12.13

            Swift.print("faceRecognizedCloud: \(d)")

            let name = d["subject"] as! String // JT 18.12.13
            let a = d["rect"] as! [Int] // JT 18.12.13

            // let r = CGRect(CGFloat(a[0]), CGFloat(a[1]), CGFloat(a[2] - a[0]), CGFloat(a[3] - a[1])) // face rect
            let r = convertPointsToRect(a) // JT 18.12.13

            let r2 = self!.previewView.drawFaceboundingBoxCloud(rect: r, hint: self!.sentImageSize) // JT 18.11.28

            DispatchQueue.main.async
            {
                self!.faceRecognitonNameCloudLabel.text = name
                self!.faceRecognitonNameCloudLabel.isHidden = false

                self!.faceRecognitonNameCloudLabel.center = CGPoint(x: r.midX, y: r.origin.y - 20) // above
            }
        }

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "faceRecognizedEdge"), object: nil, queue: nil)
        { [weak self] notification in
            guard let _ = self else { return }

            let d = notification.object as! [String: Any] // Dictionary/json

            if d["success"] as! String == "true"
            {
                let subject = d["subject"] as! String

                Swift.print("\(notification.name): \(d)")

                // SKToast.show(withMessage: "FaceDetection raw result\(d)")

                // Swift.print("FaceDetection\n\(d)") // JT 18.11.27

                //   let r = CGRect(CGFloat(a[0]), CGFloat(a[1]), CGFloat(a[2] - a[0]), CGFloat(a[3] - a[1])) // face rect
                // let r = convertPointsToRect(a)  // JT 18.12.13

                self!.previewView.removeMaskLayerMex() // JT 18.12.14 erase old

                let a = d["rect"] as! [Int] // JT 18.12.15
                let r = convertPointsToRect(a) // JT 18.12.15

                let r2 = self!.previewView.drawFaceboundingBoxEdge(rect: r, hint: self!.sentImageSize) // JT 18.11.28 green

                DispatchQueue.main.async
                {
                    // Swift.print("subject \(subject)")   // JT 18.12.14
                    self!.faceRecognitonNameEdgeLabel.text = subject
                    self!.faceRecognitonNameEdgeLabel.isHidden = false

                    self!.faceRecognitonNameEdgeLabel.center = CGPoint(x: r2.midX, y: r2.origin.y + r2.size.height + 20) // below
                    // Swift.print("\(r)") // JT 18.12.14
                }
            }
            else
            {
                self!.faceRecognitonNameEdgeLabel.text = "" // JT 18.12.14
                self!.previewView.removeMaskLayerMex() // JT 18.12.14 erase old
            }
        }

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "faceDetectedCloud"), object: nil, queue: nil)
        { [weak self] notification in
            guard let _ = self else { return }

            let d = notification.object as! [String: Any] // Dictionary/json

         //   if d["success"] as! Bool == true
            if d["success"] as! String == "true"    // JT 18.12.20
            {
                let aa = d["rects"] as! [[Int]]
                for a in aa
                {
                    let r = convertPointsToRect(a)
                    
                    self!.previewView.drawFaceboundingBoxCloud(rect: r, hint: self!.sentImageSize)
                    
                    let multiface = UserDefaults.standard.bool(forKey: "Multi-face")
                    if multiface == false
                    {
                        break   // JT 18.12.20 just showed first
                    }
                }
            }
            else
            {}
        }
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "faceDetectedEdge"), object: nil, queue: nil) // JT 18.12.20
        { [weak self] notification in
            guard let _ = self else { return }
            
            let d = notification.object as! [String: Any] // Dictionary/json
            
      //      if  d["success"] as! Bool == true    // JT 18.12.20 API changed
            if d["success"] as! String == "true"
            {
                let aa = d["rects"] as! [[Int]]
                for a in aa
                {
                    let r = convertPointsToRect(a)
                    
                  //  self!.previewView.removeMaskLayerMex() // JT 18.12.14 erase old
                    
                    self!.previewView.drawFaceboundingBoxEdge(rect: r, hint: self!.sentImageSize)
                    
                    let multiface = UserDefaults.standard.bool(forKey: "Multi-face")
                    if multiface == false
                    {
                        break   // JT 18.12.20 just showed first
                    }
                }
            }
            else
            {}
        }
        
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "removeMaskLayerMex"), object: nil, queue: nil) // JT 18.12.20
        { [weak self] notification in
            guard let _ = self else { return }
            
            DispatchQueue.main.async
            {
                self!.previewView.removeMaskLayerMex() //   erase old
            }
        }
        

    }
    

    @objc func FaceDetectionLatencyEdge(_ notification: Notification) // JT 18.12.11
    {
        let v = notification.object
        Swift.print("edge: [\(v!)]")

        let vv = Double(v as! String)      // JT 18.12.20

        DispatchQueue.main.async
        {
            self.latencyEdgeLabel.text = "Edge: \(v!) ms" // JT 18.12.11
            
            self.rollingAverageEdge = self.calcRollingAverageEdge.addSample(value: vv!)
            let useRollingAverage = UserDefaults.standard.bool(forKey: "Use Rolling Average") // JT 18.12.17
            
             if useRollingAverage
            {
                let a = String( format: "%4.3f", self.rollingAverageEdge)      // JT 18.12.17
                
                self.latencyEdgeLabel.text = "Edge: \(a) ms"    // JT 18.12.20
            }
            
            let showStddev = UserDefaults.standard.bool(forKey: "Show Stddev")
            let stddev = standardDeviation(arr: self.calcRollingAverageEdge.samples)
            let stdevStr = String( format: "%4.3f", stddev) // JT 18.12.20

            if showStddev
            {
                self.stddevEdgeLabel.text = "\(stdevStr)"
            }
            else
            {
                self.stddevEdgeLabel.isHidden = true    // JT 18.12.20
            }
            
        }
    }

    @objc func FaceDetectionLatencyCloud(_ notification: Notification)
    {
        let v = notification.object
        Swift.print("Cloud: [\(v!)]")   // JT 18.12.20

        DispatchQueue.main.async
        {
            self.latencyCloudLabel.text = "Cloud: \(v!) ms"

            let vv = Double(v as! String)      // JT 18.12.20
            self.rollingAverageCloud = self.calcRollingAverageCloud.addSample(value: vv!)
            let useRollingAverage = UserDefaults.standard.bool(forKey: "Use Rolling Average") // JT 18.12.17
            
            
            if useRollingAverage
            {
                let a = String( format: "%4.3f", self.rollingAverageCloud)      // JT 18.12.17  // JT 18.12.20
                
                self.latencyCloudLabel.text = "Cloud: \(a) ms"   // JT 18.12.17
            }

            let showStddev = UserDefaults.standard.bool(forKey: "Show Stddev") // JT 18.12.17
            let stddev = standardDeviation(arr: self.calcRollingAverageCloud.samples)   // JT 18.12.18
            let stdevStr = String( format: "%4.3f", stddev) // JT 18.12.20

            
            if showStddev
            {
                self.stddevCloudLabel.text = "\(stdevStr)"
            }
            else
            {
                self.stddevCloudLabel.isHidden = true    // JT 18.12.20
            }
          }
    }

    @objc func networkLatencyEdge(_ notification: Notification) // JT 18.12.11
    {
        let v = notification.object
        Swift.print("networkLatency edge: \(v!)")
    }

    @objc func networkLatencyCloud(_ notification: Notification) // JT 18.12.11
    {
        let v = notification.object
        Swift.print("networkLatency Cloud: \(v!)")
    }

    // MARK: -

    private func removeObservers()
    {
        NotificationCenter.default.removeObserver(self)
    }

    @objc func sessionRuntimeError(_ notification: Notification)
    {
        guard let errorValue = notification.userInfo?[AVCaptureSessionErrorKey] as? NSError else { return }

        let error = AVError(_nsError: errorValue)
        print("Capture session runtime error: \(error)")

        /*
         Automatically try to restart the session running if media services were
         reset and the last start running succeeded. Otherwise, enable the user
         to try to resume the session running.
         */
        if error.code == .mediaServicesWereReset
        {
            sessionQueue.async
            { [unowned self] in
                if self.isSessionRunning
                {
                    self.session.startRunning()
                    self.isSessionRunning = self.session.isRunning
                }
            }
        }
    }

    @objc func sessionWasInterrupted(_ notification: Notification)
    {
        /*
         In some scenarios we want to enable the user to resume the session running.
         For example, if music playback is initiated via control center while
         using AVCamBarcode, then the user can let AVCamBarcode resume
         the session running, which will stop music playback. Note that stopping
         music playback in control center will not automatically resume the session
         running. Also note that it is not always possible to resume, see `resumeInterruptedSession(_:)`.
         */
        if let userInfoValue = notification.userInfo?[AVCaptureSessionInterruptionReasonKey] as AnyObject?, let reasonIntegerValue = userInfoValue.integerValue, let reason = AVCaptureSession.InterruptionReason(rawValue: reasonIntegerValue)
        {
            print("Capture session was interrupted with reason \(reason)")
        }
    }

    @objc func sessionInterruptionEnded(_: Notification)
    {
        print("Capture session interruption ended")
    }
}

extension FaceDetectionViewController
{
    func setupVision()
    {
        requests = [faceDetectionRequest]
    }

    func handleFaces(request: VNRequest, error _: Error?)
    {
        DispatchQueue.main.async
        {
            // perform all the UI updates on the main queue
            guard let results = request.results as? [VNFaceObservation] else { return }
            self.previewView.removeMask()
            for face in results
            {
                self.previewView.drawFaceboundingBox(face: face)
            }
        }
    }

    func handleFaceLandmarks(request: VNRequest, error _: Error?)
    {
        DispatchQueue.main.async
        {
            // perform all the UI updates on the main queue
            guard let results = request.results as? [VNFaceObservation] else { return }
            self.previewView.removeMask()
            for face in results
            {
                self.previewView.drawFaceWithLandmarks(face: face)
            }
        }
    }
}

extension FaceDetectionViewController: AVCaptureVideoDataOutputSampleBufferDelegate // JT 18.11.16
{
    // MARK: - AVCaptureVideoDataOutputSampleBufferDelegate

    func captureOutput(_: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from _: AVCaptureConnection)
    {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer),
            let exifOrientation = CGImagePropertyOrientation(rawValue: exifOrientationFromDeviceOrientation()) else { return }
        var requestOptions: [VNImageOption: Any] = [:]

        if let cameraIntrinsicData = CMGetAttachment(sampleBuffer, key: kCMSampleBufferAttachmentKey_CameraIntrinsicMatrix,
                                                     attachmentModeOut: nil)
        {
            requestOptions = [.cameraIntrinsics: cameraIntrinsicData]
        }

        let imageRequestHandler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: exifOrientation, options: requestOptions)

        if doAFaceDetection
        {
            doAFaceDetection = false
            let image = sampleBuffer.uiImage // JT 18.11.26

            let size = image!.size // JT 18.11.27
            let fudge: CGFloat = 8.0 // JT 18.11.27
            let newSize = CGSize(width: size.width / fudge, height: size.height / fudge) // JT 18.11.27
            let smaller: UIImage? = image?.imageResize(sizeChange: newSize) // JT 18.11.27

            let rotateImage = smaller!.rotate(radians: .pi / 2.0) // JT 18.11.27
            //   Swift.print("\(rotateImage!.size)") // JT 18.11.27

            sentImageSize = rotateImage!.size // JT 18.11.28

//            DispatchQueue.main.async
//                { [unowned self] in
//                 //   self.tmpImageView.image = rotateImage    // JT 18.11.27
//
//            }

//            // Create path.
//            let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
//            if let filePath = paths.first?.appendingPathComponent("MyImageName.png") {
//                // Save image.
//                do {
//                    try smaller!.pngData()?.write(to: filePath, options: .atomic)   // JT 18.11.27 tmp
//                }
//                catch {
//                    // Handle the error
//                }
//            }

            
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "removeMaskLayerMex"), object: nil) // JT 18.12.20
            
            faceDetectionEdge.FaceDetection(rotateImage, "Edge") // JT 18.11.26 edge    // JT 18.12.16
            faceDetectionCloud.FaceDetection(rotateImage, "Cloud") // JT 18.11.26    cloud  OK
        }

        do
        {
            try imageRequestHandler.perform(requests)
        }
        catch
        {
            print(error)
        }
    }
}

extension UIDeviceOrientation
{
    var videoOrientation: AVCaptureVideoOrientation?
    {
        switch self {
        case .portrait: return .portrait
        case .portraitUpsideDown: return .portraitUpsideDown
        case .landscapeLeft: return .landscapeRight
        case .landscapeRight: return .landscapeLeft

        default: return nil
        }
    }
}

extension UIInterfaceOrientation
{
    var videoOrientation: AVCaptureVideoOrientation?
    {
        switch self
        {
        case .portrait: return .portrait
        case .portraitUpsideDown: return .portraitUpsideDown
        case .landscapeLeft: return .landscapeLeft
        case .landscapeRight: return .landscapeRight

        default: return nil
        }
    }
}
