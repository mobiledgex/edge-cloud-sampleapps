// Sample Client of SDK
//
//  Sample_client.swift
//  MobiledgeXSDKDemo.IOS
//
//  Port from cpp SDK demo to swift by Jean Tantra, Metatheory
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Alamofire
import Foundation
import UIKit
import GoogleMaps   // JT 19.01.29
import Security
import CoreLocation
import MapKit  

import NSLogger // JT 19.01.07
import MatchingEngineSDK    // JT 19.01.29
import MatchingEngineSDK.Swift // JT 19.01.29
//import MexSDK   // JT 19.01.29

// ----------------------------------------
//var locationManager: CLLocationManager! // JT 19.01.29 tmp

private var locationRequest: LocationRequest? // so we can stop updates




// This file Handles Events from:
//  Menu:
//  "Register Client",  // these first two are done actomicaly at launch
//  "Get App Instances",    // displays network POIs on map
//
//  "Verify Location",      // visual feedback: gray, green, failed: red
//  "Find Closest Cloudlet",    // animate Closest Cloudlet to center
//  "Reset Location",   // animate userMarker back to its gps location


// MARK: MexUtil
// common
//


private class MexUtil   // common to Mex... below
{
    static let shared = MexUtil()   // singleton

    //var sessionManager: SessionManager?  // persist

    // url  //  dme.mobiledgex.net:38001
    let baseDmeHost: String = "dme.mobiledgex.net"
    let dmePort: UInt = 38001

    let carrierNameDefault_TDG: String = "TDG"
//    let carrierNameDefault_mexdemo: String = "mexdemo"

    var baseDmeHostInUse: String = "TDG"   // baseDmeHost
    var carrierNameInUse: String = "mexdemo"   // carrierNameDefault_mexdemo

    // API Paths:   See Readme.txt for curl usage examples
    let registerAPI: String = "/v1/registerclient"
    let appinstlistAPI: String = "/v1/getappinstlist"
    let findcloudletAPI: String = "/v1/findcloudlet"
    
    
    // API: facedetection.defaultcloud.mobiledgex.net:8008
    // API: facedetection.defaultedge.mobiledgex.net:8008

    let faceServerPort: String = "8008" // JT 18.12.20 was "8000"

    let DEF_FACE_HOST_CLOUD = "facedetection.defaultcloud.mobiledgex.net"   // JT 18.12.20
 
    let DEF_FACE_HOST_EDGE = "facedetection.defaultedge.mobiledgex.net" // JT 18.12.20


    let timeoutSec: UInt64 = 5000   //  unused todo

//    var appName = "" // Your application name. was "EmptyMatchEngineApp"
//    var devName = "" // Your developer name
//
//    let appVersionStr = "1.0"   // used by createRegisterClientRequest
//
//    let headers: HTTPHeaders = [
//        "Accept": "application/json",
//        "Content-Type": "application/json", // This is the default
//        "Charsets": "utf-8",
//    ]

     var closestCloudlet = ""

    
    private init()  //   singleton called as of first access to shared
    {
        baseDmeHostInUse = baseDmeHost  // dme.mobiledgex.net

  //      carrierNameInUse = carrierNameDefault_mexdemo   // mexdemo

        MexSDK.getShared().appName = "MobiledgeX SDK Demo" // Your application name
        MexSDK.shared.devName = "MobiledgeX SDK Demo"  // Your developer name
    }
    

    // Retrieve the carrier name of the cellular network interface.
    func getCarrierName() -> String
    {
        return carrierNameInUse
    }

    func generateDmeHostPath(_ carrierName: String) -> String
    {
        if carrierName == ""
        {
            return carrierNameInUse + "." + baseDmeHostInUse
        }
        return carrierName + "." + baseDmeHostInUse
    }

    func generateBaseUri(_ carrierName: String, _: UInt) -> String
    {
        return "https://\(generateDmeHostPath(carrierName)):\(dmePort)"
    }
    

}

// MARK: -
// MARK: MexRegisterClient
//  gets sessioncookie  used by getApps and verifyLoc
//
class MexRegisterClient
{
    static let shared = MexRegisterClient()     // singleton

    public var future: Future<[String: AnyObject], Error>? // async result (captured by async?)
 
    var futureEdge: Future<[String: AnyObject], Error>? // async result (captured by async?)
    var futureCloud: Future<[String: AnyObject], Error>? // async result (captured by async?)

