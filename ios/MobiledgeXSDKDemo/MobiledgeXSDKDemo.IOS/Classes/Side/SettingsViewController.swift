//
//  SettingsViewController.swift
//  Example
//
//  Created by meta30 on 11/3/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Foundation

import UIKit
// import Eureka // JT 18.11.12

class SettingsViewController: FormViewController
{
    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Settings"

        form +++ Section
        { section in
            section.header = {
                var header = HeaderFooterView<UIView>(.callback({
                    let view = UIView(frame: CGRect(x: 0, y: 0, width: 100, height: 3))
                    view.backgroundColor = .white
                    return view
                }))
                header.height = { 3 }
                return header
            }()
        }

            <<< ButtonRow("Enhanced Network Location Services") // JT 18.11.03
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName:
                "EnhancedNetworkLocationServicesViewControllerSegue", onDismiss: nil) // JT 18.11.03
            // $0.value = "ZSE"    // JT 18.11.03
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ic_menu_compass") // JT 18.11.14
        }

            <<< ButtonRow("General Settings") // JT 18.11.03
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName:
                "GeneralSettingsViewControllerSegue", onDismiss: nil) // JT 18.11.03
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ic_menu_preferences")
        }

            <<< ButtonRow("Speed Test Settings") // JT 18.11.03
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName:
                "SpeedTestSettingsViewControllerSegue", onDismiss: nil) // JT 18.11.03
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ic_menu_recent_history") // JT 18.11.14
        }

            <<< ButtonRow("Face Detection Settings") // JT 18.11.03
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName:
                "FaceDectectionSettingsViewControllerSegue", onDismiss: nil) // JT 18.11.03
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ic_menu_camera") // JT 18.11.14
        }

        title = "Settings"
    }
}
