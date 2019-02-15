// Sample Client of SDK
//
//  Sample_client.swift
//  MatchingEngineSDK Example
//
//  Port from cpp SDK demo to swift by Jean Tantra, Metatheory.com
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

import Foundation
import CoreLocation
import MapKit
import Security
import UIKit

import Alamofire
import GoogleMaps

import NSLogger // JT 19.01.07 dlog

import MatchingEngineSDK    // SDK

// ----------------------------------------

private var locationRequest: LocationRequest? // so we can stop updates

// --------
// face servers
//
private let faceServerPort: String = "8008"

private let DEF_FACE_HOST_CLOUD = "facedetection.defaultcloud.mobiledgex.net"
private let DEF_FACE_HOST_EDGE = "facedetection.defaultedge.mobiledgex.net"

// --------

// This file Handles Events from:
//  Menu:
//  "Register Client",  // these first two are done actomicaly at launch
//  "Get App Instances",    // displays network POIs on map
//
//  "Verify Location",      // visual feedback: gray, green, failed: red
//  "Find Closest Cloudlet",    // animate Closest Cloudlet to center
//  "Reset Location",   // animate userMarker back to its gps location



// MARK: -


func processAppInstList(_ d: [String: Any] )    // JT 19.01.31 hoist
{
    Swift.print("GetAppInstlist1 \(d)")
    
    //  theMap!.clear()       // JT 18.11.12 todo main thread, where
    
    var cloudlets = [String: Cloudlet]()
    Swift.print("~~~~~")
    
    var boundsBuilder = GMSCoordinateBounds()
    
    var marker: GMSMarker?
    
    for (index, cld) in d.enumerated()
    {
        Swift.print("\n•\n \(index):\(cld)")
        
        Swift.print("\(index): \(cld.key)")
        
        if cld.key == "Cloudlets" // "cloudlet_location"
        {
            //       let ddd = cld.value
            let a = cld.value as! [[String: Any]]   // Dictionary/json
            
            Swift.print("••• \(a)")
            
            for d in a
            {
                // ["CarrierName", "CloudletName", "GpsLocation", "Distance", "Appinstances"]
                
                Swift.print("\(Array(d.keys))")
                let gps = d["GpsLocation"] as! [String: Any] // "cloudlet_location"
                
                // let gps = cld.value as! [String:Any]
                Swift.print("\(gps)")
                
                let loc = CLLocationCoordinate2D(
                    latitude: Double((gps["latitude"] as! NSNumber).stringValue)!,
                    longitude: Double((gps["longitude"] as! NSNumber).stringValue)!
                )   // JT 19.01.01
                Swift.print("\(loc)")
                
                Swift.print("\(Array(d.keys))\n")
                Swift.print("d ••••• \(d)")
                
                let dd = d["Appinstances"] as! [[String: Any]]  // Dictionary/json
                let uri = dd[0]["FQDN"] as! String // todo, now just use first
                let appName = dd[0]["AppName"] as! String
                
                let ports =  dd[0]["ports"] as! [[String: Any]]
                Swift.print("ports \(ports)")   // JT 19.01.30
                let portsDic = ports[0]     // JT 19.01.29
                
                let theFQDN_prefix = portsDic["FQDN_prefix"] as! String    // JT 19.01.30
                
                
                Swift.print("cloudlet uri: \(uri)")
                Swift.print("dd \(dd)")
                
                let carrierName = d["CarrierName"] as! String
                let cloudletName = d["CloudletName"] as! String
                let distance = d["Distance"] as! Double
                
                boundsBuilder = boundsBuilder.includingCoordinate(loc)
                
                marker = GMSMarker(position: loc)
                marker!.userData = cloudletName
                marker!.title = cloudletName
                marker!.snippet = "Tap for details"
                
                let iconTemplate = UIImage(named: "ic_marker_cloudlet-web")
                
                // todo refactor - make func
                let tint = getColorByHex(MexRegisterClient.COLOR_NEUTRAL)
                let tinted = iconTemplate!.imageWithColor(tint)
                let resized = tinted.imageResize(sizeChange: CGSize(width: 40, height: 30))
                
                let i2 = textToImage(drawText: "M", inImage: resized, atPoint: CGPoint(x: 11, y: 4))
                
                marker?.icon = (cloudletName.contains("microsoft") || cloudletName.contains("azure") || carrierName.contains("azure")) ? i2 : resized    // JT 19.01.30 // JT 19.01.31
                
                //                        init(_ cloudletName: String,
                //                        _ appName: String,
                //                        _ carrierName: String,
                //                        _ gpsLocation: CLLocationCoordinate2D ,
                //                        _ distance: Double,
                //                        _ uri: String,
                //                        _ marker: GMSMarker,
                //                        _ numBytes: Int,
                //                        _ numPackets: Int) // LatLng
                
                Swift.print("Cloudlet: \(cloudletName), \(appName), \(carrierName), \(loc),\n \(uri)")
                let cloudlet = Cloudlet(cloudletName, appName, carrierName,
                                        loc,
                                        distance,
                                        uri,
                                        theFQDN_prefix, // JT 19.01.30
                    marker!,
                    1_048_576,  // actually uses setting alue at run time
                    0)
                
                marker?.map = theMap
                cloudlets[cloudletName] = cloudlet
            }
        }
    }
    Swift.print("~~~~~]\n\(cloudlets)")
    
    CloudletListHolder.getSingleton().setCloudlets(mCloudlets: cloudlets)
    
    if !(boundsBuilder.southWest == boundsBuilder.northEast)
    {
        Swift.print("Using cloudlet boundaries")
        let padding: CGFloat  = 70.0 // offset from edges of the map in pixels
        
        
        theMap!.animate(with: .fit(boundsBuilder, withPadding: padding))
    }
    else
    {
        Swift.print("No cloudlets. Don't zoom in")
    }
}


