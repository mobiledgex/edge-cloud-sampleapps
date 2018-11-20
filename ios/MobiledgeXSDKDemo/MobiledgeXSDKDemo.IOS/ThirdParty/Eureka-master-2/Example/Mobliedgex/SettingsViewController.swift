//
//  SettingsViewController.swift
//  Example
//
//  Created by meta30 on 11/3/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Foundation

import UIKit
import Eureka

class SettingsViewController : FormViewController
{
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        title = "Settings"
        
        form +++  Section(){ section in
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
           
            <<< ButtonRow("Enhanced Network Location Services")   // JT 18.11.03
            {
                $0.title = $0.tag
                $0.presentationMode = .segueName(segueName:
                    "EnhancedNetworkLocationServicesViewControllerSegue", onDismiss: nil)   // JT 18.11.03
                $0.value = "ZSE"    // JT 18.11.03
            }
                .cellSetup { cell, row in
                    cell.imageView?.image = UIImage(named: "plus_image")
            }
            
            <<< ButtonRow("General Settings")   // JT 18.11.03
            {
                $0.title = $0.tag
                $0.presentationMode = .segueName(segueName:
                    "GeneralSettingsViewControllerSegue", onDismiss: nil)   // JT 18.11.03
        }
                .cellSetup { cell, row in
                    cell.imageView?.image = UIImage(named: "plus_image")
            }
            
            <<< ButtonRow("Speed Test Settings")   // JT 18.11.03
            {
                $0.title = $0.tag
                $0.presentationMode = .segueName(segueName:
                    "SpeedTestSettingsViewControllerSegue", onDismiss: nil)   // JT 18.11.03
        }
                .cellSetup { cell, row in
                    cell.imageView?.image = UIImage(named: "plus_image")
            }
            
            <<< ButtonRow("Face Detection Settings")   // JT 18.11.03
            {
                $0.title = $0.tag
                $0.presentationMode = .segueName(segueName:
                    "FaceDectectionSettingsViewControllerSegue", onDismiss: nil)   // JT 18.11.03
        }
                .cellSetup { cell, row in
                    cell.imageView?.image = UIImage(named: "plus_image")
        }
        
        title = "Settings"

    }
    
    class  LogoView: UIView {
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            let imageView = UIImageView(image: UIImage(named: "ic_launcher-web"))
            imageView.frame = CGRect(x: 0, y: 0, width: 320, height: 130)
            imageView.autoresizingMask = .flexibleWidth
            self.frame = CGRect(x: 0, y: 0, width: 320, height: 130)
            imageView.contentMode = .scaleAspectFit
            addSubview(imageView)
        }
        
        required init?(coder aDecoder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        
    }
}


