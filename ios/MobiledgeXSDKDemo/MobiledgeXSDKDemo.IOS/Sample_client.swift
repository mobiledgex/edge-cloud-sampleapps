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
import GoogleMaps
import Security
import PlainPing

// ----------------------------------------

private var locationRequest: LocationRequest? // so we can stop updates




// This file Handles:
//  Menu:
//  "Register Client",  // these first two are done actomicaly at launch
//  "Get App Instances",    // displays POIs
//  "Verify Location",
//  "Find Closest Cloudlet",
//  "Reset Location",

// MARK: MexUtil
// common
//


private class MexUtil   // common to Mex... below
{
    static let shared = MexUtil()   // singleton

    var sessionManager: SessionManager?  // persist

    // url
    let baseDmeHost: String = "dme.mobiledgex.net"
    let dmePort: UInt = 38001

    let carrierNameDefault_TDG: String = "TDG"
    let carrierNameDefault_mexdemo: String = "mexdemo"

    var baseDmeHostInUse: String = ""
    var carrierNameInUse: String = ""

    // API Paths:   See Readme.txt for curl usage examples
    let registerAPI: String = "/v1/registerclient"
    let appinstlistAPI: String = "/v1/getappinstlist"
    let findcloudletAPI: String = "/v1/findcloudlet"
    
    let faceServerPort: String = "8008" // JT 18.12.20 was "8000"

    let DEF_FACE_HOST_CLOUD = "facedetection.defaultcloud.mobiledgex.net"   // JT 18.12.20
 
    let DEF_FACE_HOST_EDGE = "facedetection.defaultedge.mobiledgex.net" // JT 18.12.20


    let timeoutSec: UInt64 = 5000   // JT 18.12.03 unused todo

    var appName = "" // Your application name. was "EmptyMatchEngineApp"
    var devName = "" // Your developer name

    let appVersionStr = "1.0"   // used by createRegisterClientRequest

    let headers: HTTPHeaders = [
        "Accept": "application/json",
        "Content-Type": "application/json", // This is the default
        "Charsets": "utf-8",
    ]

     var closestCloudlet = ""       // JT 18.12.12

    
    private init()  //   singleton called as of first access to shared
    {
        baseDmeHostInUse = baseDmeHost

        carrierNameInUse = carrierNameDefault_mexdemo

        appName = "MobiledgeX SDK Demo" // "EmptyMatchEngineApp"; // Your application name
        devName = "MobiledgeX SDK Demo" // "EmptyMatchEngineApp"; // Your developer name
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
// MARK: postRequest

private func postRequest(_ uri: String,
                 _ request: [String: Any],  // Dictionary/json
                 _ postName: String = "postRequestReplyLogger") // this is posted after results
    -> Future<[String: AnyObject], Error> // JT 18.11.28
{
    let promise = Promise<[String: AnyObject], Error>()   // JT 18.11.28

    Swift.print("URI to post to:\n \(uri)\n")
    if postName != "FaceDetection"
    {
        Swift.print("\(request)")
        logw("•uri:\n\(uri)\n") // JT 18.11.26 log to file
    }

  //  logw("•request:\n\(request)\n") // JT 18.11.26 log to file

    dealWithTrustPolicy(uri) // certs
    Swift.print("==========\n")

    let requestObj = MexUtil.shared.sessionManager!.request(
        uri,
        method: .post,
        parameters: request,
        encoding: JSONEncoding.default,
        headers: MexUtil.shared.headers // JT 18.11.27
    ).responseJSON
    { response in
        if postName != "FaceDetection"  // JT 18.11.27
        {
            debugPrint("\n••\n\(response.request! )\n") // curl
        }

        guard response.result.isSuccess else
        {   // failed
          //  print("\nError while fetching remote -: \(String(describing: response.result.error))")

            let msg = String(describing: response.result.error)

            // hack parse error
            if msg.contains("dt-id=") // special case // && postName == "GetToken"
            { // not really an error
                let dtId = msg.components(separatedBy: "dt-id=")
                let s1 = dtId[1].components(separatedBy: ",")
                let token = s1[0]
                Swift.print("\(token)")
                
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName),  object: token)  // "GetToken"
                
                promise.succeed(value: ["token": token as AnyObject])    // JT 18.12.06
            }
            else
            {
                Swift.print("post error \(response.result.error!)")  // JT 18.12.16
            }
            return
        }

        switch response.result
        {
        case let .failure(error):
            Swift.print("\(error)")
            // Do whatever here
            // If error
            promise.fail(error: error)  // JT 18.11.28
            return

        case let .success(data):
            // First make sure you got back a dictionary if that's what you expect
            guard let json = data as? [String: AnyObject] else
            {
                Swift.print("json = data as? [String: AnyObject]  errorrrrr")
                return
            }
            Swift.print("=\(postName)=\n \(json)")

            // If success
            promise.succeed(value: json)  // JT 18.11.28

            NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName),
                                            object: json)
            