func makeUserMakerImage(_ color: UInt32) -> UIImage
{
    let iconTemplate = UIImage(named: "ic_marker_mobile-web")
    let tint = getColorByHex(color)
    let tinted = iconTemplate!.imageWithColor(tint)
    let resized = tinted.imageResize(sizeChange: CGSize(width: 60, height: 60))

    return resized
}


// MARK: -

private func useCloudlets(_ findCloudletReply: [String: Any]) // unused
{
    if findCloudletReply.count == 0
    {
        Swift.print("REST VerifyLocation Status: NO RESPONSE")
    }
    else
    {
        //            cout << "REST FindCloudlet Status: "
        //                 << "Version: " << findCloudletReply["ver"]
        //                 << ", Location Found Status: " << findCloudletReply["status"]
        //                 << ", Location of cloudlet. Latitude: " << findCloudletReply["cloudlet_location"]["lat"]
        //                 << ", Longitude: " << findCloudletReply["cloudlet_location"]["long"]
        //                 << ", Cloudlet FQDN: " << findCloudletReply["fqdn"] << endl;

        let loooc = findCloudletReply["cloudlet_location"] as! [String: Any]
        let latN = loooc["lat"] as? NSNumber // ZZZ
        let lat = "\(latN!)"
        let longN = loooc["long"] as? NSNumber
        let long = "\(longN!)"

        let line1 = "REST FindCloudlet Status: \n"
        let ver = findCloudletReply["ver"] as? NSNumber
        let line2 = "Version: " + "\(ver!)\n"
        let line3 = ", Location Found Status: " + (findCloudletReply["status"] as! String) + "\n"
        let line4 = ", Location of cloudlet. Latitude: " + lat + "\n"
        let line5 = ", Longitude: " + long + "\n"
        Swift.print("\(findCloudletReply["FQDN"]!)")
        let line6 = ", Cloudlet FQDN: " + (findCloudletReply["FQDN"] as! String ) + "\n"

        Swift.print(line1 + line2 + line3 + line4 + line5 + line6)
        let ports: [[String: Any]] = findCloudletReply["ports"] as! [[String: Any]]
        
       // let size = ports.count // size_t
        for appPort in ports
        {
            Swift.print("\(appPort)")
            //  let ap = appPort as [String:Any]
            //                cout << ", AppPort: Protocol: " << appPort["proto"]
            //                     << ", AppPort: Internal Port: " << appPort["internal_port"]
            //                     << ", AppPort: Public Port: " << appPort["public_port"]
            //                     << ", AppPort: Public Path: " << appPort["public_path"]
            //                     << endl;
            //
            //                let proto = appPort["proto"]
            //                let internal_port = appPort["internal_port"]
            //
            //                let public_port = appPort["public_port"]
            //                let public_path = appPort["public_path"]
            //
            //                Swift.print(", AppPort: Protocol: \(proto)" +
            //                ", AppPort: Internal Port: \(internal_port)" +
            //                    ", AppPort: Internal Port: \(public_port)" +
            //                    ", AppPort: ublic Path:  \(public_path)"

            //                )
        }
    }
}

