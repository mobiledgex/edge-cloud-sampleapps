//
//  FaceDectectionSettingsViewController.swift
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

class FaceDectectionSettingsViewController: FormViewController
{
    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Face Dectection Setting"

        form +++ Section()  // Eureka

            //            Section() {
            //                $0.header = HeaderFooterView<EurekaLogoView>(.class)
            //            }

            <<< SwitchRow
        {
            $0.title = "Multi-face "
            $0.value = UserDefaults.standard.bool(forKey: "Multi-face")

        }.onChange
        { /* [weak self] */ row in
            Swift.print("Multi-face \(row) \(row.value!)")

            UserDefaults.standard.set(row.value, forKey: "Multi-face")
        }
        .cellSetup
        { _, row in
            row.subTitle = " Track multiple faces"
        }
            <<< SwitchRow
        {
            $0.title = "Local processing"
            $0.value = UserDefaults.standard.bool(forKey: "Local processing") // initially selected
        }.onChange
        { /* [weak self] */ row in
            Swift.print("Local processing" + " \(row) \(row.value!)")

            UserDefaults.standard.set(row.value, forKey: "Local processing")
        }
        .cellSetup
        { _, row in
            row.subTitle = " Include tracking via local processing "
        }
            <<< SwitchRow
        {
            $0.title = "Show full process latency"
            $0.value = UserDefaults.standard.bool(forKey: "Show full process latency") // initially selected
        }.onChange
        { /* [weak self] */ row in
            Swift.print("Show full process latency" + " \(row) \(row.value!)")

            UserDefaults.standard.set(row.value, forKey: "Show full process latency") //
        }
        .cellSetup
        { _, row in
            row.subTitle = " Measure all"
        }
            <<< SwitchRow
        {
            $0.title = "Show network latency"
            $0.value = UserDefaults.standard.bool(forKey: "Show network latency") // initially selected
        }.onChange
        { /* [weak self] */ row in
            Swift.print("Show network latency" + " \(row) \(row.value!)")

            UserDefaults.standard.set(row.value, forKey: "Show network latency") //
        }
        .cellSetup
        { _, row in
            row.subTitle = " Measures only network latency" //
        }

            <<< SwitchRow
        {
            $0.title = "Show Stddev"
            $0.value = UserDefaults.standard.bool(forKey: "Show Stddev") // initially selected
        }.onChange
        { /* [weak self] */ row in

            Swift.print("Show Stddev" + " \(row) \(row.value!)")

            UserDefaults.standard.set(row.value, forKey: "Show Stddev") //
        }
        .cellSetup
        { _, row in
            row.subTitle = " Standard deviation" // 
        }

            <<< SwitchRow
        {
            $0.title = "Use Rolling Average"
            $0.value = UserDefaults.standard.bool(forKey: "Use Rolling Average") // initially selected
        }.onChange
        { /* [weak self] */ row in
            Swift.print("Use Rolling Average" + " \(row) \(row.value!)")

            UserDefaults.standard.set(row.value, forKey: "Use Rolling Average")

        }.cellSetup
        { _, row in
            row.subTitle = " Show measurements and rolling average."
        }
    }
}