      //      Log.logger.name = "curl"  // JT 18.12.05 todo
            
            let curl = response.request.debugDescription    // JT 18.12.05
            logw("\n \(curl)\n")      // JT 18.12.05

            logw("postName:\(postName)\nresult:\n\(json)") // JT 18.11.26  log to file
        }

        Swift.print("\(response)")
        Swift.print("\(response.result)")
        Swift.print("\(response.data!)")

        //            print(response.metrics)
        //           print(response.timeline)
    }

    if postName != "FaceDetection"  // JT 18.11.27 too big
    {
        debugPrint(requestObj) // dump curl
    }
    
    return promise.future   // JT 18.11.28
}

// in general
//
private  func dealWithTrustPolicy(
    _ url: URLConvertible  // a string
)
{
    // let certificates = getCertificates()
    let certificates = ServerTrustPolicy.certificates() // alamo extension
    Swift.print("~~~ \(certificates) ---")

    let trustPolicy = ServerTrustPolicy.pinCertificates(
        certificates: certificates,
        validateCertificateChain: true,
        validateHost: true
    )

    do
    {
        let whoToTrust = try url.asURL().host
        //     Swift.print("\(whoToTrust)")

        let trustPolicies = [whoToTrust!: trustPolicy]

        let policyManager = ServerTrustPolicyManager(policies: trustPolicies)

        MexUtil.shared.sessionManager = SessionManager(
            configuration: .default,
            serverTrustPolicyManager: policyManager
        )
    }
    catch
    {
        Swift.print("dealWithTrustPolicy asURL throws: trust betraied")
    }
}



// MARK: -
// MARK: MexRegisterClient

class MexRegisterClient
{
    static let shared = MexRegisterClient()     // singleton

    var future:Future<[String: AnyObject], Error>? // async result (captured by async?)
 
    var futureEdge:Future<[String: AnyObject], Error>? // async result (captured by async?)
    var futureCloud:Future<[String: AnyObject], Error>? // async result (captured by async?)

    var tokenserveruri = "" // set by RegisterClient    // JT 18.11.25
    var sessioncookie = ""  // set by RegisterClient    // JT 18.11.25 used by getApp and verifyLoc
    
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
        Swift.print("\(#function)") // JT 18.12.16

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
            
          //  let sessioncookie = (registerClientReply["SessionCookie"] as! String)   // JT 18.12.07
            
