//
//  SideMenuViewController.swift
//  MatchingEngineSDK Example
//
//  Created by Jean Tantra, Metatheory.com
//
// Copyright 2019 MobiledgeX
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import Foundation
import UIKit

import GoogleSignIn

class SideMenuViewController: FormViewController, GIDSignInUIDelegate
{
    // @IBOutlet var signInButton: GIDSignInButton!

    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Menu"

        GIDSignIn.sharedInstance().uiDelegate = self

        sideMenu() //  eurekaForm
    }

    func sideMenu()
    {
        form +++ // Section("")    // Eureka

            Section()
            //                {
            //                $0.header = HeaderFooterView<LogoView>(.class)
            //            }

            <<< ButtonRow("Sign in with Google")
        {
            $0.title = $0.tag
            $0.cellStyle = .subtitle
            Swift.print(" Sign in with Google") // Log

            $0.onCellSelection(self.signInWithGoogle)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "g-logo")
        }

            <<< ButtonRow("Face Recognition")
        {
            $0.title = $0.tag
            Swift.print("Face Recognition") // Log

            $0.onCellSelection(self.doFaceRecognitionTapped)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ios11-control-center-camera-icon")
        }

            <<< ButtonRow("Face Detection")
        {
            $0.title = $0.tag
            Swift.print("Face Detection") // Log

            $0.onCellSelection(self.doFaceDetectionTapped)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ios11-control-center-camera-icon")
        }

            <<< ButtonRow("Settings")
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName: "Settings", onDismiss: nil)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "Cog_font_awesome.png")
        }

            <<< ButtonRow("About")
        {
            $0.title = $0.tag
            //              $0.presentationMode = .segueName(segueName: "About", onDismiss: nil)

            $0.onCellSelection(self.aboutTapped)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "About")
        }
//        form +++ Section("Benchmark")
//            <<< ButtonRow("Edge")
//        {
//            $0.title = $0.tag
//
//            Swift.print("todo! Edge")
//
//            $0.onCellSelection(self.edgeBenchmarkTapped)
//        }
//        .cellSetup
//        { cell, _ in
//            let im = UIImage(named: "ic_marker_cloudlet-web")
//
//            let tintColorGray = UIColor(red: 0.416, green: 0.14, blue: 0.416, alpha: 0.416)
//
//            let ti = im!.imageWithColor(tintColorGray)
//
//            cell.imageView?.image = ti // UIImage(named: ti)
//        }
//
//            <<< ButtonRow("Local")
//        {
//            $0.title = $0.tag
//
//            Swift.print("todo! Local")  // Log
//        }
//        .cellSetup
//        { cell, _ in
//            let im = UIImage(named: "ic_marker_mobile-web")
//
//            let tintColorGray = UIColor(red: 0.416, green: 0.14, blue: 0.416, alpha: 0.416)
//
//            let ti = im!.imageWithColor(tintColorGray)
//
//            cell.imageView?.image = ti
//        } // JT 19.02.05
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
        print("signInWithGoogle! \(row)") // Log
        signInWithGoogle()
    }

    func signInWithGoogle()
    {
        print("signInWithGoogle") // Log

        GIDSignIn.sharedInstance().signIn()
    }

    func faceRecognitionTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("faceRecognitionTapped! \(row)") // Log
    }

    func faceRecognitionTapped()
    {
        print("faceRecognitionTapped") // Log
        SKToast.show(withMessage: "faceRecognitionTapped - todo") //UI
    }

    func buttonTapped(cell _: ButtonCellOf<String>, row: ButtonRow) // template
    {
        print("tapped! \(row)") // Log
    }

    func doFaceDetectionTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("buttonTapped2! \(row)") // Log

        doFaceDetectionViewController()
    }
  
    func doFaceRecognitionTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        
        let vc = storyboard.instantiateViewController(withIdentifier: "FaceDetectionViewController") // same controller. todo clone
        
        doAFaceRecognition = true
        
        faceDetectCount = OSAtomicInt32(3)  // JT 19.02.05 next this is what starts things

        UserDefaults.standard.set(true, forKey: "doFaceRecognition")
        UserDefaults.standard.synchronize()  // JT 19.02.04

        navigationController!.pushViewController(vc, animated: true)
    }
    

    func doFaceDetectionViewController()
    {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)

        let vc = storyboard.instantiateViewController(withIdentifier: "FaceDetectionViewController")

        doAFaceRecognition = false

        faceDetectCount = OSAtomicInt32(3)  // JT 19.02.05 next this is what starts things

        UserDefaults.standard.set(false, forKey: "doFaceRecognition")
        UserDefaults.standard.synchronize()  // JT 19.02.04
        
        navigationController!.pushViewController(vc, animated: true)
    }

    func aboutTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("aboutTapped! \(row)") // Log
        aboutTapped()
    }

    func aboutTapped()
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
        aboutView.headerBackgroundImage = UIImage(named: "ic_launcher-web.png") //

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

    func edgeBenchmarkTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("edgeBenchmarkTapped! \(row)") // Log
        edgeBenchmarkTapped()
    }

    func edgeBenchmarkTapped()
    {
        print("edgeBenchmarkTapped!") // Log
        SKToast.show(withMessage: "edgeBenchmarkTapped - todo")  // UI
    }

    func localBenchmarkTapped(cell _: ButtonCellOf<String>, row: ButtonRow)
    {
        print("localBenchmarkTapped! \(row)") // Log
        localBenchmarkTapped()
    }

    func localBenchmarkTapped()
    {
        print("localBenchmarkTapped!") // Log
        SKToast.show(withMessage: "localBenchmarkTapped - todo") // UI
    }

    // ----
}