    var tokenserveruri = "" // set by RegisterClient
    var sessioncookie = ""  // set by RegisterClient    // used by getApp and verifyLoc
    
    // Color of user todo
    public static let COLOR_NEUTRAL: UInt32 = 0xFF67_6798   // default
    public static let COLOR_VERIFIED: UInt32 = 0xFF00_9933
    public static let COLOR_FAILURE: UInt32 = 0xFFFF_3300
    public static let COLOR_CAUTION: UInt32 = 0xFF00_B33C // Amber: ffbf00;

    private init() // singleton
    {

    } // init

    deinit
    {
        NotificationCenter.default.removeObserver(self)
    }
    
    

    func getLocaltionUpdates()
    {
        Swift.print("\(#function)")

        resetUserLocation(true)
    }

    // MARK:

    
    // Sets: sessioncookie, tokenserveruri
    
    func registerClientResult(_ registerClientReply: [String: Any])
    {
        if registerClientReply.count == 0
        {
            Swift.print("REST RegisterClient Error: NO RESPONSE.")
        }
        else
        {
            let line1 = "\nREST RegisterClient Status: \n"
            let ver = registerClientReply["Ver"] as? NSNumber
            let line2 = "Version: " + "\(ver!)"
            let line3 = ",\n Client Status:" + (registerClientReply["Status"] as! String)
            let line4 = ",\n SessionCookie:" + (registerClientReply["SessionCookie"] as! String)
            
            Swift.print(line1 + line2 + line3 + line4 + "\n\n")
            
            Swift.print("Token Server URI: " + (registerClientReply["TokenServerURI"] as! String) + "\n")

            Swift.print("")
        }
    }
    
    
    func createRegisterClientRequest()
        -> [String: Any] // Dictionary/json
    {
        let u = MexSDK.shared   // JT 18.12.26
        
        var regClientRequest = [String: String]()     // Dictionary/json regClientRequest
        
        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = u.appName
        regClientRequest["DevName"] = u.devName
        regClientRequest["AppVers"] = u.appVersionStr
        
        return regClientRequest
    }
    
    func createRegisterClientRequest( ver: String = "1", appName: String, devName: String, appVers: String)
        -> [String: Any] // Dictionary/json
    {
        var regClientRequest = [String: String]()     // Dictionary/json regClientRequest
        
        regClientRequest["ver"] = ver   // JT 19.01.31
        regClientRequest["AppName"] = appName
        regClientRequest["DevName"] = devName
        regClientRequest["AppVers"] = appVers
        
        return regClientRequest
    }

    
    
    // MARK: registerClientNow
    
    func registerClientNow() // called by top right menu
    {
        Swift.print("registerClientNow")    // log
        Swift.print("Register MEX client.") // log
        Swift.print("====================\n")   // log
        
        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        Swift.print("\(baseuri)")   // log
        
        // 🔵 API
        let registerClientRequest = MexRegisterClient.shared.createRegisterClientRequest()
        
        let urlStr = baseuri + MexUtil.shared.registerAPI
        
         let future =
MexSDK.shared.postRequest(urlStr, registerClientRequest, "RegisterClient1")
        
        MexRegisterClient.shared.future = future    // JT 19.01.30

        MexRegisterClient.shared.future!.on(
            success:
            {
                print("RegisterClient1 received value: \($0)")
                
                let registerClientReply = $0 as [String: Any]
                
                self.sessioncookie = (registerClientReply["SessionCookie"] as! String)  // save for later
                
                 self.tokenserveruri = (registerClientReply["TokenServerURI"] as! String) // save for later

                
                Logger.shared.log(.network, .info,   " save sessioncookie for later: \n \(self.sessioncookie)  \n" )    // Log

                Logger.shared.log(.network, .info,   " save tokenserveruri for later: \n \(self.tokenserveruri)  \n" )    // Log

                MexRegisterClient.shared.registerClientResult(registerClientReply) // Log
                
                MexRegisterClient.shared.getLocaltionUpdates()
                
                SKToast.show(withMessage: "Client registered")  // UI
                
                ////Log.logger.name = "RegisterClient"    // Log
                ////logw("\nRegisterClient result: \(registerClientReply)")   // Log
        },
            failure: { print("RegisterClient failed with error: \($0)")
                
        },
            completion: { let _ = $0 //print("completed with result: \($0)")
                
        }
        )
    }
    
