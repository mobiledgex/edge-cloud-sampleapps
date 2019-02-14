//
//  PermissionViewController.swift
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
import SPPermission

class PermissionViewController: UIViewController
{
    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Permissions" // JT 19.01.15
    }

    @IBAction func why1Action(_: Any)
    {
        Swift.print("why1Action")

        showWhy1()
    }

    @IBAction func why2Action(_: Any)
    {
        Swift.print("why2Action")

        showWhy2()
    }


    func showWhy1()
    {
        let alert = UIAlertController(title: "Alert", message: "App needs your loation to verify it.", preferredStyle: .alert) // .actionSheet) // JT 19.02.10

        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { _ in
            // execute some code when this option is selected
        }))

        present(alert, animated: true, completion: nil)
    }

    func showWhy2()
    {
        let alert = UIAlertController(title: "Alert", message: "Why app needs permission is to verify location.", preferredStyle: .alert) // .actionSheet)

        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { _ in
            // execute some code when this option is selected
        }))

        present(alert, animated: true, completion: nil)
    }
    
    @IBAction func okAction(_: Any)
    {
        Swift.print("okAction")
        presentPermissonDialog()    // JT 19.01.16
    }
    @objc func presentPermissonDialog()
    {
    //    SPAnimationAlpha.hideList(views: [self.presentButton, self.changeBackgroundButton])
        
        SPPermission.Dialog.request(
            with: [.locationAlways], //[.locationWhenInUse],  //.camera, .calendar, .microphone],    // JT 19.01.29
            on: self,
            delegate: self,
            dataSource: self    // JT 19.01.16
            //,colorSource: self
        )
    }

    
}

extension PermissionViewController: SPPermissionDialogDelegate
{ 
    func didHide()
    {
        print("SPPermissionDialogDelegate - didHide")
    }

    func didAllow(permission _: SPPermissionType)
    {
        //  print("SPPermissionDialogDelegate - didAllow \(permission.name)")
        Swift.print("didAllow")
        
        UserDefaults.standard.set( true, forKey: "firstTimeUsagePermission")    // JT 18.12.17

        let _ = navigationController?.popViewController(animated: true)   // JT 19.01.16
        

        NotificationCenter.default.post(name: NSNotification.Name(rawValue: "permissionGrantedGetLocaltionUpdates"), object: nil)   // JT 19.02.10

    }

    func didDenied(permission _: SPPermissionType)
    {
        //  print("SPPermissionDialogDelegate - didDenied \(permission.name)")
    }
}

// extension PermissionViewController: SPPermissionDialogColorSource {
//
//    /*
//     For customize colors ovveride more parametrs.
//     All parametrs not requerid.
//     For example I am ovveride two parametrs.
//     */
//
//    var whiteColor: UIColor {
//        return UIColor.white
//    }
//
//    var blackColor: UIColor {
//        return UIColor.black
//    }
// }
//

extension PermissionViewController: SPPermissionDialogDataSource
{
    var dragToDismiss: Bool
    {
        return true
    }

    var showCloseButton: Bool
    {
        return true
    }

    var allowTitle: String
    {
        return "Allow"
    }

    var allowedTitle: String
    {
        return "Allowed"
    }

    var dialogTitle: String
    {
        return "Need Permissions"
    }

    var dialogSubtitle: String
    {
        return "Permissions Request"
    }

    var dialogComment: String
    {
        return "Permissions are necessary for the correct work of the application and the performance of all functions. Push are not required permissions"
    }

    func name(for permission: SPPermissionType) -> String?
    {
        switch permission {
        case .camera:
            return "Camera"
        case .photoLibrary:
            return "Photo Library"
        case .notification:
            return "Notification"
        case .microphone:
            return "Microphone"
        case .calendar:
            return "Calendar"
        case .contacts:
            return "Contacts"
        case .reminders:
            return "Reminders"
        case .speech:
            return "Speech"
        case .locationAlways:
            return "Location"
        case .locationWhenInUse:
            return "Location"
                // JT 19.01.29
//        case .locationWithBackground:
//            return "Location"
            
        case .mediaLibrary:
            return "Media Library"
            
        default:
            return "BAD enum"   // JT 19.01.29

        }
    }

    func description(for permission: SPPermissionType) -> String?
    {
        switch permission {
        case .camera:
            return "Allow app for use camera"
        case .calendar:
            return "Application can add events to calendar"
        case .contacts:
            return "Access for your contacts and phones"
        case .microphone:
            return "Allow record voice from app"
        case .notification:
            return "Get important information without opening app."
        case .photoLibrary:
            return "Access for save photos in your gallery"
        case .reminders:
            return "Application can create new task"
        case .speech:
            return "Allow check you voice"
        case .locationAlways:
            return "App will can check your location"
//        case .locationWhenInUse:    // JT 19.01.29
//            return "App can check your location when in use" // JT 19.01.15   // JT 19.01.29 ???
            
//        case .locationWithBackground:
//            return "App will can check your location" // JT 19.01.29
        case .mediaLibrary:
            return "Allow check your media"
            
        default:
            return "?? not found"   // JT 19.01.29

        }
    }

    func image(for _: SPPermissionType) -> UIImage?
    {
        return nil // default icon
    }

    func deniedTitle(for _: SPPermissionType) -> String?
    {
        return "Permission denied"
    }

    func deniedSubtitle(for _: SPPermissionType) -> String?
    {
        return "Please, go to Settings and allow permissions"
    }

    var cancelTitle: String
    {
        return "Cancel"
    }

    var settingsTitle: String
    {
        return "Settings"
    }
}
