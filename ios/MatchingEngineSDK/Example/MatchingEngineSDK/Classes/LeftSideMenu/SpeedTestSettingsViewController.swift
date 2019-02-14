//
//  SpeedTestSettingsViewController.swift
//  Example
//
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

class SpeedTestSettingsViewController: FormViewController
{
    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Speed Test Settings"

        form +++ Section("")    // Eureka

            <<< ActionSheetRow<String>()
        {
            $0.title = "Download Size"
            $0.selectorTitle = "Download Size"
            $0.options = ["1 MB", "5 MB", "10 MB", "20 MB"]
            $0.value = UserDefaults.standard.string(forKey: "Download Size") ?? "1 MB" // initially selected
        }.onChange
        { /*[weak self]*/ row in
            Swift.print("Download Size \(row.value!)")
            UserDefaults.standard.set(row.value, forKey: "Download Size")

            //      row.subTitle = "Number of Megabytes to download for speed test" //    todo? fix subtitle second line
        }
        .cellSetup
        { _, _ in
            //     row.subTitle = "Number of Megabytes to download for speed test" //   todo fix subtitle second line
        }
            <<< ActionSheetRow<String>()    // JT 19.01.16
        {
            $0.title = "Latency Test Method"
            $0.selectorTitle = "Latency Test Method"
            $0.options = ["Socket", "Ping ICMP"]   // JT 19.01.16
            $0.value = UserDefaults.standard.string(forKey: "LatencyTestMethod") ?? "Ping"
        }.onChange
        { /*[weak self]*/ row in
            Swift.print("LatencyTestMethod" + " \(row.value)")
            UserDefaults.standard.set(row.value, forKey: "LatencyTestMethod")
            
        }
        .cellSetup
        { _, _ in

            }
        
            <<< ActionSheetRow<String>()
        {
            $0.title = "Latency Test Packets"
            $0.selectorTitle = "Latency Test Packets"
            $0.options = ["5", "10", "20"]  // JT 19.01.31
            $0.value = UserDefaults.standard.string(forKey: "Latency Test Packets") ?? "4" // initially selected
        }.onChange
        { /*[weak self]*/ row in
            Swift.print("Latency Test Packets" + " \(row)")
            UserDefaults.standard.set(row.value, forKey: "Latency Test Packets")

            // row.subTitle = "Number ot times to repeat latency test" //
        }
        .cellSetup
        { _, _ in
            //   row.subTitle = "Number ot times to repeat latency test" //
        }

            <<< SwitchRow
                {
                    $0.title = "Latency Test Auto-Start"
                    $0.value = UserDefaults.standard.bool(forKey: "Latency Test Auto-Start") // initially selected
                }.onChange
                { /*[weak self]*/ row in
                    Swift.print("Latency Test Auto-Start \(row)")
                    UserDefaults.standard.set(row.value, forKey: "Latency Test Auto-Start")
                    
                    // row.subTitle = "When cloudlet discovered" //
                    
                }.cellSetup
                { _, _ in
                    // row.subTitle = "When cloudlet discovered" //
        }
   
    }
}