    func registerClientThenGetInstApps() // called on launch
    {
        Swift.print("Register MEX client  then getInstApps.")
        Swift.print("====================\n")

        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        Swift.print("\(baseuri)")

        let registerClientRequest = MexSDK.shared.createRegisterClientRequest(
            appName: MexSDK.shared.appName,
            devName: MexSDK.shared.devName,
            appVers: MexSDK.shared.appVersionStr
        ) // JT 18.12.26

        let urlStr = baseuri + MexUtil.shared.registerAPI
        MexRegisterClient.shared.future = MexSDK.shared.postRequest(urlStr, registerClientRequest, "RegisterClient1")

        MexRegisterClient.shared.future!.on(
            success:
            {
                print("RegisterClient1 received value: \($0)")

                let registerClientReply = $0 as [String: Any]

                self.sessioncookie = (registerClientReply["SessionCookie"] as! String)
                self.tokenserveruri = (registerClientReply["TokenServerURI"] as! String)
                
                
                Logger.shared.log(.network, .info,   " save sessioncookie for later: \n \(self.sessioncookie)  \n" )    // JT 19.01.05
                
                Logger.shared.log(.network, .info,   " save tokenserveruri for later: \n \(self.tokenserveruri)  \n" )    // JT 19.01.05

                MexRegisterClient.shared.registerClientResult(registerClientReply)

                MexRegisterClient.shared.getLocaltionUpdates()

                SKToast.show(withMessage: "Client registered")

                MexGetAppInst.shared.getAppInstNow()
            },
            failure: { print("failed with error: \($0)") },
            completion: { _ = $0 // print("completed with result: \($0)")
            }
        )
    }
    

}


// MARK: -
// MARK: MexGetAppInst

class MexGetAppInst
{
    static let shared = MexGetAppInst()
    
    
    private init()  // singleton
    {
        
    }
    
    deinit
    {
//        NotificationCenter.default.removeObserver(self)
    }
    
    
    
    func getAppInstNow()    // called by top right menu
    {
        Swift.print("GetAppInstList")
        Swift.print(" MEX client.")
        Swift.print("====================\n")
        
        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        Swift.print("\(baseuri)")
        
        let loc = retrieveLocation()
        
        let getAppInstListRequest = MexGetAppInst.shared.createGetAppInstListRequest(MexUtil.shared.carrierNameDefault_TDG, loc)
        
       //// Log.logger.name = "GetAppInstlist"
       //// logw("\n getAppInstListRequest:\n \(getAppInstListRequest)")    // JT 18.12.27 todo remove logging
      
        // 🔵
        let urlStr = baseuri + MexUtil.shared.appinstlistAPI
        MexRegisterClient.shared.future =    MexSDK.shared.postRequest(urlStr, getAppInstListRequest, "GetAppInstlist1")
        
        MexRegisterClient.shared.future!.on(success:
            {
                print("GetAppInstlist1 received value: \($0)")
                
                let d = $0 as [String: Any]
                
                MexGetAppInst.shared.processAppInstList(d)
                
              ////  Log.logger.name = "GetAppInstlist"  // JT 19.01.28
              ////  logw("\n GetAppInstlist result: \(d)") // JT 18.12.27 todo remove logging   // JT 19.01.28
                
        },
            failure: { print("failed with error: \($0)") },
            completion: { let _ = $0    //print("completed with result: \($0)" )
                
        })
        
    }
    
    func processAppInstList(_ d: [String: Any] )
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
                    let portsDic = ports[0] as!  [String: Any]    // JT 19.01.29
                    
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
    
