//
//  EnhancedNetworkLocationServicesViewController.swift
//  Example
//
//  Created by meta30 on 11/3/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//


import Foundation

import UIKit
import Eureka

class EnhancedNetworkLocationServicesViewController : FormViewController
{
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        title = "Enhanced Network Location Services"

        
        form +++   Section()
            
            //            Section() {
            //                $0.header = HeaderFooterView<EurekaLogoView>(.class)
            //            }
            
            <<< SwitchRow() {
                $0.title = "Location Verification"
                $0.value = UserDefaults.standard.bool(forKey: "Location Verification")    // initially selected
                }.onChange { [weak self] row in
                    
                    Swift.print("Location Verification \(row) \(row.value)")
                    
                    UserDefaults.standard.set(row.value, forKey:"Location Verification" )       // JT 18.11.03
                    
                } .cellSetup { cell, row in
                    row.subTitle = " Enhabced Newtwirk Location Services." // JT 18.11.04
            }
            
            <<< SwitchRow() {
                $0.title = "Network Switching Enabled"
                $0.value = UserDefaults.standard.bool(forKey: "Network Switching Enabled")    // initially selected
                }.onChange { [weak self] row in
                    
                    Swift.print("Network Switching Enabled \(row)")
                    
                    UserDefaults.standard.set(row.value, forKey: "Network Switching Enabled" )       // JT 18.11.03

                    
                }.cellSetup { cell, row in
                    row.subTitle = " Wifi to Cell network switching" // JT 18.11.04
        }
        
    }
    
}