            Swift.print("Token Server URI: " + (registerClientReply["TokenServerURI"] as! String) + "\n")
          //  let tokenserveruri = (registerClientReply["TokenServerURI"] as! String)
            Swift.print("")
        }
    }
    
    
    func createRegisterClientRequest()
        -> [String: Any] // Dictionary/json
    {
        let u = MexUtil.shared
        
        var regClientRequest = [String: String]()     // Dictionary/json regClientRequest
        
        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = u.appName
        regClientRequest["DevName"] = u.devName
        regClientRequest["AppVers"] = u.appVersionStr
        
        return regClientRequest
    }
    
    func createRegisterClientRequest( ver: String = "1", appName: String, devName: String, appVers: String) // JT 18.12.19
        -> [String: Any] // Dictionary/json
    {
        let u = MexUtil.shared
        
        var regClientRequest = [String: String]()     // Dictionary/json regClientRequest
        
        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = u.appName
        regClientRequest["DevName"] = u.devName
        regClientRequest["AppVers"] = u.appVersionStr
        
        return regClientRequest
    }

    
    
    // MARK: registerClientNow
    
    func registerClientNow() // called by top right menu
    {
        Swift.print("registerClientNow")
        Swift.print("Register MEX client.")
        Swift.print("====================\n")
        
        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        Swift.print("\(baseuri)")
        
        // 🔵
        let registerClientRequest = MexRegisterClient.shared.createRegisterClientRequest()
        
        let urlStr = baseuri + MexUtil.shared.registerAPI // JT 18.11.27
        MexRegisterClient.shared.future = postRequest(urlStr, registerClientRequest, "RegisterClient1") // JT 18.11.28 todo persist future
        
        MexRegisterClient.shared.future!.on(
            success:
            {
                print("RegisterClient1 received value: \($0)")
                
                let registerClientReply = $0 as [String: Any]    // JT 18.12.03
                
                self.sessioncookie = (registerClientReply["SessionCookie"] as! String)   // JT 18.12.07
                
                 self.tokenserveruri = (registerClientReply["TokenServerURI"] as! String)

                MexRegisterClient.shared.registerClientResult(registerClientReply)
                
                MexRegisterClient.shared.getLocaltionUpdates()
                
                SKToast.show(withMessage: "Client registered")
                
                Log.logger.name = "RegisterClient"
                logw("\nRegisterClient result: \(registerClientReply)")    // JT 18.12.04
        },
            failure: { print("failed with error: \($0)")
                
        },
            completion: { let _ = $0 //print("completed with result: \($0)")  // JT 18.12.04
                
        }
        ) // JT 18.11.28
    }
    
    func registerClientThenGetInstApps() // called on launch
    {
        Swift.print("registerClient then getInstApps")
        Swift.print("Register MEX client.")
        Swift.print("====================\n")
        
        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        Swift.print("\(baseuri)")
        
        let registerClientRequest = MexRegisterClient.shared.createRegisterClientRequest()
        
        let urlStr = baseuri + MexUtil.shared.registerAPI // JT 18.11.27
        MexRegisterClient.shared.future = postRequest(urlStr, registerClientRequest, "RegisterClient1")
        
        MexRegisterClient.shared.future!.on(
            success:
            {
                print("RegisterClient1 received value: \($0)")
                
                let registerClientReply = $0 as [String: Any]    // JT 18.12.03
                
                self.sessioncookie = (registerClientReply["SessionCookie"] as! String)   // JT 18.12.07
                
                self.tokenserveruri = (registerClientReply["TokenServerURI"] as! String)
                MexRegisterClient.shared.registerClientResult(registerClientReply)
                
                MexRegisterClient.shared.getLocaltionUpdates()
                
                SKToast.show(withMessage: "Client registered")
                
                MexGetAppInst.shared.getAppInstNow() //   chain // JT 18.12.06
        },
            failure: { print("failed with error: \($0)") },
            completion: { let _ = $0    //print("completed with result: \($0)")
                
        }
        )
    }
    

}


// MARK: -
// MARK: MexGetAppInst

class MexGetAppInst
{
    static let shared = MexGetAppInst()
    
    //var theRequest: DataRequest?  // JT 18.11.25
    
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
        
        Log.logger.name = "GetAppInstlist"
        logw("\n getAppInstListRequest:\n \(getAppInstListRequest)")    // JT 18.12.04
      
        // 🔵
        let urlStr = baseuri + MexUtil.shared.appinstlistAPI
        MexRegisterClient.shared.future =    postRequest(urlStr, getAppInstListRequest, "GetAppInstlist1")  // JT 18.11.27
        