// MARK: -


/**
 * This makes a web service call to the location simulator to update the current IP address
 * entry in the database with the given latitude/longitude.
 *
 * @param lat
 * @param lng
 */
public func updateLocSimLocation(_ lat: Double, _ lng: Double)
{
    // Swift.print("\(#function)")

    let jd: [String: Any]? = ["latitude": lat, "longitude": lng]    // Dictionary/json

    let hostName: String = MexUtil.shared.generateDmeHostPath(MexUtil.shared.getCarrierName()).replacingOccurrences(of: "dme", with: "locsim")

    let urlString: URLConvertible = "http://\(hostName):8888/updateLocation"

    Swift.print("\(urlString)")

    Alamofire.request( urlString,
                      method: HTTPMethod.post,
                      parameters: jd,
                      encoding: JSONEncoding.default)
        .responseString
    { response in
        Swift.print("----\n")
        Swift.print("\(response)")
        //     debugPrint(response)

        switch response.result {
        case .success:
            //      debugPrint(response)
            SKToast.show(withMessage: "UpdateLocSimLocation result: \(response)")

        case let .failure(error):
            print(error)
            SKToast.show(withMessage: "UpdateLocSimLocation Failed: \(error)")
        }
    }
}



func processFindCloudletResult(_ d: [String: Any])  // JT 19.01.31
{
    // Swift.print("\(#function)")

    for (index, cld) in d.enumerated()
    {
        Swift.print("\n•\n \(index):\(cld)")
        
        //                init(_ cloudletName: String,
        //                _ appName: String,
        //                _ carrierName: String,
        //                _ gpsLocation: CLLocationCoordinate2D ,
        //                _ distance: Double,
        //                _ uri: String,
        //                _ marker: GMSMarker,
        //                _ numBytes: Int,
        //                _ numPackets: Int) // LatLng
        
        //                 if index == 0
        //                 {
        //                    uri = cld.value as! String
        //                }
        Swift.print("\(index): \(cld.key)")
        
        if cld.key == "FQDN"
        {
            let v = cld.value
            Swift.print("•FQDN• \(v)")
            
            MexUtil.shared.closestCloudlet = v as! String
            
            Swift.print("")
        }
        
        if cld.key == "cloudlet_location"
        {
            let dd = cld.value as! [String: Any] // Dictionary/json
            
            Swift.print("••• \(dd)")
            
            let loc = CLLocationCoordinate2D(
                latitude: Double((dd["latitude"] as! NSNumber).stringValue)!,
                longitude: Double((dd["longitude"] as! NSNumber).stringValue)!
            )   // JT 19.01.05
            
            theMap!.animate(toLocation: loc)
            SKToast.show(withMessage: "Found cloest cloudlet")
            
            // break
        }
    }
}

// MARK: -
// MARK: resetUserLocation