    // Carrier name can change depending on cell tower.
    func createGetAppInstListRequest(_ carrierName: String, _ gpslocation: [String: Any]) -> [String: Any]
    {
        //   json findCloudletRequest;
        var appInstListRequest = [String: Any]()    // Dictionary/json
        
        appInstListRequest["vers"] = 1
        appInstListRequest["SessionCookie"] = MexRegisterClient.shared.sessioncookie
        appInstListRequest["CarrierName"] = carrierName
        appInstListRequest["GpsLocation"] = gpslocation
        
        return appInstListRequest
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


// MARK: -
// MARK: MexVerifyLocation

class MexVerifyLocation
{
    static let shared = MexVerifyLocation()
    var future:Future<[String: AnyObject], Error>? // async result (captured by async?)

    private init()  // singleton
    {
        
    }
    
    func doVerifyLocation() // call by top right menu
    {
        // Produces a new request. Now with sessioncooke and token initialized.
        
        Swift.print("Verify Location of this Mex client.")
        Swift.print("===================================\n\n")
        
        //  // JT 18.12.28
//        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.carrierNameInUse, MexUtil.shared.dmePort)
//        let loc = retrieveLocation()
//
//        let verifyLocationRequest = createVerifyLocationRequest(MexUtil.shared.carrierNameInUse, loc, "", sessioncookie: MexRegisterClient.shared.sessioncookie)    // JT 18.12.26
//
//        VerifyLocation(baseuri, verifyLocationRequest)
        
        if MexRegisterClient.shared.tokenserveruri.count == 0   // JT 18.12.28
        {
            Swift.print("TokenURI is empty!")
            //     let empty = [String: Any]()
            return
        }
        else
        {
            Swift.print("VerifyLocation getToken \(MexRegisterClient.shared.tokenserveruri)") // todo
            getToken(MexRegisterClient.shared.tokenserveruri) // async, will call verify after getting token
        }

    }
    
    
    func createVerifyLocationRequest(_ carrierName: String,
                                             _ gpslocation: [String: Any],
                                             _ verifyloctoken: String,
                                             sessioncookie: String = MexRegisterClient.shared.sessioncookie)
        -> [String: Any]
    {
        var verifyLocationRequest = [String: Any]() // Dictionary/json
        
        verifyLocationRequest["ver"] = 1
        verifyLocationRequest["SessionCookie"] = sessioncookie
        verifyLocationRequest["CarrierName"] = carrierName
        verifyLocationRequest["GpsLocation"] = gpslocation
        verifyLocationRequest["VerifyLocToken"] = verifyloctoken
        
        return verifyLocationRequest
    }
    
    
    // string formatted json args and reply.
    private func VerifyLocation(_: String, _: [String: Any])
    {
        if MexRegisterClient.shared.tokenserveruri.count == 0
        {
            Swift.print("TokenURI is empty!")
            //     let empty = [String: Any]()
            return
        }
        else
        {
            Swift.print("VerifyLocation getToken \(MexRegisterClient.shared.tokenserveruri)") // todo
            
            if MexRegisterClient.shared.tokenserveruri.count == 0
            {
                Swift.print("No URI to get token!")
                return // nil;
            }
            
           // getToken(MexRegisterClient.shared.tokenserveruri) // async, will call verify after getting token
            
            future = MexSDK.shared.postRequest(MexRegisterClient.shared.tokenserveruri, [String: Any](), "GetToken") // async
            // NOTE special case: "GetToken" fails and its error result is parsed
            
            future!.on(
                success:
                {
                    print("GetToken received value: \($0)")
                    
                    let d = $0 as [String: Any]
                    let verifyLocToken = d["token"] as! String
                    
                    Logger.shared.log(.network, .info,   " got verifyLocToken: \n \(verifyLocToken) used by verifyLocation  \n" )    // JT 19.01.05

                    self.verifyLocation(verifyLocToken) // chain VerifyLocToken
                    
            },
                failure: { print("GetToken failed with error: \($0)") },
                completion: { _ = $0 // print("completed with result: \($0)")
            }
            )

        }
    }
    
    
    private func getToken(_ uri: String)  // async
    {
        Swift.print("In Get Token")
        if uri.count == 0
        {
            Swift.print("No URI to get token!")
            return // nil;
        }

        Swift.print("\(uri)")

        future = MexSDK.shared.postRequest(uri, [String: Any](), "GetToken") // async
        // NOTE special case: "GetToken" fails and its error result is parsed

        future!.on(
            success:
            {
                print("GetToken received value: \($0)")

                let d = $0 as [String: Any]
                
                let verifyLocToken = d["token"] as! String
                
                Logger.shared.log(.network, .info,   " got verifyLocToken: \n \(verifyLocToken) used by verifyLocation  \n" )    // JT 19.01.05
                self.verifyLocation(verifyLocToken)

            },
            failure: { print("failed with error: \($0)") },
            completion: { _ = $0 // print("completed with result: \($0)")
            }
        )
    }
    
    func verifyLocation(_ tokenReply: String)
    {
        Swift.print("VerifyLocation: Retrieved token: [ \(tokenReply) ]")
        
        let loc = retrieveLocation()
        
        var verifyLocationRequest = createVerifyLocationRequest(MexUtil.shared.carrierNameInUse, loc, "")
        
        // Update request with the new token:
        // json tokenizedRequest;
       var tokenizedRequest = [String: Any]()  // Dictionary/json
         tokenizedRequest += verifyLocationRequest  // Dictionary/json

        tokenizedRequest["VerifyLocToken"] = tokenReply
        
        
        Swift.print("Verifylocation actual call...")
        
        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.carrierNameInUse, MexUtil.shared.dmePort)
        let verifylocationAPI: String = "/v1/verifylocation"
        
        let uri = baseuri + verifylocationAPI
        
//        Log.logger.name = "Verifylocation"
//        logw("\n VerifylocationRequest url:\n \(uri)")
//        logw("\n VerifylocationRequest:\n \(tokenizedRequest)")
        
        // 🔵
        self.future = MexSDK.shared.postRequest(uri, tokenizedRequest, "Verifylocation1")
        
        self.future!.on(success: { print("Verifylocation1 received value: \($0)")
            let d = $0 as [String: Any]
//            Log.logger.name = "Verifylocation"
//            logw("\n VerifylocationResult:\n \(d)")    //  to file and console
            
            // Swift.print("Verifylocation1 result \(d)")
            
            SKToast.show(withMessage: "Verifylocation result: \(d)")
            
            
            let image =  makeUserMakerImage(MexRegisterClient.COLOR_VERIFIED)
            userMarker!.icon = image
        },
        failure: { print("Verifylocation failed with error: \($0)")
            
            let image =  makeUserMakerImage(MexRegisterClient.COLOR_FAILURE)
            
            userMarker!.icon = image
        },
        completion: {    let _ = $0 //print("completed with result: \($0)")
            

        })
    }
}




// MARK: -
// MARK: findNearestCloudlet

class MexFindNearestCloudlet
{
    static let shared = MexFindNearestCloudlet()
    
