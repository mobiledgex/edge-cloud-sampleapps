//
//  ViewController.swift
//  MobiledgeXSDKDemo.IOS
//
//  Created by meta30 on 10/23/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import GoogleMaps   // JT 18.10.23
import SideMenu     // JT 18.11.12  todo
import UIKit

var theMap: GMSMapView? // JT 18.11.15 used by sample.client

class ViewController: UIViewController, GMSMapViewDelegate, UIAdaptivePresentationControllerDelegate // JT 18.10.25
{
    @IBOutlet var viewMap: GMSMapView!

    let rightBarDropDown = DropDown() // JT 18.10.25   // JT 18.11.12 todotodo

    private var locationVerified: Bool = false // JT 18.11.14 todo
    private var locationVerificationAttempted: Bool = false

    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "MobiledgeX SDK Demo" // JT 18.10.25

        // Do any additional setup after loading the view, typically from a nib.

        // -----    // JT 18.09.22

        let leftButton: UIButton = UIButton(type: UIButton.ButtonType.custom) as UIButton
        leftButton.frame = CGRect(x: 0, y: 0, width: 40, height: 40) // JT 18.05.31
        leftButton.setImage(UIImage(named: "menu-512"), for: UIControl.State.normal)
        leftButton.addTarget(self, action: #selector(menuButtonAction), for: UIControl.Event.touchUpInside)

        let leftBarButtonItem: UIBarButtonItem = UIBarButtonItem(customView: leftButton)

        navigationItem.leftBarButtonItem = leftBarButtonItem
        // -----
        //    setSideBarDefaults()    // JT 18.09.22

        // -----

        theMap = viewMap // JT 18.11.11 publish
        theMap!.delegate = self // JT 18.11.12 for taps
        theMap!.isMyLocationEnabled = true // JT 18.11.14 blue dot

        let camera: GMSCameraPosition = GMSCameraPosition.camera(withLatitude: 48.857165, longitude: 2.354613, zoom: 8.0)

        viewMap.camera = camera

        setupRightBarDropDown() // JT 18.10.25   // JT 18.11.12
        // JT 18.11.10
//        let manager = NetworkReachabilityManager(host: "www.apple.com") // JT 18.11.08
//
//        manager?.listener = { status in
//            print("Network Status Changed: \(status)")
//        }
//
//        manager?.startListening()
    }

    fileprivate func setSideBarDefaults()
    {
        let modes: [SideMenuManager.MenuPresentMode] = [.menuSlideIn, .viewSlideOut, .menuDissolveIn]
        //       presentModeSegmentedControl.selectedSegmentIndex = modes.index(of: SideMenuManager.default.menuPresentMode)!
        SideMenuManager.default.menuPresentMode = modes[1] // JT 18.06.06
        let styles: [UIBlurEffect.Style] = [.dark, .light, .extraLight]
    }