func resetUserLocation(_ show: Bool) // called by "Reset user location" menu
{
    // Swift.print("\(#function)")

    locationRequest = Locator.subscribePosition(accuracy: .house, onUpdate:
        { newLocation in
            // print("New location received: \(newLocation)")
            if userMarker == nil
            {
                doUserMarker(newLocation.coordinate)
            }
            userMarker!.position = newLocation.coordinate
            
            DispatchQueue.main.async
                {
                    stopGPS()
            }
            
            if show
            {
                theMap!.animate(toLocation: userMarker!.position)
            }
            
    }, onFail: { err, _ in
        print("subscribePosition: Failed with error: \(err)")
    })
}

private func stopGPS()
{
    Locator.stopRequest(locationRequest!)
}


func doUserMarker(_ loc: CLLocationCoordinate2D)
{
    // Swift.print("\(#function)")

    // requestWhenInUseAuthorization()
    
    userMarker = GMSMarker(position: loc)
    userMarker!.title = "You are here"
    userMarker!.snippet = "Drag to spoof" //   marker!.snippet = "Tap for details"
    
    let resized =  makeUserMakerImage(MexRegisterClient.COLOR_NEUTRAL)
    
    userMarker!.icon = resized
    userMarker!.map = theMap
    
    userMarker!.isDraggable = true // drag to test spoofing
}

// MARK: -

// used by: GetToken, getAppInstNow, verify  loc

 public func retrieveLocation() -> [String: Any]
{
    // Swift.print("\(#function)")

    var location:[String: Any] = [ "latitude": -122.149349, "longitude": 37.459609] //     //  json location, somewhere

    if userMarker != nil // get app isnt sets userMarker
    {
        location["latitude"] = userMarker!.position.latitude
        location["longitude"] = userMarker!.position.longitude
    }

    return location
}

// MARK: -
public var faceRecognitionImages2 =  [(UIImage,String)]()  // image + service. one at a time   // JT 19.02.05

class MexFaceRecognition
{
    var faceDetectionStartTimes:[String:DispatchTime]? // two at a time cloud/edge
    var faceRecognitionStartTimes:[String:DispatchTime]? // two at a time cloud/edge

   var faceRecognitionCurrentImage: UIImage?
    
    // Mark: -
    // Mark: FaceDetection
    
    func FaceDetection(_ image: UIImage?, _ service: String)         -> Future<[String: AnyObject], Error>
    {
        // Swift.print("\(#function)")

        let broadcast =  "FaceDetectionLatency" + service
        
        let faceDetectionFuture = FaceDetectionCore(image, service, post: broadcast)
        
        return faceDetectionFuture  // JT 19.02.05
    }
    
    //  todo? pass in host
    