    private init()  //   singleton called as of first access to shared
    {
     }

    func findNearestCloudlet() //   called by top right menu
    {
        Swift.print("Finding nearest Cloudlet appInsts matching this Mex client.")
        Swift.print("===========================================================")

        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        let loc = retrieveLocation()

        let findCloudletRequest = createFindCloudletRequest(MexUtil.shared.carrierNameDefault_TDG, loc)

        let urlStr = baseuri + MexUtil.shared.findcloudletAPI
//        Log.logger.name = "FindCloudlet"
//       logw("\n findCloudlet url\n \(urlStr)")
//        logw("\n findCloudletRequest:\n \(findCloudletRequest)")

        // 🔵 API
        MexRegisterClient.shared.future = MexSDK.shared.postRequest(urlStr, findCloudletRequest, "FindCloudlet1")

        MexRegisterClient.shared.future!.on( // JT 19.01.30
            success:
            {
                print("GetToken received value: \($0)")

                let d = $0 as [String: Any]

//                Log.logger.name = "FindCloudlet"
//                logw("\n FindCloudlet result:\n \(d)")

                self.processFindCloudletResult(d)

            },
            failure: { print("failed with error: \($0)") },
            completion: { _ = $0 // print("completed with result: \($0)")
            }
        )
    }

    // Carrier name can change depending on cell tower.
    func createFindCloudletRequest(_ carrierName: String, _ gpslocation: [String: Any], sessioncookie: String = MexRegisterClient.shared.sessioncookie) -> [String: Any]
    {
        //    findCloudletRequest;
        var findCloudletRequest = [String: Any]() // Dictionary/json

        findCloudletRequest["vers"] = 1
        findCloudletRequest["SessionCookie"] = sessioncookie
        findCloudletRequest["CarrierName"] = carrierName
        findCloudletRequest["GpsLocation"] = gpslocation

        return findCloudletRequest
    }

