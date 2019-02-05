//
//  FaceDectectionSettingsViewController.swift
//  Example
//
//  Created by meta30 on 11/3/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//


import Foundation

import UIKit
import Eureka

class FaceDectectionSettingsViewController : FormViewController
{
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        title = "Face Dectection Setting"
        
        
        form +++   Section()
            
            //            Section() {
            //                $0.header = HeaderFooterView<EurekaLogoView>(.class)
            //            }
            
            <<< SwitchRow() {
                $0.title = "Multi-face"
                $0.value = UserDefaults.standard.bool(forKey: "Multi-face")    // initially selected

                }.onChange { [weak self] row in
                    
                    Swift.print("Multi-face \(row)")
                    
                    Swift.print("Multi-face \(row) \(row.value!)")
                    
                    UserDefaults.standard.set(row.value, forKey:"Multi-face" )       // JT 18.11.03

            }
                .cellSetup { cell, row in
                  row.subTitle = " Track multiple faces" // JT 18.11.04
            }
            <<< SwitchRow() {
                $0.title = "Local processing"
                $0.value = UserDefaults.standard.bool(forKey: "Local processing")    // initially selected
                }.onChange { [weak self] row in
                    
                    Swift.print("Local processing" + " \(row) \(row.value!)")
                    
                    UserDefaults.standard.set(row.value, forKey:"Local processing" )       // JT 18.11.03
            }
                .cellSetup { cell, row in
                    row.subTitle = " Include tracking via local processing " // JT 18.11.04
            }
            <<< SwitchRow() {
                $0.title = "Show full process latancy"
                $0.value = UserDefaults.standard.bool(forKey: "Show full process latancy")    // initially selected
                }.onChange { [weak self] row in
                    
                    Swift.print("Show full process latancy"  + " \(row) \(row.value!)")
                    
                    UserDefaults.standard.set(row.value, forKey:"Show full process latancy"  )       // JT 18.11.03
            }
                .cellSetup { cell, row in
                    row.subTitle = " Measure all" // JT 18.11.04
            }
            <<< SwitchRow() {
                $0.title = "Show network latancy"
                $0.value = UserDefaults.standard.bool(forKey: "Show network latancy")    // initially selected
                }.onChange { [weak self] row in
                    
                    Swift.print("Show network latancy"  + " \(row) \(row.value!)")
                    
                    UserDefaults.standard.set(row.value, forKey:"Show network latancy"  )       // JT 18.11.03
                }
                .cellSetup { cell, row in
                    row.subTitle = " Measures only network latency" // JT 18.11.04
            }
            
            <<< SwitchRow() {
                $0.title = "Show Stddev"
                $0.value = UserDefaults.standard.bool(forKey: "Show Stddev")    // initially selected
                }.onChange { [weak self] row in
                    
                    
                    Swift.print("Show Stddev"  + " \(row) \(row.value!)")
                    
                    UserDefaults.standard.set(row.value, forKey:"Show Stddev"  )       // JT 18.11.03
        }
                .cellSetup { cell, row in
                    row.subTitle = " Standard deviation" // JT 18.11.04
            }
            
            <<< SwitchRow() {
                $0.title = "Use Rolling Average"
                $0.value = UserDefaults.standard.bool(forKey: "Use Rolling Average")    // initially selected
                }.onChange { [weak self] row in
                    
                    Swift.print("Use Rolling Average"  + " \(row) \(row.value!)")
                    
                    UserDefaults.standard.set(row.value, forKey:"Use Rolling Average"  )       // JT 18.11.03
                    
                }  .cellSetup { cell, row in
                    row.subTitle = " Show measurements and rolling average." // JT 18.11.04
        }
    }
    
}