    func setupRightBarDropDown()
    {
        let image = UIImage(named: "dot-menu@3x copy")?.withRenderingMode(.alwaysOriginal)
        let barButtonItem = UIBarButtonItem(image: image, style: .plain, target: self, action: #selector(ViewController.openMenu(sender:)))

        navigationItem.rightBarButtonItem = barButtonItem

        rightBarDropDown.anchorView = barButtonItem

        rightBarDropDown.dataSource = [
            "Register Client",
            "Get App Instances",
            "Verify Location",
            "Find Closet Cloudlet",
            "Reset Location",
        ]
    }

 

    //     // MARK: - GMUMapViewDelegate

    // show more place info when info marker is tapped
    func mapView(_: GMSMapView, didTapInfoWindowOf marker: GMSMarker)
    {
        if marker.userData == nil // JT 18.11.14
        {
            return
        }

        let cloudletName = marker.userData as! String // JT 18.11.12

        let lets = CloudletListHolder.getSingleton().getCloudletList() // JT 18.11.12

        if lets[cloudletName] != nil
        {
            let cl = lets[cloudletName]
            Swift.print("\(cloudletName)")
            Swift.print("\(cl)")

            Swift.print("didTapInfoWindowOf \(cloudletName)")

            let storyboard = UIStoryboard(name: "Main", bundle: nil)

            let vc = storyboard.instantiateViewController(withIdentifier: "CloudletDetailsViewController") as! CloudletDetailsViewController // JT 18.11.13
            // pass in data - decoupled
            UserDefaults.standard.set(cl!.getCloudletName() + " : " + cl!.getUri(), forKey: "Cloudlet Name:") // JT 18.11.13
            UserDefaults.standard.set(cl!.getAppName(), forKey: "App Name:")
            UserDefaults.standard.set(cl!.getCarrierName(), forKey: "Carrier:")

            UserDefaults.standard.set(cl!.getLatitude(), forKey: "Latitude:")
            UserDefaults.standard.set(cl!.getLongitude(), forKey: "Longitude:")
            UserDefaults.standard.set(cl!.getDistance(), forKey: "Distance:")

            UserDefaults.standard.set(cl!.getLatencyMin(), forKey: "Latency Min:")
            UserDefaults.standard.set(cl!.getLatencyAvg(), forKey: "Latency Avg:")
            UserDefaults.standard.set(cl!.getLatencyMax(), forKey: "Latency Max:")
            UserDefaults.standard.set(cl!.getLatencyStddev(), forKey: "Latency Stddev:")
            // UserDefaults.standard.set( cl , forKey: "currentDetailsCloudlet")   // JT 18.11.13

            navigationController!.pushViewController(vc, animated: true) // JT 18.11.12
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
//            "Find Closet Cloudlet",
//            "Reset Location",
            
            switch index
            {
            case 0:
                registerClientNow() // JT 18.11.11

            case 1:
                getAppInstNow() // JT 18.11.12

            case 2:
                Swift.print("Verify Location")

                doVerifyLocation() // JT 18.11.14
                
            case 3:
                Swift.print("Find Closet Cloudlet")
                findNearestCloudlet() // JT 18.11.14
                
            case 4:
                Swift.print("Reset Location")

                resetUserLocation(false) // JT 18.11.14 Note: Locator.currentPositionnot working

            default:
                break
            }
        }
    }

    @objc func menuButtonAction() // JT 18.11.12
    {
        print("menuButtonAction")

        if presentingViewController == nil
        {
//            present(SideMenuManager.default.menuLeftNavigationController!, animated: true, completion: nil)   // JT 18.11.12 todo

            let storyboard = UIStoryboard(name: "Main", bundle: nil)

            let vc = storyboard.instantiateViewController(withIdentifier: "SideMenuViewController")

            navigationController!.pushViewController(vc, animated: true) // JT 18.11.12
        }
        else
        {
            dismiss(animated: true, completion: nil)
        }
    }

    // MARK: -

//    public func onMarkerClick(_ marker: GMSMarker) -> Bool
//    {
//        Swift.print("onMarkerClick( \(marker) Draggable= \(marker.isDraggable)")
//        // marker.showInfoWindow();
//        Swift.print("todo showInfoWindow") // JT 18.11.01
//        return true
//    }
//
//    public func onMarkerDragEnd(_ marker: GMSMarker)
//    {
//        Swift.print("onMarkerDragEnd( \(marker))")
//        showSpoofGpsDialog(marker.position)
//    } // JT 18.11.18

    // MARK: -

    private func showSpoofGpsDialog(_ spoofLatLng: CLLocationCoordinate2D) // LatLng)   // JT 18.11.01
    {
        if userMarker == nil
        {
            return
        }
        let oldLatLng = userMarker!.position

        userMarker!.position = spoofLatLng // JT 18.11.01 mUserLocationMarker
        let choices: [String] = ["Spoof GPS at this location", "Update location in GPS database"]

        let alert = UIAlertController(title: " ", message: "Choose", preferredStyle: .alert) // .actionSheet)

        alert.addAction(UIAlertAction(title: choices[0], style: .default, handler: { _ in
            // execute some code when this option is selected

            SKToast.show(withMessage: "GPS spoof enabled.") // JT 18.11.02

            self.locationVerificationAttempted = false
            self.locationVerified = false

            let distance =
                oldLatLng.distance(from: spoofLatLng) / 1000

            userMarker!.snippet = "Spoofed \(String(format: "%.2f", distance)) km from actual location"

        }))
        alert.addAction(UIAlertAction(title: choices[1], style: .default, handler: { _ in

            updateLocSimLocation(userMarker!.position.latitude, userMarker!.position.longitude) // JT 18.11.15

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