    func FaceDetectionCore(_ image: UIImage?,  _ service: String, post broardcastMsg: String?)     -> Future<[String: AnyObject], Error>
    {
        // Swift.print("\(#function)")

        let promise = Promise<[String: AnyObject], Error>()
        
        // detector/detect
        // Used to send a face image to the server and get back a set of coordinates for any detected faces.
        // POST http://<hostname>:8008/detector/detect/
        
        let faceDetectionAPI: String = "/detector/detect/"
        
        //    Swift.print("FaceDetection")
        //    Swift.print("FaceDetection MEX .")
        //    Swift.print("====================\n")
        //
        
        getNetworkLatency(DEF_FACE_HOST_EDGE, post: "updateNetworkLatenciesEdge")
        getNetworkLatency(DEF_FACE_HOST_CLOUD, post: "updateNetworkLatenciesCloud")  //
        
        let _ = GetSocketLatency( DEF_FACE_HOST_CLOUD, Int32(faceServerPort)!, "latencyCloud")   //
        
        
        let baseuri = (service == "Cloud" ? DEF_FACE_HOST_CLOUD  : DEF_FACE_HOST_EDGE) + ":" + faceServerPort  //
        
        let urlStr = "http://" + baseuri + faceDetectionAPI //   URLConvertible
        
        // Swift.print("urlStr \(urlStr)")
        
        var params: [String: String] = [:] //
        
        //   urlStr = "http://mobiledgexsdkdemomobiledgexsdkdemo10.microsoftwestus2cloudlet.azure.mobiledgex.net:8008/detector/detect/"
        
        if let image = image
        {
            let imageData = (image.pngData()! as NSData).base64EncodedString(
                options: NSData.Base64EncodingOptions.lineLength64Characters
            )
            
            params["image"] = imageData
            
            //   let imageData2 = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"  //  tmp smallest working example
            //   params["image"] = imageData2 //  tmp
            
            let headers: HTTPHeaders = [
                "Accept": "application/json",
                // "Content-Type": "application/json",    //  fails. we are doing url encoding no json
                "Charsets": "utf-8",
                ]

            
            if faceDetectionStartTimes == nil   //
            {
                faceDetectionStartTimes = [String:DispatchTime]()   // JT 19.02.11 todo threadsafe Dictionary
            }
            faceDetectionStartTimes![service] =  DispatchTime.now() //
            

            let _ = pendingCount.increment()    // JT 19.02.05
            //Swift.print("0=-- \(faceDetectCount.add(0)) \(pendingCount.add(0)) ")  // JT  // JT 19.02.05  // JT 19.02.06

            let requestObj = Alamofire.request(urlStr,
                                               method: HTTPMethod.post,
                                               parameters: params
                // , encoding: JSONEncoding.default // of -d
                , headers: headers)
                
                .responseJSON
                { response in
                    //    Swift.print("----\n")
                    //    Swift.print("\(response)")
                    //    debugPrint(response)
                    let _ = pendingCount.decrement()    // JT 19.02.05

                    switch response.result
                    {
                    case let .success(data):
                        
                        let end = DispatchTime.now() // <<<<<<<<<<   end time

                        // Swift.print("")---
                        print("•", terminator:"")   // JT 19.01.28

                        let d = data as! [String: Any]
                        let success = d["success"] as! String
                        if success == "true"
                        {
                            print("Y.\(service) ", terminator:"")   // JT 19.01.28
   // Swift.print("data: \(data)")
                            
                            let start =  self.faceDetectionStartTimes![service] //
                            let nanoTime = end.uptimeNanoseconds - start!.uptimeNanoseconds  //self.faceDetectionStartTime!.uptimeNanoseconds // <<<<< Difference in nano seconds (UInt64)
                            

                            
                            let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests
                            
                           // Swift.print("FaceDetection time: \(timeInterval)")
                            SKToast.show(withMessage: "FaceDetection  time: \(timeInterval) result: \(data)")
                            
                            let aa = d["rects"]
                            
                            let msg =    "FaceDetection" + service   // JT 19.02.04

                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: msg), object: aa) //   draw blue rect around face  [[Int]]
                            
                            promise.succeed(value: d as [String : AnyObject])
                            
                            let latency = String(format: "%4.3f", timeInterval * 1000)
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: broardcastMsg!), object: latency)
                        }
                        else
                        {
                            //        Logger.shared.log(.network, .info, postName + " request\n \(request) \n") // JT 19.01.04
                            print("N.\(service) ", terminator:"")   // JT 19.01.28

                        }
                     
                    case let .failure(error):
                        print(error)

                        promise.fail(error: error)