    func processFindCloudletResult(_ d: [String: Any])
    {
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
}


// MARK: -
// MARK: resetUserLocation

func resetUserLocation(_ show: Bool) // called by "Reset user location" menu
{
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

private func retrieveLocation() -> [String: Any]
{
    var location:[String: Any] = [ "latitude": -122.149349, "longitude": 37.459609] //     //  json location, somewhere

    if userMarker != nil // get app isnt sets userMarker
    {
        location["latitude"] = userMarker!.position.latitude
        location["longitude"] = userMarker!.position.longitude
    }

    return location
}

// MARK: -

class MexFaceRecognition
{
    var faceDetectionStartTimes:[String:DispatchTime]? // two at a time cloud/edge
    var faceRecognitionStartTimes:[String:DispatchTime]? // two at a time cloud/edge

    var faceRecognitionImages2 =  [(UIImage,String)]()  // JT 19.01.16 image + service. one at a time
    
   var faceRecognitionCurrentImage: UIImage?

    
    // Mark: -
    // Mark: FaceDetection
    
    func FaceDetection(_ image: UIImage?, _ service: String)     
    {
        let broadcast =  "FaceDetectionLatency" + service
        
        let faceDetectionFuture = FaceDetectionCore(image, service, post: broadcast)
        
        faceDetectionFuture.on(
            success:
            {
               // print("FaceDetection received value: \($0)")
                
                let reply = $0 as [String: Any]
                
                let postName = "faceDetected" + service
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName ) , object: reply) //  draw blue rect around
                
                SKToast.show(withMessage: "FaceDetection ")
                
                let tv = UserDefaults.standard.bool(forKey: "doFaceRecognition") //
                
                if tv && doAFaceRecognition
                {
                    doAFaceRecognition = false // wait for que to empty
                    
                    self.faceRecognitionImages2.removeAll()
                    
                    if false   //  use whole image
                    {
                        let rects = reply["rects"] as! [[Int]]

                        for a in rects
                        {
                             let r = convertPointsToRect(a)

                            Swift.print("r = \(r)")
                            let face = image!.cropped(to: r)
                            self.faceRecognitionImages2.append((face, "Cloud")) // JT 19.01.16
                            self.faceRecognitionImages2.append((face, "Edge"))  // JT 19.01.16

                        }
                    }
                    else
                    {
                        self.faceRecognitionImages2.append((image!, "Cloud") )    //   use whole image  // JT 19.01.16
                        self.faceRecognitionImages2.append((image!, "Edge")) // JT 19.01.16 )    //   use whole image

                    }
                    
                    Swift.print("")
                    self.doNextFaceRecognition()    // JT 19.01.16
                }
                
                // Log.logger.name = "FaceDetection"
                // logw("\FaceDetection result: \(registerClientReply)")
        },
            failure: { print("FaceDetection failed with error: \($0)")
                
        },
            completion: { _ = $0 // print("completed with result: \($0)")
        }
        )
    }
    
    //  todo? pass in host
    
    func FaceDetectionCore(_ image: UIImage?,  _ service: String, post broardcastMsg: String?)     -> Future<[String: AnyObject], Error>
    {
        let promise = Promise<[String: AnyObject], Error>()
        
        // detector/detect
        // Used to send a face image to the server and get back a set of coordinates for any detected faces.
        // POST http://<hostname>:8008/detector/detect/
        
        let faceDetectionAPI: String = "/detector/detect/"
        
        //    Swift.print("FaceDetection")
        //    Swift.print("FaceDetection MEX .")
        //    Swift.print("====================\n")
        //
        
        getNetworkLatency(MexUtil.shared.DEF_FACE_HOST_EDGE, post: "updateNetworkLatenciesEdge")
        getNetworkLatency(MexUtil.shared.DEF_FACE_HOST_CLOUD, post: "updateNetworkLatenciesCloud")  // JT 19.01.16
        
        let _ = GetSocketLatency( MexUtil.shared.DEF_FACE_HOST_CLOUD, Int32(MexUtil.shared.faceServerPort)!, "latencyCloud")   // JT 19.01.16
        
        
        let baseuri = (service == "Cloud" ? MexUtil.shared.DEF_FACE_HOST_CLOUD  : MexUtil.shared.DEF_FACE_HOST_EDGE) + ":" + MexUtil.shared.faceServerPort  // JT 19.01.16
        
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

            
        //    faceDetectionStartTime = DispatchTime.now() // <<<<<<<<<< Start time  // JT 19.01.16
            
            if faceDetectionStartTimes == nil   // JT 19.01.16
            {
                faceDetectionStartTimes = [String:DispatchTime]()
            }
            faceDetectionStartTimes![service] =  DispatchTime.now() // JT 19.01.16
            
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
                        
                        // Swift.print("")---
                        print("!", terminator:"")   // JT 19.01.28

                        let d = data as! [String: Any]
                        let success = d["success"] as! String
                        if success == "true"
                        {
                            // Swift.print("data: \(data)")
                            
                            let end = DispatchTime.now() // <<<<<<<<<<   end time
                            let start =  self.faceDetectionStartTimes![service] // JT 19.01.16
                            let nanoTime = end.uptimeNanoseconds - start!.uptimeNanoseconds  //self.faceDetectionStartTime!.uptimeNanoseconds // <<<<< Difference in nano seconds (UInt64)
                            

                            
                            let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests
                            
                           // Swift.print("FaceDetection time: \(timeInterval)")
                            SKToast.show(withMessage: "FaceDetection  time: \(timeInterval) result: \(data)")
                            
                            let aa = d["rects"]
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "FaceDetection"), object: aa) //   draw blue rect around face  [[Int]]
                            
