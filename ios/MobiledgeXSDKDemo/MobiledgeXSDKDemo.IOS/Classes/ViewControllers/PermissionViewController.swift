//
//  PermissionViewController.swift
//  MobiledgeXSDKDemo.IOS
//
//  Created by meta30 on 1/15/19.
//  Copyright © 2019 MobiledgeX. All rights reserved.
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
        let alert = UIAlertController(title: "Alert", message: "Why app needs permission", preferredStyle: .alert) // .actionSheet)

        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { _ in
            // execute some code when this option is selected
        }))

        present(alert, animated: true, completion: nil)
    }

    func showWhy2()
    {
        let alert = UIAlertController(title: "Alert", message: "Why app needs permission to verify location- Because", preferredStyle: .alert) // .actionSheet)

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
            with: [.locationWhenInUse],  //.camera, .calendar, .microphone],
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
        case .locationWithBackground:
            return "Location"
        case .mediaLibrary:
            return "Media Library"
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
        case .locationWhenInUse:
            return "App can check your location when in use" // JT 19.01.15
        case .locationWithBackground:
            return "App will can check your location"
        case .mediaLibrary:
            return "Allow check your media"
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
