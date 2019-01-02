//
//  ViewController.swift
//  MobiledgeXSDKDemo.IOS
//
//  Created by meta30 on 10/23/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import GoogleMaps
import UIKit

var theMap: GMSMapView? //   used by sample.client
var userMarker: GMSMarker?   // set by RegisterClient , was: mUserLocationMarker.

class ViewController: UIViewController, GMSMapViewDelegate, UIAdaptivePresentationControllerDelegate
{
    @IBOutlet var viewMap: GMSMapView!

    let rightBarDropDown = DropDown()

    private var locationVerified: Bool = false //  todo where to set this true?
    private var locationVerificationAttempted: Bool = false

    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "MobiledgeX SDK Demo"

        // Do any additional setup after loading the view, typically from a nib.

        // -----

        let leftButton: UIButton = UIButton(type: UIButton.ButtonType.custom) as UIButton
        leftButton.frame = CGRect(x: 0, y: 0, width: 40, height: 40)
        leftButton.setImage(UIImage(named: "menu-512"), for: UIControl.State.normal)
        leftButton.addTarget(self, action: #selector(menuButtonAction), for: UIControl.Event.touchUpInside)

        let leftBarButtonItem: UIBarButtonItem = UIBarButtonItem(customView: leftButton)

        navigationItem.leftBarButtonItem = leftBarButtonItem
          // -----

        theMap = viewMap //   publish
        theMap!.delegate = self //  for taps
        theMap!.isMyLocationEnabled = true //   blue dot

        let camera: GMSCameraPosition = GMSCameraPosition.camera(withLatitude: 48.857165, longitude: 2.354613, zoom: 8.0)

        viewMap.camera = camera

        setupRightBarDropDown()

        //        let manager = NetworkReachabilityManager(host: "www.apple.com")
//
//        manager?.listener = { status in
//            print("Network Status Changed: \(status)")
//        }
//
//        manager?.startListening()
        
        MexRegisterClient.shared.registerClientThenGetInstApps()
        
        // latency
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "latencyCloud"), object: nil, queue: nil) // updateNetworkLatencies
        { [weak self] notification in
            guard let _ = self else { return }
            
            let v = notification.object as! String
            UserDefaults.standard.set( v, forKey: "latencyCloud")
        }
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "latencyEdge"), object: nil, queue: nil) // updateNetworkLatencies
        { [weak self] notification in
            guard let _ = self else { return }
            
            let v = notification.object as! String
            UserDefaults.standard.set( v, forKey: "latencyEdge")
        }
        
        
      //  getNetworkLatencyCloud()    //   "latencyCloud"

        DispatchQueue.main.async {
            getNetworkLatencyEdge() //   "latencyEdge"
        }
    }

    func setupRightBarDropDown()
    {
        let image = UIImage(named: "dot-menu@3x copy")?.withRenderingMode(.alwaysOriginal)
        let barButtonItem = UIBarButtonItem(image: image, style: .plain, target: self, action: #selector(ViewController.openMenu(sender:))) //  todo rename image

        navigationItem.rightBarButtonItem = barButtonItem

        rightBarDropDown.anchorView = barButtonItem

        rightBarDropDown.dataSource = [ // these first two are automatically done on launch
            "Register Client",
            "Get App Instances",
            "Verify Location",
            "Find Closet Cloudlet",
            "Reset Location",
        ]
    }

 

    // MARK: - GMUMapViewDelegate

    // show more place info when info marker is tapped
    func mapView(_: GMSMapView, didTapInfoWindowOf marker: GMSMarker)
    {
        if marker.userData == nil
        {
            return
        }

        let cloudletName = marker.userData as! String

        let lets = CloudletListHolder.getSingleton().getCloudletList()

        if lets[cloudletName] != nil
        {
            let cl = lets[cloudletName]
            Swift.print("\(cloudletName)")
            Swift.print("\(cl)")

            Swift.print("didTapInfoWindowOf \(cloudletName)")

            let storyboard = UIStoryboard(name: "Main", bundle: nil)

            let vc = storyboard.instantiateViewController(withIdentifier: "CloudletDetailsViewController") as! CloudletDetailsViewController
            
            // pass in data - decoupled
            UserDefaults.standard.set(cl!.getCloudletName() + " : " + cl!.getUri(), forKey: "Cloudlet Name:")
            UserDefaults.standard.set(cl!.getAppName(), forKey: "App Name:")
            UserDefaults.standard.set(cl!.getCarrierName(), forKey: "Carrier:")

            UserDefaults.standard.set(cl!.getLatitude(), forKey: "Latitude:")
            UserDefaults.standard.set(cl!.getLongitude(), forKey: "Longitude:")
            UserDefaults.standard.set(cl!.getDistance(), forKey: "Distance:")

            UserDefaults.standard.set(cl!.getLatencyMin(), forKey: "Latency Min:")
            UserDefaults.standard.set(cl!.getLatencyAvg(), forKey: "Latency Avg:")
            UserDefaults.standard.set(cl!.getLatencyMax(), forKey: "Latency Max:")
            UserDefaults.standard.set(cl!.getLatencyStddev(), forKey: "Latency Stddev:")
            // UserDefaults.standard.set( cl , forKey: "currentDetailsCloudlet")

            navigationController!.pushViewController(vc, animated: true)
            
            vc.cloudlet = cl!
        }
    }

    // hide info or search when map is tapped
    func mapView(_: GMSMapView, didTapAt coordinate: CLLocationCoordinate2D)
    {
        Swift.print("didTapAt \(coordinate)")
    }

    func mapView(_: GMSMapView, didLongPressAt coordinate: CLLocationCoordinate2D)
    {
        Swift.print("onMapLongClick(\(coordinate))")
        showSpoofGpsDialog(coordinate)
    }

    // Mark: -

    @objc public func openMenu(sender _: UIBarButtonItem)
    {
        Swift.print("openMenu")

        rightBarDropDown.show()

        // Action triggered on selection
        rightBarDropDown.selectionAction = { [weak self] index, item in
            Swift.print("selectionAction \(index) \(item) ")

//            "Register Client",
//            "Get App Instances",
//            "Verify Location",
//            "Find Closest Cloudlet",
//            "Reset Location",
            
            switch index
            {
            case 0:
                MexRegisterClient.shared.registerClientNow()    //  "Register Client",

            case 1:
                MexGetAppInst.shared.getAppInstNow()    // "Get App Instances"

            case 2:
                Swift.print("Verify Location")

                MexVerifyLocation.shared.doVerifyLocation()     // "Verify Location"
                
            case 3:
                Swift.print("Find Closest Cloudlet")
                MexFindNearestCloudlet.shared.findNearestCloudlet()     //  "Find Closest Cloudlet"
                
            case 4:
                Swift.print("Reset Location")

                resetUserLocation(false) // "Reset Location" Note: Locator.currentPositionnot working

            default:
                break
            }
        }
    }

    @objc func menuButtonAction()
    {
        print("menuButtonAction")

        if presentingViewController == nil
        {

            let storyboard = UIStoryboard(name: "Main", bundle: nil)

            let vc = storyboard.instantiateViewController(withIdentifier: "SideMenuViewController")

            navigationController!.pushViewController(vc, animated: true)
        }
        else
        {
            dismiss(animated: true, completion: nil)
        }
    }

  
    // MARK: -

    private func showSpoofGpsDialog(_ spoofLatLng: CLLocationCoordinate2D) // LatLng)
    {
        if userMarker == nil
        {
            return
        }
        let oldLatLng = userMarker!.position

        userMarker!.position = spoofLatLng //   mUserLocationMarker
        let choices: [String] = ["Spoof GPS at this location", "Update location in GPS database"]

        let alert = UIAlertController(title: " ", message: "Choose", preferredStyle: .alert) // .actionSheet)

        alert.addAction(UIAlertAction(title: choices[0], style: .default, handler: { _ in
            // execute some code when this option is selected

            SKToast.show(withMessage: "GPS spoof enabled.")

            self.locationVerificationAttempted = false
            self.locationVerified = false

            let distance =
                oldLatLng.distance(from: spoofLatLng) / 1000

            userMarker!.snippet = "Spoofed \(String(format: "%.2f", distance)) km from actual location"
            
            let resized =  makeUserMakerImage(MexRegisterClient.COLOR_NEUTRAL)
            
            userMarker!.icon = resized

        }))
        alert.addAction(UIAlertAction(title: choices[1], style: .default, handler: { _ in

            updateLocSimLocation(userMarker!.position.latitude, userMarker!.position.longitude)
            
            let resized =  makeUserMakerImage(MexRegisterClient.COLOR_NEUTRAL)
            
            userMarker!.icon = resized

        }))

        present(alert, animated: true, completion: nil)
    }
}

extension CLLocationCoordinate2D
{
    // distance in meters, as explained in CLLoactionDistance definition
    func distance(from: CLLocationCoordinate2D) -> CLLocationDistance
    {
        let destination = CLLocation(latitude: from.latitude, longitude: from.longitude)
        return CLLocation(latitude: latitude, longitude: longitude).distance(from: destination)
    }
}