                            promise.succeed(value: d as [String : AnyObject])
                            
                            let latency = String(format: "%4.3f", timeInterval * 1000)
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: broardcastMsg!), object: latency)
                        }
                        else
                        {
                            let tv =  UserDefaults.standard.bool(forKey: "doFaceRecognition") //
                            if tv == false
                            {
                                doAFaceDetection = true // atomic, one at a time
                            }//next
                        }
                        
                    case let .failure(error):
                        print(error)
                        //  SKToast.show(withMessage: "FaceDetection Failed: \(error)")
                        promise.fail(error: error)
                        print("~", terminator:"")   // JT 19.01.28

                    }
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
        if faceRecognitionImages2.count == 0
        {
            doAFaceDetection = true //  todo tmp
            doAFaceRecognition = true
            
            print(":", terminator:"")   // JT 19.01.28

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
        -> Future<[String: AnyObject], Error>
    {
        let promise = Promise<[String: AnyObject], Error>()
        
       // Logger.shared.log(.network, .info, image! )      // JT 19.01.16

        // detector/detect
        // Used to send a face image to the server and get back a set of coordinates for any detected faces.
        // POST http://<hostname>:8008/detector/detect/
        
        let faceRecognitonAPI: String = "/recognizer/predict/"
        
        //    Swift.print("FaceRecogniton")
        //    Swift.print("FaceRecogniton MEX .")
        //    Swift.print("====================\n")
        
        
        let postMsg =  "faceRecognitionLatency" + service
        let baseuri = (service ==  "Cloud" ? MexUtil.shared.DEF_FACE_HOST_CLOUD : MexUtil.shared.DEF_FACE_HOST_EDGE)   + ":" + MexUtil.shared.faceServerPort  //
        
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
            
       //     faceRecognitionStartTime = DispatchTime.now() // <<<<<<<<<< Start time    // JT 19.01.16
    
            if faceRecognitionStartTimes == nil   // JT 19.01.16 LIT hack
            {
                faceRecognitionStartTimes = [String:DispatchTime]()
            }
            faceRecognitionStartTimes![service] =  DispatchTime.now() // JT 19.01.16
            
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
                        
                        // Swift.print("")
                        let d = data as! [String: Any]
                        let success = d["success"] as! String
                        if success == "true"
                        {
                            // Swift.print("data: \(data)")
                            
                            let end = DispatchTime.now()   // <<<<<<<<<<   end time
                            let start =  self.faceRecognitionStartTimes![service] // JT 19.01.16
                            let nanoTime = end.uptimeNanoseconds - start!.uptimeNanoseconds  //
                            let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests
                            
                            promise.succeed(value: d as [String : AnyObject])  //
                            
                            Swift.print("••• FaceRecognition time: \(timeInterval)")
                            
                            SKToast.show(withMessage: "FaceRecognition  time: \(timeInterval) result: \(data)")
                            
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "FaceRecognized"), object: d)   //  doNextFaceRecognition
                            
                            
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
    getNetworkLatency( MexUtil.shared.DEF_FACE_HOST_EDGE, post: "latencyEdge")
}

func getNetworkLatencyCloud()   // JT 18.12.27 ? broken?
{
    getNetworkLatency( MexUtil.shared.DEF_FACE_HOST_CLOUD, post: "latencyCloud")
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

