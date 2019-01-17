//
//  GeneralSettingsViewController.swift
//  Example
//
//  Created by meta30 on 11/3/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Foundation

import UIKit

class GeneralSettingsViewController: FormViewController
{
    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "General Settings"

        form +++ Section("")    // Eureka

            <<< AlertRow<String>() // ActionSheetRow
        {
            $0.title = "DME Selection"
            $0.selectorTitle = "DME Selection"
            $0.options = ["Demo", "TDG Integraton"]
            $0.value = UserDefaults.standard.string(forKey: "DME Selection") ?? "Demo" // initially selected
        }.onChange
        { /* [weak self] */ row in
            Swift.print("DME Selection" + "\(row) \(row.value!)")

            UserDefaults.standard.set(row.value, forKey: "DME Selection")
//  // JT 19.01.15
//            Demo (mexdemo) - mexdemo.dme.mobiledgex.net
//                > DME API Reorg (tdg2) - tdg2.dme.mobiledgex.net
//                    > Automation Testing (automationbonn) - automationbonn.dme.mobiledgex.net
            
        }
    }
}
