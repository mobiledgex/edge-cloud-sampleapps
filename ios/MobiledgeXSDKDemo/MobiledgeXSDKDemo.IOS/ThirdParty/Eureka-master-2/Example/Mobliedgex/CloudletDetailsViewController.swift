//
//  CloudletDetailsViewController.swift
//  Example
//
//  Created by meta30 on 11/3/18.
//  Copyright © 2018 Xmartlabs. All rights reserved.
//


import Foundation

import UIKit
import Eureka

class CloudletDetailsViewController : FormViewController
{
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        title = "Cloudlet Details"
        
        
        form +++   Section()
            
            //            Section() {
            //                $0.header = HeaderFooterView<EurekaLogoView>(.class)
            //            }
            
            <<< TextRow() {
                $0.title = "Cloudlet Name:"
                $0.value = UserDefaults.standard.string(forKey: "Cloudlet Name:") ?? "azwes2Cloudlet"
                
                }
            
            <<< TextRow() {
                $0.title = "App Name:"
                $0.value = UserDefaults.standard.string(forKey: "App Name:") ?? "MobiledgeX SDK Demo"
            }
            <<< TextRow() {
                $0.title = "Carrier:"
                $0.value = UserDefaults.standard.string(forKey: "Carrier:") ?? "azure"
            }
            <<< TextRow() {
                $0.title = "Latitude:"
                $0.value = UserDefaults.standard.string(forKey: "Latitude:") ?? "37.338"
        }
            <<< TextRow() {
                $0.title = "Longitude:"
                $0.value = UserDefaults.standard.string(forKey: "Longitude:") ?? "37.338"
        }
            <<< TextRow() {
                $0.title = "Distance:"
                $0.value = UserDefaults.standard.string(forKey: "Distance:") ?? "1237.338"
        }
            <<< TextRow() {
                $0.title = "Latency Min:"
                $0.value = UserDefaults.standard.string(forKey: "Latency Min:") ?? "4.00"  + " ms"
        }
            <<< TextRow() {
                $0.title = "Latency Avg:"
                $0.value = (UserDefaults.standard.string(forKey: "Latency Avg:") ?? "5") + " ms"
        }
            <<< TextRow() {
                $0.title = "Latency Max:"
                $0.value = UserDefaults.standard.string(forKey: "Latency Max:") ?? "96.1" + " ms"    // JT 18.11.04
        }
            <<< TextRow() {
                $0.title = "Latency Stddev:"
                $0.value = UserDefaults.standard.string(forKey: "Latency Stddev:") ?? "6.064"  + " ms"
        }
        
            <<< ButtonRow() { (row: ButtonRow) -> Void in
                row.title = "Latency Test"
                }
                .onCellSelection { [weak self] (cell, row) in
Swift.print("Latency Test")
        }
        form +++   Section()
            <<< ButtonRow() { (row: ButtonRow) -> Void in
                row.title = "Speed Test : 6,42 Mnits/sec"
                }
                .onCellSelection { [weak self] (cell, row) in
                    Swift.print("Speed Test")
        }

    }
    
}