        MexRegisterClient.shared.future!.on(success:
            {
                print("GetAppInstlist1 received value: \($0)")
                
                let d = $0 as [String: Any]    // JT 18.12.03
                
                MexGetAppInst.shared.processAppInstList(d)  // JT 18.12.03
                
                Log.logger.name = "GetAppInstlist"
                logw("\n GetAppInstlist result: \(d)")    // JT 18.12.04
                
        },
            failure: { print("failed with error: \($0)") },
            completion: { let _ = $0    //print("completed with result: \($0)" )
                
        })   // JT 18.11.28
        
    }
    
    func processAppInstList(_ d: [String: Any] )   // JT 18.12.03
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
                        latitude: Double((gps["lat"] as! NSNumber).stringValue)!,
                        longitude: Double((gps["long"] as! NSNumber).stringValue)!
                    )
                    Swift.print("\(loc)")
                    
                    Swift.print("\(Array(d.keys))\n")
                    Swift.print("d ••••• \(d)")
                    
                    let dd = d["Appinstances"] as! [[String: Any]]  // Dictionary/json
                    let uri = dd[0]["FQDN"] as! String // todo, now just use first
                    let appName = dd[0]["AppName"] as! String
                    
                    Swift.print("cloudlet uri: \(uri)")
                    Swift.print("dd \(dd)")
                    //    let loc2 = CLLocationCoordinate2D() //
                    
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
                    
                    marker?.icon = cloudletName.contains("microsoft") ? i2 : resized
                    
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
           // let padding: CGFloat  = 125.0 // offset from edges of the map in pixels
            
            //                let update = GMSCameraUpdate.fit(boundsBuilder, withPadding: 64)
            //                theMap!.moveCamera(update)
            
            theMap!.animate(with: .fit(boundsBuilder, withPadding: 64.0))
        }
        else
        {
            Swift.print("No cloudlets. Don't zoom in")
            
            //  cu = CameraUpdateFactory.newLatLng(mUserLocationMarker.getPosition());
            
            //      theMap!.animate(toLocation: marker!.position)   //  user position
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

func makeUserMakerImage(_ color: UInt32) -> UIImage    // JT 18.12.16
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
        Swift.print("\(findCloudletReply["FQDN"]!)")    // JT 18.12.16
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
        
        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.carrierNameInUse, MexUtil.shared.dmePort)
        let loc = retrieveLocation()
        
        let verifyLocationRequest = createVerifyLocationRequest(MexUtil.shared.carrierNameInUse, loc, "")
        
        VerifyLocation(baseuri, verifyLocationRequest)
    }
    
    
    func createVerifyLocationRequest(_ carrierName: String,
                                             _ gpslocation: [String: Any],
                                             _ verifyloctoken: String,
                                             _ sessioncookie: String = MexRegisterClient.shared.sessioncookie)  // JT 18.12.19
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
            getToken(MexRegisterClient.shared.tokenserveruri) // async
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

        future = postRequest(uri, [String: Any](), "GetToken") // async
        // NOTE special case: "GetToken" fails and its error result is parsed

        future!.on(
            success:
            {
                print("GetToken received value: \($0)")

                let d = $0 as [String: Any] // JT 18.12.06
                let tokenReply = d["token"] as! String // JT 18.12.06
                self.verifyLocation(tokenReply)

            },
            failure: { print("failed with error: \($0)") },
            completion: { _ = $0 // print("completed with result: \($0)") // JT 18.12.04
            }
        ) // JT 18.11.28
    }
    
    func verifyLocation(_ tokenReply: String)   // JT 18.12.06
    {
        
        Swift.print("VerifyLocation: Retrieved token: [ \(tokenReply) ]")
        
        let loc = retrieveLocation()
        
        var verifyLocationRequest = createVerifyLocationRequest(MexUtil.shared.carrierNameInUse, loc, "")    // JT 18.11.27
        
        // Update request with the new token:
        // json tokenizedRequest;
       var tokenizedRequest = [String: Any]()  // Dictionary/json
         tokenizedRequest += verifyLocationRequest  // Dictionary/json

//        tokenizedRequest["ver"] = verifyLocationRequest["ver"]
//        tokenizedRequest["SessionCookie"] = verifyLocationRequest["SessionCookie"]
//        tokenizedRequest["CarrierName"] = verifyLocationRequest["CarrierName"]
//        tokenizedRequest["GpsLocation"] = verifyLocationRequest["GpsLocation"]
        tokenizedRequest["VerifyLocToken"] = tokenReply
        
        
        Swift.print("Verifylocation actual call...")
        
        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.carrierNameInUse, MexUtil.shared.dmePort)  // JT 18.11.27
        let verifylocationAPI: String = "/v1/verifylocation"
        
        let uri = baseuri + verifylocationAPI
        
        Log.logger.name = "Verifylocation"
        logw("\n VerifylocationRequest url:\n \(uri)")    // JT 18.12.04
        logw("\n VerifylocationRequest:\n \(tokenizedRequest)")    // JT 18.12.04
        
        // 🔵
        self.future = postRequest(uri, tokenizedRequest, "Verifylocation1")  // JT 18.11.28
        
        self.future!.on(success: { print("Verifylocation1 received value: \($0)")
            let d = $0 as [String: Any]     // JT 18.12.04
            Log.logger.name = "Verifylocation"
            logw("\n VerifylocationResult:\n \(d)")    // JT 18.12.04 to file and console
            
            // Swift.print("Verifylocation1 result \(d)")
            
            SKToast.show(withMessage: "Verifylocation result: \(d)")    // JT 18.12.06
            
            
            let resized =  makeUserMakerImage(MexRegisterClient.COLOR_VERIFIED)  // JT 18.12.16
            userMarker!.icon = resized
        },
        failure: { print("Verifylocation failed with error: \($0)")
            
            let resized =  makeUserMakerImage(MexRegisterClient.COLOR_FAILURE)  // JT 18.12.16
            
            userMarker!.icon = resized
        },
        completion: {    let _ = $0 //print("completed with result: \($0)")  // JT 18.12.04
            

        })   // JT 18.11.28
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

        Log.logger.name = "FindCloudlet"
        let urlStr = baseuri + MexUtil.shared.findcloudletAPI
        logw("\n findCloudlet url\n \(urlStr)") // JT 18.12.04
        logw("\n findCloudletRequest:\n \(findCloudletRequest)") // JT 18.12.04

        // 🔵 API
        MexRegisterClient.shared.future = postRequest(urlStr, findCloudletRequest, "FindCloudlet1")

        MexRegisterClient.shared.future!.on(
            success:
            {
                print("GetToken received value: \($0)")

                let d = $0 as [String: Any] // JT 18.12.04

                Log.logger.name = "FindCloudlet"
                logw("\n FindCloudlet result:\n \(d)") // JT 18.12.04

                self.processFindCloudletResult(d) // JT 18.12.06

            },
            failure: { print("failed with error: \($0)") },
            completion: { _ = $0 // print("completed with result: \($0)")  // JT 18.12.04
            }
        ) // JT 18.11.28
    }

    // Carrier name can change depending on cell tower.
    func createFindCloudletRequest(_ carrierName: String, _ gpslocation: [String: Any], sessioncookie: String = MexRegisterClient.shared.sessioncookie) -> [String: Any]
    {
        //    findCloudletRequest;
        var findCloudletRequest = [String: Any]() // Dictionary/json

        findCloudletRequest["vers"] = 1
        findCloudletRequest["SessionCookie"] = sessioncookie    // JT 18.12.19
        findCloudletRequest["CarrierName"] = carrierName
        findCloudletRequest["GpsLocation"] = gpslocation

        return findCloudletRequest
    }

    func processFindCloudletResult(_ d: [String: Any]) // JT 18.12.04
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

                MexUtil.shared.closestCloudlet = v as! String // JT 18.11.27    // JT 18.12.12
                Swift.print("")
            }

            if cld.key == "cloudlet_location"
            {
                let dd = cld.value as! [String: Any] // Dictionary/json

                Swift.print("••• \(dd)")

                let loc = CLLocationCoordinate2D(
                    latitude: Double((dd["lat"] as! NSNumber).stringValue)!,
                    longitude: Double((dd["long"] as! NSNumber).stringValue)!
                )

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
    
    let resized =  makeUserMakerImage(MexRegisterClient.COLOR_NEUTRAL)  // JT 18.12.16
    
    userMarker!.icon = resized
    userMarker!.map = theMap
    
    userMarker!.isDraggable = true // drag to test spoofing
}