                        Swift.print("error doAFaceDetection")

                    } // end sucess/failure
                    
                  //  Swift.print("1=-- \(faceDetectCount.add(0))")  // JT  // JT 19.02.05
                    
                    if faceDetectCount.decrement() == 0
                    {
                        faceDetectCount = OSAtomicInt32(3)  // JT 19.02.05 next
                    }
                    
                    //Swift.print("2=-- \(faceDetectCount.add(0)) \(pendingCount.add(0)) ")  // JT  // JT 19.02.05  // JT 19.02.06
                    
            }
            
            // debugPrint(requestObj) // dump curl
            // Swift.print("")
        }
        
        return promise.future
    }

    // Mark: -
    // Mark: FaceRecognition
    
    
    
    func doNextFaceRecognition()
    {
        // Swift.print("\(#function)")

        if faceRecognitionImages2.count == 0    // we put 2 copys of same image and route to cloud/edge
        {
            faceDetectCount = OSAtomicInt32(3)  // JT 19.02.05 next

            print("+", terminator:"")   // JT 19.01.28

            return
        }
        let tuple = faceRecognitionImages2.removeFirst()
        let imageOfFace = tuple.0 as UIImage
        let service = tuple.1 as String
        faceRecognitionCurrentImage = imageOfFace
        
        var faceRecognitionFuture:Future<[String: AnyObject], Error>? // async result (captured by async?)
        
        faceRecognitionFuture = FaceRecognition(imageOfFace, service )
        
        faceRecognitionFuture!.on(
            success:
            {
                print("FaceRecognition received value: \($0)")
                
                let reply = $0 as [String: Any]    //   json/dictionary
                print("FaceRecognition received value: \(reply)")
                
                
                SKToast.show(withMessage: "FaceRec \(reply["subject"]) confidence: \(reply["confidence"]) ")
                Swift.print("FaceRecognition \(reply)")
                
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: "faceRecognized" + service), object: reply )
                
                DispatchQueue.main.async {
                    self.doNextFaceRecognition()     //   next
                    
                }
                //Log.logger.name = "FaceDetection"
                //logw("\FaceDetection result: \(registerClientReply)")
        },
            failure: { print("FaceRecognition failed with error: \($0)")
                
                DispatchQueue.main.async {
                    self.doNextFaceRecognition()     //   next
                    
                }
                
        },
            completion: { let _ = $0 //print("completed with result: \($0)")
                
        }
        )
    }
    
    
    
    func FaceRecognition(_ image: UIImage?, _ service: String)
        -> Future<[String: AnyObject], Error>   // JT 19.02.05
    {
        // Swift.print("\(#function)")

        let promise = Promise<[String: AnyObject], Error>()
        
       // Logger.shared.log(.network, .info, image! )      //

        // detector/detect
        // Used to send a face image to the server and get back a set of coordinates for any detected faces.
        // POST http://<hostname>:8008/detector/detect/
        
        let faceRecognitonAPI: String = "/recognizer/predict/"
        
        //    Swift.print("FaceRecogniton")
        //    Swift.print("FaceRecogniton MEX .")
        //    Swift.print("====================\n")
        
        
        let postMsg =  "faceRecognitionLatency" + service
        let baseuri = (service ==  "Cloud" ? DEF_FACE_HOST_CLOUD : DEF_FACE_HOST_EDGE)   + ":" + faceServerPort  //
        
        let urlStr = "http://" + baseuri + faceRecognitonAPI //  URLConvertible
        
         Swift.print("urlStr \(urlStr)")
        
        var params: [String: String] = [:]
        
        //   urlStr = "http://mobiledgexsdkdemomobiledgexsdkdemo10.microsoftwestus2cloudlet.azure.mobiledgex.net:8008/recognizer/predict/"
        
        if let image = image
        {
            let imageData = (image.pngData()! as NSData).base64EncodedString(
                options: NSData.Base64EncodingOptions.lineLength64Characters
            )
            
            params["image"] = imageData
            
            //   let imageData2 = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"  //  tmp smallest working example
            //   params["image"] = imageData2 //  tmp
            
            let headers: HTTPHeaders = [
                "Accept": "application/json",
                // "Content-Type": "application/json",    // fails. we are doing url encoding no json
                "Charsets": "utf-8",
                ]
            
    
            if faceRecognitionStartTimes == nil   // LIT hack
            {
                faceRecognitionStartTimes = [String:DispatchTime]()
            }
            faceRecognitionStartTimes![service] =  DispatchTime.now() //
            
            let requestObj = Alamofire.request(urlStr,
                                               method: HTTPMethod.post,
                                               parameters: params
                // , encoding: JSONEncoding.default // of -d
                , headers: headers)
                
                .responseJSON
                { response in
                    //    Swift.print("----\n")
                    //    Swift.print("\(response)")
                    //    debugPrint(response)
                    
                    switch response.result {
                    case let .success(data):
                        let end = DispatchTime.now()   // <<<<<<<<<<   end time

                        // Swift.print("")
                        let d = data as! [String: Any]
                        let success = d["success"] as! String
                        if success == "true"
                        {
                            // Swift.print("data: \(data)")
                            
                            let start =  self.faceRecognitionStartTimes![service] //
                            let nanoTime = end.uptimeNanoseconds - start!.uptimeNanoseconds  //
                            let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests
                            
                            promise.succeed(value: d as [String : AnyObject])  //
                            
                            Swift.print("••• FaceRecognition time: \(timeInterval)")
                            
                            SKToast.show(withMessage: "FaceRecognition  time: \(timeInterval) result: \(data)")
                            
                        //    let msg = "FaceRecognized" + service    // JT 19.02.04
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "FaceRecognized"), object: d)   //  doNextFaceRecognition "FaceRecognized"
                            
                            
                            let latency = String( format: "%4.3f", timeInterval * 1000 ) //  ms
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: postMsg), object: latency)     //  post latency
                        }
                        else
                        {
                            Swift.print("FaceRecognition failed")
                        }
                        
                    case let .failure(error):
                        print(error)
                        SKToast.show(withMessage: "FaceRecognition Failed: \(error)")
                        promise.fail(error: error)
                    }
            }
            
            // debugPrint(requestObj) // dump curl
            // Swift.print("")
        }
        
        return promise.future
    }
}



