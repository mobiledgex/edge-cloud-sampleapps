//
//  EnhancedNetworkLocationServicesViewController.swift
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

class EnhancedNetworkLocationServicesViewController: FormViewController
{
    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Enhanced Network Location Services"

        form +++ Section()

            //            Section() {
            //                $0.header = HeaderFooterView<EurekaLogoView>(.class)
            //            }

            <<< SwitchRow
        {
            $0.title = "Location Verification"
            $0.value = UserDefaults.standard.bool(forKey: "Location Verification") // initially selected
        }.onChange
        { /*[weak self]*/ row in
            Swift.print("Location Verification \(row) \(row.value!)")

         //   UserDefaults.standard.set(row.value, forKey: "Location Verification") // JT 19.01.15
            UserDefaults.standard.set( row.value, forKey: "VerifyLocation")     // JT 19.01.15

        }.cellSetup
        { _, row in
            row.subTitle = " Enhanced Network Location Services."
        }

        #if false   // JT 18.11.19 Network Switching for IOS?
//        <<< SwitchRow
//            {
//                $0.title = "Network Switching Enabled"
//                $0.value = UserDefaults.standard.bool(forKey: "Network Switching Enabled") // initially selected
//            }.onChange
//            { /*[weak self]*/ row in
//                Swift.print("Network Switching Enabled \(row)")
//
//                UserDefaults.standard.set(row.value, forKey: "Network Switching Enabled") // JT 18.11.03
//
//            }.cellSetup
//            { _, row in
//                row.subTitle = " Wifi to Cell network switching" // JT 18.11.04
//        }

        #endif

    }
}