// MARK: -

// used by: GetToken, getAppInstNow, verify  loc

private func retrieveLocation() -> [String: Any]
{
    var location = [String: Any]() //     //  json location;

    if userMarker != nil // get app isnt sets userMarker
    {
        location["lat"] = userMarker!.position.latitude // -122.149349;
        location["long"] = userMarker!.position.longitude // 37.459609;
    }
    else
    {
        location["lat"] = -122.149349   //
        location["long"] = 37.459609
    }

    return location
}

// MARK: -

class MexFaceRecognition
{
     var faceDetectionStartTime:DispatchTime?    // JT 18.11.28 one at a time
    
     var faceRecognitionStartTime:DispatchTime?      // JT 18.12.10 one at a time
     var faceRecognitionImages =  [UIImage]()  // JT 18.12.10
     var faceRecognitionCurrentImage: UIImage?   // JT 18.12.10 todo refactor

    
    // Mark: -
    // Mark: FaceDetection
    
    func FaceDetection(_ image: UIImage?, _ service: String)     
    {
        let broadcast =  "FaceDetectionLatency" + service   // JT 18.12.16
        
        let faceDetectionFuture = FaceDetectionCore(image, service, post: broadcast)    // JT 18.12.16
        
        faceDetectionFuture.on(
            success:
            {
               // print("FaceDetection received value: \($0)")
                
                let reply = $0 as [String: Any] // JT 18.12.03
                
                let postName = "faceDetected" + service
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName ) , object: reply) // JT 18.12.08 draw blue rect around
                
                SKToast.show(withMessage: "FaceDetection ")
                
                let tv = UserDefaults.standard.bool(forKey: "doFaceRecognition") //
                
                if tv && doAFaceRecognition
                {
                    doAFaceRecognition = false // wait for que to empty
                    
                    self.faceRecognitionImages.removeAll() // JT 18.12.10
                    
                    if false   // JT 18.12.15 use whole image
                    {
                        let rects = reply["rects"] as! [[Int]] // JT 18.12.10

                        for a in rects
                        {
                             let r = convertPointsToRect(a)  // JT 18.12.13

                            Swift.print("r = \(r)") // JT 18.12.10
                            let face = image!.cropped(to: r)
                            self.faceRecognitionImages.append(face) // JT 18.12.10
                        }
                    }
                    else
                    {
                        self.faceRecognitionImages.append(image!)    // JT 18.12.15 use whole image
                    }
                    
                    Swift.print("")
                    self.doNextFaceRecognition(service) // JT 18.12.10
                }
                
                // Log.logger.name = "FaceDetection"
                // logw("\FaceDetection result: \(registerClientReply)")    // JT 18.12.04
        },
            failure: { print("FaceDetection failed with error: \($0)")
                
        },
            completion: { _ = $0 // print("completed with result: \($0)")  // JT 18.12.04
        }
        ) // JT 18.11.28
    }
    
    // JT 18.12.12 todo pass in host
    
    func FaceDetectionCore(_ image: UIImage?,  _ service: String, post broardcastMsg: String?)     -> Future<[String: AnyObject], Error> // JT 18.11.28
    {
        let promise = Promise<[String: AnyObject], Error>() // JT 18.11.28
        
        // detector/detect
        // Used to send a face image to the server and get back a set of coordinates for any detected faces.
        // POST http://<hostname>:8000/detector/detect/
        
        let faceDetectionAPI: String = "/detector/detect/"
        
        //    Swift.print("FaceDetection")
        //    Swift.print("FaceDetection MEX .")
        //    Swift.print("====================\n")
        //
        
        getNetworkLatency(MexUtil.shared.DEF_FACE_HOST_EDGE, post: "updateNetworkLatenciesEdge") // JT 18.12.12
 //       getNetworkLatency(MexUtil.shared.DEF_FACE_HOST_CLOUD, post: "updateNetworkLatenciesCloud") // JT 18.12.12 // JT 18.12.20
        
        let baseuri = (service == "Cloud" ? MexUtil.shared.DEF_FACE_HOST_EDGE : MexUtil.shared.DEF_FACE_HOST_CLOUD) + ":" + MexUtil.shared.faceServerPort
        
        let urlStr = "http://" + baseuri + faceDetectionAPI // JT 18.11.27 URLConvertible
        
        // Swift.print("urlStr \(urlStr)")  // JT 18.12.20
        
        var params: [String: String] = [:] // JT 18.11.27
        
        //   urlStr = "http://mobiledgexsdkdemomobiledgexsdkdemo10.microsoftwestus2cloudlet.azure.mobiledgex.net:8000/detector/detect/"
        
        if let image = image
        {
            let imageData = (image.pngData()! as NSData).base64EncodedString(
                options: NSData.Base64EncodingOptions.lineLength64Characters
            )
            
            params["image"] = imageData // JT 18.11.26
            
            //   let imageData2 = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"  // JT 18.11.27 tmp smallest working example
            //   params["image"] = imageData2 // JT 18.11.26 tmp
            
            let headers: HTTPHeaders = [
                "Accept": "application/json",
                // "Content-Type": "application/json",    // JT 18.11.27 fails. we are doing url encoding no json
                "Charsets": "utf-8",
                ]
            //    postRequest(urlStr, params, "FaceDetection")
            
            faceDetectionStartTime = DispatchTime.now() // <<<<<<<<<< Start time
            
            let requestObj = Alamofire.request(urlStr,
                                               method: HTTPMethod.post,
                                               parameters: params
                // , encoding: JSONEncoding.default // of -d
                , headers: headers) // JT 18.11.27
                
                .responseJSON
                { response in
                    //    Swift.print("----\n")
                    //    Swift.print("\(response)")
                    //    debugPrint(response)
                    
                    switch response.result {
                    case let .success(data):
                        
                        // Swift.print("")---
                        let d = data as! [String: Any]
                        let success = d["success"] as! String
                        if success == "true"
                        {
                            // Swift.print("data: \(data)") // JT 18.11.27
                            
                            let end = DispatchTime.now() // <<<<<<<<<<   end time
                            let nanoTime = end.uptimeNanoseconds - self.faceDetectionStartTime!.uptimeNanoseconds // <<<<< Difference in nano seconds (UInt64)
                            let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests
                            
                           // Swift.print("FaceDetection time: \(timeInterval)") // JT 18.11.28 // JT 18.12.20
                            SKToast.show(withMessage: "FaceDetection  time: \(timeInterval) result: \(data)") // JT 18.11.28
                            
                            let aa = d["rects"]
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "FaceDetection"), object: aa) // JT 18.12.08 draw blue rect around face  [[Int]]
                            
                            promise.succeed(value: d as [String : AnyObject])   //["rects": aa as AnyObject]) // JT 18.11.28 [[Int]]
                            
                            let latency = String(format: "%4.3f", timeInterval * 1000) // JT 18.12.11   // JT 18.12.20
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: broardcastMsg!), object: latency) // JT 18.12.11
                        }
                        else
                        {
                            let tv =  UserDefaults.standard.bool(forKey: "doFaceRecognition") //
                            if tv == false
                            {
                                doAFaceDetection = true // JT 18.11.26 atomic, one at a time
                            }
                        }
                        
                    case let .failure(error):
                        print(error)
                        //  SKToast.show(withMessage: "FaceDetection Failed: \(error)") // JT 18.12.10
                        promise.fail(error: error) // JT 18.12.10
                    }
            }
            
            // debugPrint(requestObj) // dump curl
            // Swift.print("")
        }
        
        return promise.future // JT 18.11.28
    }

    // Mark: -
    // Mark: FaceRecognition
    
    
    
    func doNextFaceRecognition( _ service: String)      // JT 18.12.16
    {
        if faceRecognitionImages.count == 0
        {
            doAFaceDetection = true // JT 18.11.26 todo tmp
            doAFaceRecognition = true
            return
        }
        let imageOfFace = faceRecognitionImages.removeFirst()
        faceRecognitionCurrentImage = imageOfFace
        
        var faceRecognitionFuture:Future<[String: AnyObject], Error>? // async result (captured by async?)
        
        faceRecognitionFuture = FaceRecognition(imageOfFace, service) // JT 18.12.10    // JT 18.12.16
        
        faceRecognitionFuture!.on(
            success:
            {
                print("FaceRecognition received value: \($0)")
                
                let reply = $0 as [String: Any]    // JT 18.12.03 json/dictionary
                print("FaceRecognition received value: \(reply)")
                
               // let rects = reply["rect"]
               // let rectData =   rects as! [Int] // JT 18.12.10
                
                SKToast.show(withMessage: "FaceRec \(reply["subject"]) confidence: \(reply["confidence"]) ")
                Swift.print("FaceRecognition \(reply)")
                
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: "faceRecognized" + service), object: reply )  // JT 18.12.16
                
                //     doAFaceRecognition = true
                
                //            DispatchQueue.main.async(execute: {
                //                doNextFaceRecognition()     // JT 18.12.10
                //
                //            })
                DispatchQueue.main.async {
                    self.doNextFaceRecognition(service)     // JT 18.12.10 next
                    
                }
                //Log.logger.name = "FaceDetection"
                //logw("\FaceDetection result: \(registerClientReply)")    // JT 18.12.04
        },
            failure: { print("FaceRecognition failed with error: \($0)")
                
        },
            completion: { let _ = $0 //print("completed with result: \($0)")  // JT 18.12.04
                
        }
        ) // JT 18.11.28
    }
    
    
    
    func FaceRecognition(_ image: UIImage?, _ service: String) // JT 18.11.26
        -> Future<[String: AnyObject], Error> // JT 18.11.28
    {
        let promise = Promise<[String: AnyObject], Error>()   // JT 18.11.28
        
        // detector/detect
        // Used to send a face image to the server and get back a set of coordinates for any detected faces.
        // POST http://<hostname>:8000/detector/detect/
        
        let faceRecognitonAPI: String = "/recognizer/predict/"  // JT 18.12.11
        
        //    Swift.print("FaceRecogniton")
        //    Swift.print("FaceRecogniton MEX .")
        //    Swift.print("====================\n")
        
        
        let postMsg =  "faceRecognitionLatency" + service   // JT 18.12.13
        let baseuri = (service ==  "Cloud" ? MexUtil.shared.DEF_FACE_HOST_CLOUD : MexUtil.shared.DEF_FACE_HOST_EDGE)   + ":" + MexUtil.shared.faceServerPort  // JT 18.12.20
        
        let urlStr = "http://" + baseuri + faceRecognitonAPI // JT 18.11.27 URLConvertible
        
       //  Swift.print("urlStr \(urlStr)") // JT 18.12.20
        
        var params: [String: String] = [:] // JT 18.11.27
        
        //   urlStr = "http://mobiledgexsdkdemomobiledgexsdkdemo10.microsoftwestus2cloudlet.azure.mobiledgex.net:8000/recognizer/predict/"
        
        if let image = image
        {
            let imageData = (image.pngData()! as NSData).base64EncodedString(
                options: NSData.Base64EncodingOptions.lineLength64Characters
            )
            
            params["image"] = imageData // JT 18.11.26
            
            //   let imageData2 = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"  // JT 18.11.27 tmp smallest working example
            //   params["image"] = imageData2 // JT 18.11.26 tmp
            
            let headers: HTTPHeaders = [
                "Accept": "application/json",
                // "Content-Type": "application/json",    // JT 18.11.27 fails. we are doing url encoding no json
                "Charsets": "utf-8",
                ]
            
            faceRecognitionStartTime = DispatchTime.now() // <<<<<<<<<< Start time
            
            let requestObj = Alamofire.request(urlStr,
                                               method: HTTPMethod.post,
                                               parameters: params
                // , encoding: JSONEncoding.default // of -d
                , headers: headers) // JT 18.11.27
                
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
                            // Swift.print("data: \(data)") // JT 18.11.27
                            
                            let end = DispatchTime.now()   // <<<<<<<<<<   end time
                            let nanoTime = end.uptimeNanoseconds -  self.faceRecognitionStartTime!.uptimeNanoseconds // <<<<< Difference in nano seconds (UInt64)
                            let timeInterval = Double(nanoTime) / 1_000_000_000 // Technically could overflow for long running tests
                            
                            promise.succeed(value: d as [String : AnyObject])  //
                            
                            Swift.print("FaceRecognition time: \(timeInterval)")  // JT 18.11.28
                            SKToast.show(withMessage: "FaceRecognition  time: \(timeInterval) result: \(data)")   // JT 18.11.28
                            
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "FaceRecognized"), object: d)   // JT 18.12.13 doNextFaceRecognition
                            
                            
                            let latency = String( format: "%4.3f", timeInterval * 1000 ) // JT 18.12.11 ms
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: postMsg), object: latency)     // JT 18.12.11 post latency
                        }
                        else
                        {
                            Swift.print("FaceRecognition failed")   // JT 18.12.14
                        }
                        
                    case let .failure(error):
                        print(error)
                        SKToast.show(withMessage: "FaceRecognition Failed: \(error)")
                        promise.fail(error: error)      // JT 18.12.10
                    }
            }
            
            // debugPrint(requestObj) // dump curl
            // Swift.print("")
        }
        
        return promise.future   // JT 18.11.28
    }
}



