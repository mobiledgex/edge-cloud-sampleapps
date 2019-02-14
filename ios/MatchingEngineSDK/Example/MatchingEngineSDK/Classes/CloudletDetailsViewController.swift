//
//  CloudletDetailsViewController.swift
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

class CloudletDetailsViewController: FormViewController, CircularSpinnerDelegate
{
    let BYTES_TO_MBYTES: Double = 1024 * 1024

    var cloudlet: Cloudlet? //
    var tranferRate: Int = 0 //

    deinit
    {
        NotificationCenter.default.removeObserver(self) //
    }

    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Cloudlet Details"

        CircularSpinner.dismissButton = true //

        addObservers() //

        form +++ Section()

            //            Section() {
            //                $0.header = HeaderFooterView<EurekaLogoView>(.class)
            //            }

            <<< LabelRow    //TextRow
        {
            $0.title = "Cloudlet Name:" // row title
            $0.value = UserDefaults.standard.string(forKey: "Cloudlet Name:") ?? "azwes2Cloudlet"   //   todo unique
        }

            <<< LabelRow    //TextRow
        {
            $0.title = "App Name:"
            $0.value = UserDefaults.standard.string(forKey: "App Name:") ?? "MobiledgeX SDK Demo"
        }
            <<< LabelRow    //TextRow
        {
            $0.title = "Carrier:"
            $0.value = UserDefaults.standard.string(forKey: "Carrier:") ?? "azure"
        }
            <<< LabelRow    //TextRow   //
        {
            $0.title = "Latitude:"
            $0.value = UserDefaults.standard.string(forKey: "Latitude:") ?? "37.338"
        }
            <<< LabelRow    //TextRow   //
        {
            $0.title = "Longitude:"
            $0.value = UserDefaults.standard.string(forKey: "Longitude:") ?? "37.338"
        }
            <<< LabelRow    //TextRow   //
        {
            $0.title = "Distance:"
            $0.value = UserDefaults.standard.string(forKey: "Distance:") ?? "0"
            // todo String(format: "%.3f", value)
        }
        form +++ Section("Latency") //
            <<< LabelRow    //TextRow   //
        {
            $0.title = "Latency Min:"
            $0.value = (UserDefaults.standard.string(forKey: "Latency Min:") ?? "9999") + " ms" // JT 19.02.06
            $0.tag = "Latency Min:" //

        }.cellUpdate
        { _, row in //
            DispatchQueue.main.async
            {
                row.value = self.formatValue(self.cloudlet!.latencyMin) + " ms"
            }
        }
            <<< LabelRow    //TextRow
        {
            $0.title = "Latency Avg:"
            $0.tag = "Latency Avg:" //

            $0.value = (UserDefaults.standard.string(forKey: "Latency Avg:") ?? "0") + " ms"    // JT 19.02.06
        }
        .cellUpdate
        { _, row in //
            Swift.print("latencyAvg \(self.cloudlet!.latencyAvg)") //
            DispatchQueue.main.async
            {
                row.value = self.formatValue(self.cloudlet!.latencyAvg) + " ms"
            }
        }
            <<< LabelRow    //TextRow
        {
            $0.title = "Latency Max:"
            $0.tag = "Latency Max:" //

            $0.value = (UserDefaults.standard.string(forKey: "Latency Max:") ?? "0") + " ms" // JT 18.11.04
        }
        .cellUpdate
        { _, row in //
            DispatchQueue.main.async
            {
                row.value = self.formatValue(self.cloudlet!.latencyMax) + " ms"
            }
        }
            <<< LabelRow    //TextRow
        {
            $0.title = "Latency Stddev:"
            $0.tag = "Latency Stddev:" //

            $0.value = (UserDefaults.standard.string(forKey: "Latency Stddev:") ?? "0.0") + " ms"   // JT 19.02.06
        }
        .cellUpdate
        { _, row in //
            DispatchQueue.main.async
            {
                row.value = self.formatValue(self.cloudlet!.latencyStddev)
            }
        }

