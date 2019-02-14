//
//  SettingsViewController.swift
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

            <<< ButtonRow("Enhanced Network Location Services")
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName:
                "EnhancedNetworkLocationServicesViewControllerSegue", onDismiss: nil)

        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ic_menu_compass")
        }

            <<< ButtonRow("General Settings")
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName:
                "GeneralSettingsViewControllerSegue", onDismiss: nil)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ic_menu_preferences")
        }

            <<< ButtonRow("Speed Test Settings")
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName:
                "SpeedTestSettingsViewControllerSegue", onDismiss: nil)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ic_menu_recent_history")
        }

            <<< ButtonRow("Face Detection Settings") // JT 18.11.03
        {
            $0.title = $0.tag
            $0.presentationMode = .segueName(segueName:
                "FaceDectectionSettingsViewControllerSegue", onDismiss: nil)
        }
        .cellSetup
        { cell, _ in
            cell.imageView?.image = UIImage(named: "ic_menu_camera")  
        }

        title = "Settings"
    }
}