func convertPointsToRect(_ a:[Int])  ->CGRect    // JT 18.12.13 Mex data
{
    let r = CGRect(CGFloat(a[0]), CGFloat(a[1]), CGFloat(a[2] - a[0]), CGFloat(a[3] - a[1])) // face rect
    
    return r
}

// MARK:-

func getNetworkLatencyEdge()
{
    getNetworkLatency( MexUtil.shared.DEF_FACE_HOST_EDGE, post: "latencyEdge")    // JT 18.12.12
    
}

func getNetworkLatencyCloud()
{
  //  getNetworkLatency( MexUtil.shared.DEF_FACE_HOST_CLOUD, post: "latencyCloud")    // JT 18.12.12    // JT ping facedetection.defaultcloud.mobiledgex.net
    
}



func getNetworkLatency(_ hostName:String, post name: String)    // JT 18.12.12
{
    //Swift.print("\(#function) \(hostName)")     // JT 18.12.16    // JT 18.12.20
    
    
    PlainPing.ping(hostName, withTimeout: 1.0, completionBlock:
        { (timeElapsed: Double?, error: Error?) in
        
        if let latency = timeElapsed
        {
            let latencyMsg = String(format: "%4.3f", latency) // JT 18.12.11    // JT 18.12.20
            
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: name), object: latencyMsg) // JT 18.12.12
        }
        if let error = error
        {
            Swift.print("ping failed: \(hostName)")
            print("ping error: \(error.localizedDescription)")  // JT 18.12.16
        }
        
    })
}


