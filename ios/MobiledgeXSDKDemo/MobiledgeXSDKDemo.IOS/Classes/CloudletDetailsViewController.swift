//
//  CloudletDetailsViewController.swift
//  Example
//
//  Created by meta30 on 11/3/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Foundation

import UIKit
// import CircularSpinner  // JT 18.11.16

class CloudletDetailsViewController: FormViewController, CircularSpinnerDelegate
{
    let BYTES_TO_MBYTES: Double = 1024 * 1024

    var cloudlet: Cloudlet? // JT 18.11.13
    var tranferRate: Int = 0 // JT 18.11.16

    deinit
    {
        NotificationCenter.default.removeObserver(self) // JT 18.11.17
    }

    override func viewDidLoad()
    {
        super.viewDidLoad()

        title = "Cloudlet Details"

        CircularSpinner.dismissButton = true // JT 18.11.16

        addObservers() // JT 18.11.17 todo everywhere

        form +++ Section()

            //            Section() {
            //                $0.header = HeaderFooterView<EurekaLogoView>(.class)
            //            }

            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Cloudlet Name:" // row title
            $0.value = UserDefaults.standard.string(forKey: "Cloudlet Name:") ?? "azwes2Cloudlet"   // JT 18.11.19 todo unique
        }

            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "App Name:"
            $0.value = UserDefaults.standard.string(forKey: "App Name:") ?? "MobiledgeX SDK Demo"
        }
            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Carrier:"
            $0.value = UserDefaults.standard.string(forKey: "Carrier:") ?? "azure"
        }
            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Latitude:"
            $0.value = UserDefaults.standard.string(forKey: "Latitude:") ?? "37.338"
        }
            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Longitude:"
            $0.value = UserDefaults.standard.string(forKey: "Longitude:") ?? "37.338"
        }
            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Distance:"
            $0.value = UserDefaults.standard.string(forKey: "Distance:") ?? "1237.338"
            // todo String(format: "%.3f", value)
        }
        form +++ Section("Latency") // JT 18.11.19
            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Latency Min:"
            $0.value = UserDefaults.standard.string(forKey: "Latency Min:") ?? "4.00" + " ms"
            $0.tag = "Latency Min:" // JT 18.11.13

        }.cellUpdate
        { _, row in // JT 18.11.13
            DispatchQueue.main.async
            {
                row.value = self.formatValue(self.cloudlet!.latencyMin) + " ms"
            }
        }
            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Latency Avg:"
            $0.tag = "Latency Avg:" // JT 18.11.13

            $0.value = (UserDefaults.standard.string(forKey: "Latency Avg:") ?? "5") + " ms"
        }
        .cellUpdate
        { _, row in // JT 18.11.13
            Swift.print("latencyAvg \(self.cloudlet!.latencyAvg)") // JT 18.11.16
            DispatchQueue.main.async
            {
                row.value = self.formatValue(self.cloudlet!.latencyAvg) + " ms"
            }
        }
            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Latency Max:"
            $0.tag = "Latency Max:" // JT 18.11.13

            $0.value = UserDefaults.standard.string(forKey: "Latency Max:") ?? "96.1" + " ms" // JT 18.11.04
        }
        .cellUpdate
        { _, row in // JT 18.11.13
            DispatchQueue.main.async
            {
                row.value = self.formatValue(self.cloudlet!.latencyMax) + " ms"
            }
        }
            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Latency Stddev:"
            $0.tag = "Latency Stddev:" // JT 18.11.13

            $0.value = UserDefaults.standard.string(forKey: "Latency Stddev:") ?? "6.064" + " ms"
        }
        .cellUpdate
        { _, row in // JT 18.11.13
            DispatchQueue.main.async
            {
                row.value = self.formatValue(self.cloudlet!.latencyStddev)
            }
        }

            <<< ButtonRow
        { (row: ButtonRow) -> Void in
            let numPings = Int(UserDefaults.standard.string(forKey: "Latency Test Packets") ?? "5") // JT 18.11.16

            row.title = "Latency Test: \(numPings!) pings" // JT 18.11.19
        }
        .onCellSelection
        { [weak self] _, _ in
            Swift.print("Latency Test")
            let numPings = Int(UserDefaults.standard.string(forKey: "Latency Test Packets") ?? "5") // JT 18.11.16

            self!.cloudlet!.runLatencyTest(numPings: numPings!) // JT 18.11.13  // JT 18.11.16
        }
        form +++ Section()
            <<< ButtonRow
        { (row: ButtonRow) -> Void in
             let downLoadStringSize = UserDefaults.standard.string(forKey: "Download Size") ?? "1 MB"

            row.title = "Speed Test: " + downLoadStringSize // JT 18.11.19
        }
        .onCellSelection
        { [weak self] _, _ in
            Swift.print("Speed Test")
            self!.cloudlet!.doSpeedTest() // JT 18.11.16

            CircularSpinner.show(animated: true, showDismissButton: true, delegate: nil)
            CircularSpinner.setValue(0.01, animated: true)

        }.cellUpdate
        { _, row in // JT 18.11.13
            Swift.print("Speed Test :\(Double(self.tranferRate) / self.BYTES_TO_MBYTES) MBs")
            row.title = "Speed Test:\(Double(self.tranferRate)/self.BYTES_TO_MBYTES) MBs"
            // row.title = "ZZZZ \(self.tranferRate)"  // JT 18.11.16
        }

            <<< LabelRow    //TextRow   // JT 18.11.19
        {
            $0.title = "Tranfer Rate:"
            $0.tag = "tranferRate" // JT 18.11.13

            //   $0.value = "Speed Test :\(Double(self.tranferRate)/BYTES_TO_MBYTES) MBs"
            $0.value = "\(self.formatValue(Double(self.tranferRate) / self.BYTES_TO_MBYTES)) MBs"
        }
        .cellUpdate
        { _, row in // JT 18.11.13
            //  row.value =  "\(Double(self.tranferRate)/self.BYTES_TO_MBYTES) MBs"
            Swift.print("\(self.tranferRate)") // JT 18.11.16
            //  let tr =  "\(self.tranferRate)"
            let tr2 = "\(self.formatValue(Double(self.tranferRate) / self.BYTES_TO_MBYTES)) MBs"
            DispatchQueue.main.async
            {
                row.value = tr2 // JT 18.11.16
            }
        }

    }   /////

    // MARK: -


    private func formatValue(_ value: Double) -> String
    {
        return String(format: "%.3f", value)
    }

    func addObservers() // JT 18.11.17
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

            let n = notification.object as! NSNumber // JT 18.11.18
            let progress = Float(n.doubleValue) // notification.object as! Float // JT 18.11.09

            //   Swift.print("speedTestProgress")
            DispatchQueue.main.async
            {
                CircularSpinner.setValue(progress, animated: true)
            }
        }
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "tranferRate"), object: nil, queue: nil)
        { notification in
            // Swift.print("RegisterClient \(notification)")
            
            let d = notification.object as! Int // JT 18.11.09
            self.tranferRate = d // JT 18.11.16
            Swift.print("@@ \(self.tranferRate)") // JT 18.11.16
            (self.form.rowBy(tag: "tranferRate") as? LabelRow)!.reload()

            DispatchQueue.main.async
                {
                    CircularSpinner.hide() // JT 18.11.16
                    
                    (self.form.rowBy(tag: "tranferRate") as? LabelRow)!.reload() // JT 18.11.16 voodoo  todo BUG UI is a showing previous data
            }
        }

    }
}