            <<< ButtonRow
        { (row: ButtonRow) -> Void in
            let numPings = Int(UserDefaults.standard.string(forKey: "Latency Test Packets") ?? "5") //

            row.title = "Latency Test: \(numPings!) pings" //
        }
        .onCellSelection
        { [weak self] _, _ in
            Swift.print("Latency Test")
            let numPings = Int(UserDefaults.standard.string(forKey: "Latency Test Packets") ?? "5") //

            self!.cloudlet!.runLatencyTest(numPings: numPings!) //
        }
        form +++ Section()
            <<< ButtonRow
        { (row: ButtonRow) -> Void in
             let downLoadStringSize = UserDefaults.standard.string(forKey: "Download Size") ?? "1 MB"

            row.title = "Speed Test: " + downLoadStringSize //
        }
        .onCellSelection
        { [weak self] _, _ in
            Swift.print("Speed Test")
            self!.cloudlet!.doSpeedTest() //
            
            CircularSpinner.show(animated: true, showDismissButton: true, delegate: nil)
            //             self!.cloudlet!.speedTestTaskRunning = false //    // JT

            CircularSpinner.setValue(0.01, animated: true)

        }.cellUpdate
        { _, row in //
            Swift.print("Speed Test :\(Double(self.tranferRate) / self.BYTES_TO_MBYTES) MBs")
            row.title = "Speed Test:\(Double(self.tranferRate)/self.BYTES_TO_MBYTES) MBs"
            // row.title = "ZZZZ \(self.tranferRate)"  //
        }

            <<< LabelRow    //TextRow   //
        {
            $0.title = "Tranfer Rate:"
            $0.tag = "tranferRate" //

            //   $0.value = "Speed Test :\(Double(self.tranferRate)/BYTES_TO_MBYTES) MBs"
            $0.value = "\(self.formatValue(Double(self.tranferRate) / self.BYTES_TO_MBYTES)) MBs"
        }
        .cellUpdate
        { _, row in //
            //  row.value =  "\(Double(self.tranferRate)/self.BYTES_TO_MBYTES) MBs"
            Swift.print("\(self.tranferRate)") //  Log
            //  let tr =  "\(self.tranferRate)"
            let tr2 = "\(self.formatValue(Double(self.tranferRate) / self.BYTES_TO_MBYTES)) MBs"
            DispatchQueue.main.async
            {
                row.value = tr2 //
            }
        }
        handleAutoStart()   // JT 19.02.05
    }   /////

    // MARK: -
    func handleAutoStart()
    {
        let autoStart = UserDefaults.standard.bool(forKey: "Latency Test AutoStart") ?? false  // JT 19.01.30 Auto-start
        
        if autoStart
        {
            let numPings = Int(UserDefaults.standard.string(forKey: "Latency Test Packets") ?? "5") //
            
            self.cloudlet!.runLatencyTest(numPings: numPings!) //
        }
    }

    private func formatValue(_ value: Double) -> String
    {
        return String(format: "%.3f", value)
    }

    func addObservers() //
    {
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "updateLatencies"), object: nil, queue: nil)
        { [weak self] _ in
            guard let _ = self else { return }

            Swift.print("updateLatencies")
            DispatchQueue.main.async
            {   //
                (self!.form.rowBy(tag: "Latency Min:") as? LabelRow)!.reload()
                (self!.form.rowBy(tag: "Latency Avg:") as? LabelRow)!.reload()
                (self!.form.rowBy(tag: "Latency Max:") as? LabelRow)!.reload()
                (self!.form.rowBy(tag: "Latency Stddev:") as? LabelRow)!.reload()
            }
        }

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "speedTestProgress"), object: nil, queue: nil)
        { [weak self] notification in
            guard let _ = self else { return }  // bullet proff for getting called after self deinit

            let n = notification.object as! NSNumber //
            let progress = Float(n.doubleValue) // notification.object as! Float //

            //   Swift.print("speedTestProgress")
            DispatchQueue.main.async
            {
                CircularSpinner.setValue(progress, animated: true)
            }
        }
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "tranferRate"), object: nil, queue: nil)
        { notification in
            // Swift.print("RegisterClient \(notification)")
            
            let d = notification.object as! Int //
            self.tranferRate = d //
            Swift.print("@@ \(self.tranferRate)") // Log
            (self.form.rowBy(tag: "tranferRate") as? LabelRow)!.reload()

            DispatchQueue.main.async
                {
                    CircularSpinner.hide() //
                    
                    (self.form.rowBy(tag: "tranferRate") as? LabelRow)!.reload() // JT 18.11.16 voodoo  todo BUG UI is a showing previous data
            }
        }

    }
}
