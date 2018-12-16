//
//  SideMenuViewController.swift
//  Example
//
//  Created by meta30 on 11/4/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Foundation
import UIKit

import GoogleSignIn // JT 18.11.17
// import RFAboutView_Swift // JT 18.11.17 uses

class SideMenuViewController: FormViewController, GIDSignInUIDelegate // JT 18.11.17
{
    // @IBOutlet var signInButton: GIDSignInButton!

    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Menu"

        GIDSignIn.sharedInstance().uiDelegate = self // JT 18.11.17

        sideMenu() //  eurekaForm
    }

    func sideMenu()
    {
        form +++ // Section("")    // Eureka

            Section()
            //                {
            //                $0.header = HeaderFooterView<LogoView>(.class)
            //            }

            <<< ButtonRow("Sign in with Google") // JT 18.11.03
        {
            $0.title = $0.tag
            $0.cellStyle = .subtitle // JT 18.11.04
            Swift.print(" Sign in with Google") // JT 18.11.12

            $0.onCellSelection(self.signInWithGoogle) // JT 18.11.17
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "g-logo")
        }

            <<< ButtonRow("Face Recognition") // JT 18.11.03
        {
            $0.title = $0.tag
            Swift.print("Face Recognition")

            //               $0.presentationMode = .segueName(segueName:
            //                    "Face Recognition", onDismiss: nil)   // JT 18.11.03

            $0.onCellSelection(self.doFaceRecognitionTapped)  // JT 18.12.13
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ios11-control-center-camera-icon")
        }

            <<< ButtonRow("Face Detection") // JT 18.11.03
        {
            $0.title = $0.tag
            Swift.print("Face Detection")
            //         $0.presentationMode = .segueName(segueName:
            //             "Face Detection", onDismiss: nil)   // JT 18.11.03
            Swift.print("todo! Face Detection") // JT 18.11.12

            $0.onCellSelection(self.doFaceDetectionTapped)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ios11-control-center-camera-icon")
        }

            <<< ButtonRow("Settings") // JT 18.11.03
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName: "Settings", onDismiss: nil)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "Cog_font_awesome.png")
        }

            <<< ButtonRow("About") // JT 18.11.03
        {
            $0.title = $0.tag
            //              $0.presentationMode = .segueName(segueName: "About", onDismiss: nil)

            $0.onCellSelection(self.aboutTapped)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "About")
        }
        form +++ Section("Benchmark")
            <<< ButtonRow("Edge") // JT 18.11.03
        {
            $0.title = $0.tag

            Swift.print("todo! Edge") // JT 18.11.12

            $0.onCellSelection(self.edgeBenchmarkTapped) // JT 18.11.17
        }
        .cellSetup
        { cell, _ in
            let im = UIImage(named: "ic_marker_cloudlet-web")

            let tintColorGray = UIColor(red: 0.416, green: 0.14, blue: 0.416, alpha: 0.416)

            let ti = im!.imageWithColor(tintColorGray)

            cell.imageView?.image = ti // UIImage(named: ti)
        }

            <<< ButtonRow("Local") // JT 18.11.03
        {
            $0.title = $0.tag

            Swift.print("todo! Local") // JT 18.11.12
        }
        .cellSetup
        { cell, _ in
            let im = UIImage(named: "ic_marker_mobile-web")

            let tintColorGray = UIColor(red: 0.416, green: 0.14, blue: 0.416, alpha: 0.416)

            let ti = im!.imageWithColor(tintColorGray)

            cell.imageView?.image = ti
        }
    }

    class LogoView: UIView
    {
        override init(frame: CGRect)
        {
            super.init(frame: frame)
            let imageView = UIImageView(image: UIImage(named: "ic_launcher-web"))
            imageView.frame = CGRect(x: 0, y: 0, width: 320, height: 130)
            imageView.autoresizingMask = .flexibleWidth
            self.frame = CGRect(x: 0, y: 0, width: 320, height: 130)
            imageView.contentMode = .scaleAspectFit
            addSubview(imageView)
        }

        required init?(coder _: NSCoder)
        {
            fatalError("init(coder:) has not been implemented")
        }
    }

    // MARK: -

    // Menu item actions

    // pairs 1) UI, 2) programmatic
    //

    func signInWithGoogle(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("signInWithGoogle! \(row)") // JT 18.11.13
        signInWithGoogle()
    }

    func signInWithGoogle()
    {
        print("signInWithGoogle") // JT 18.11.13

        GIDSignIn.sharedInstance().signIn()
    }

    func faceRecognitionTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("faceRecognitionTapped! \(row)") // JT 18.11.13
    }

    func faceRecognitionTapped()
    {
        print("faceRecognitionTapped") // JT 18.11.13
        SKToast.show(withMessage: "faceRecognitionTapped - todo") // JT 18.11.02
    }

    func buttonTapped(cell _: ButtonCellOf<String>, row: ButtonRow) // JT 18.11.17 template
    {
        print("tapped! \(row)") // JT 18.11.13
    }

    func doFaceDetectionTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("buttonTapped2! \(row)") // JT 18.11.13

        doFaceDetectionViewController() // JT 18.11.17
    }
  
    func doFaceRecognitionTapped(cell _: ButtonCellOf<String>, row: ButtonRow)        // JT 18.12.13
    {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        
        let vc = storyboard.instantiateViewController(withIdentifier: "FaceDetectionViewController") // same controller. todo clone
        
 doAFaceRecognition = true
        UserDefaults.standard.set(true, forKey: "doFaceRecognition")    // JT 18.12.13
        
        navigationController!.pushViewController(vc, animated: true) // JT 18.11.16
    }
    

    func doFaceDetectionViewController() // JT 18.11.17
    {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)

        let vc = storyboard.instantiateViewController(withIdentifier: "FaceDetectionViewController")

        doAFaceRecognition = false

        UserDefaults.standard.set(false, forKey: "doFaceRecognition")    // JT 18.12.13

        navigationController!.pushViewController(vc, animated: true) // JT 18.11.16
    }

    func aboutTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("aboutTapped! \(row)") // JT 18.11.13
        aboutTapped() // JT 18.11.17
    }

    func aboutTapped() // JT 18.11.17
    {
        // First create a UINavigationController (or use your existing one).
        // The RFAboutView needs to be wrapped in a UINavigationController.

        let aboutNav = UINavigationController()

        // Initialise the RFAboutView:

        let aboutView = RFAboutViewController(copyrightHolderName: "MobiledgeX",
                                              contactEmail: "mail@example.com",
                                              contactEmailTitle: "Contact us", websiteURL: NSURL(string: "http://MobiledgeX.com") as! URL, websiteURLTitle: "Our Website")

        // Set some additional options:

        aboutView.headerBackgroundColor = .black
        aboutView.headerTextColor = .white
        aboutView.blurStyle = .dark
        aboutView.headerBackgroundImage = UIImage(named: "ic_launcher-web.png") // JT 18.11.17

        // Add an additional button:
        //   aboutView.addAdditionalButton("Privacy Policy", content: "Here's the privacy policy")

        // Add an acknowledgement:
//        aboutView.addAcknowledgement("An awesome library", content: "License information for the awesome library")

        aboutView.addAdditionalButton("Credits", content: "Coded by: Jean Tantra, www.Metatheory.com\nContact: JeanTantra@Metatheory.com  510.872.4476")

        // Add the aboutView to the NavigationController:
        aboutNav.setViewControllers([aboutView], animated: false)

        // Present the navigation controller:
        present(aboutNav, animated: true, completion: nil)
    }

    // ====

    func edgeBenchmarkTapped(cell _: ButtonCellOf<String>, row: ButtonRow) // JT 18.11.17 template
    {
        print("edgeBenchmarkTapped! \(row)") // JT 18.11.13
        edgeBenchmarkTapped()
    }

    func edgeBenchmarkTapped()
    {
        print("edgeBenchmarkTapped!") // JT 18.11.13
        SKToast.show(withMessage: "edgeBenchmarkTapped - todo") // JT 18.11.02
    }

    func localBenchmarkTapped(cell _: ButtonCellOf<String>, row: ButtonRow) // JT 18.11.17 template
    {
        print("localBenchmarkTapped! \(row)") // JT 18.11.13
        localBenchmarkTapped()
    }

    func localBenchmarkTapped()
    {
        print("localBenchmarkTapped!") // JT 18.11.13
        SKToast.show(withMessage: "localBenchmarkTapped - todo") // JT 18.11.02
    }

    // ----
}