func convertPointsToRect(_ a:[Int])  ->CGRect    //   Mex data
{
    let r = CGRect(CGFloat(a[0]), CGFloat(a[1]), CGFloat(a[2] - a[0]), CGFloat(a[3] - a[1])) // face rect
    
    return r
}

// MARK:-

func getNetworkLatencyEdge()
{
    getNetworkLatency( DEF_FACE_HOST_EDGE, post: "latencyEdge")
}

func getNetworkLatencyCloud()   // JT 18.12.27 ? broken?
{
    getNetworkLatency( DEF_FACE_HOST_CLOUD, post: "latencyCloud")
}



func getNetworkLatency(_ hostName:String, post name: String)    // JT 19.01.14
{
    //Swift.print("\(#function) \(hostName)")
    
    // Ping once
    let  pingOnce = SwiftyPing(host: hostName, configuration: PingConfiguration(interval: 0.5, with: 5), queue: DispatchQueue.global())
    pingOnce?.observer = { (_, response) in
        let duration = response.duration
       // print(duration)   // JT 19.01.28
        pingOnce?.stop()
        
        let latency = response.duration * 1000    // JT 19.01.14
 
    //     print("\(hostName) latency (ms): \(latency)")

        
        let latencyMsg = String( format: "%4.2f", latency )
        
        NotificationCenter.default.post(name: NSNotification.Name(rawValue: name), object: latencyMsg)
    }
    pingOnce?.start()   // JT 19.01.14
    
    
//    PlainPing.ping(hostName, withTimeout: 1.0, completionBlock:
//        { (timeElapsed: Double?, error: Error?) in
//
//        if let latency = timeElapsed
//        {
//            let latencyMsg = String(format: "%4.3f", latency)
//
//            NotificationCenter.default.post(name: NSNotification.Name(rawValue: name), object: latencyMsg)
//        }
//        if let error = error
//        {
//            Swift.print("ping failed: \(hostName)")
//            print("ping error: \(error.localizedDescription)")
//        }
//
//    })

    
}

