// Sample Client of SDK
//
//  Sample_client.swift
//  MobiledgeXSDKDemo.IOS
//
//  Port from cpp SDK demo by meta30
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Alamofire
import Foundation
import GoogleMaps
import Security

// ----------------------------------------

private var locationRequest: LocationRequest? // so we can stop updates

var userMarker: GMSMarker?   // set by RegisterClient , was: mUserLocationMarker

// This file Handles:
//
//  "Register Client",
//  "Get App Instances",
//  "Verify Location",
//  "Find Closet Cloudlet",
//  "Reset Location",

// MARK: MexUtil
// common
//
private class MexUtil
{
    static let shared = MexUtil()   // JT 18.11.18

    var sessionManager: SessionManager?  // JT 18.11.25

    let carrierNameDefault3: String = "TDG"
    let baseDmeHost: String = "dme.mobiledgex.net"

    let carrierNameDefault4: String = "mexdemo"

    var baseDmeHostInUse: String = "" // baseDmeHost2
    var carrierNameDefaultInUse: String = "" // carrierNameDefault4

    // API Paths:
    let registerAPI: String = "/v1/registerclient"
    let findcloudletAPI: String = "/v1/findcloudlet"
    let appinstlistAPI: String = "/v1/getappinstlist"

    let timeoutSec: UInt64 = 5000
    let dmePort: UInt = 38001

    var appName = "EmptyMatchEngineApp" // Your application name
    var devName = "EmptyMatchEngineApp" // Your developer name

    let appVersionStr = "1.0"

    let headers: HTTPHeaders = [
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Charsets": "utf-8",
    ]

    init()
    {
        baseDmeHostInUse = baseDmeHost

        carrierNameDefaultInUse = carrierNameDefault4

        appName = "MobiledgeX SDK Demo" // "EmptyMatchEngineApp"; // Your application name
        devName = "MobiledgeX SDK Demo" // "EmptyMatchEngineApp"; // Your developer name

//        let delegate = Alamofire.SessionManager.default.delegate
//        delegate.taskWillPerformHTTPRedirection = nil   // NOP
    }

    // Retrieve the carrier name of the cellular network interface.
    func getCarrierName() -> String
    {
        return carrierNameDefaultInUse
    }

    func generateDmeHostPath(_ carrierName: String) -> String
    {
        if carrierName == ""
        {
            return carrierNameDefaultInUse + "." + baseDmeHostInUse
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
                 _ request: [String: Any],
                 _ postName: String) // this is posted after results
    //-> [String: Any]
{
    Swift.print("URI to post to:\n \(uri)\n")
    Swift.print("\(request)")

    dealWithTrustPolicy(uri) // certs
    Swift.print("==========\n")

    let requestObj = MexUtil.shared.sessionManager!.request(
        uri,
        method: .post,
        parameters: request,
        encoding: JSONEncoding.default,
        headers: MexUtil().headers
    ).responseJSON
    { response in
        debugPrint("\n••\n\(response.request! )\n")

        guard response.result.isSuccess else
        {
            print("\nError while fetching remote -: \(String(describing: response.result.error))")

            let msg = String(describing: response.result.error)

            // hack parse error
            if msg.contains("dt-id=") // && postName == "GetToken"
            {
                let dtId = msg.components(separatedBy: "dt-id=")
                let s1 = dtId[1].components(separatedBy: ",")
                let token = s1[0]
                Swift.print("\(token)")
                
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName),  object: token)
            }
            else
            {
                Swift.print("post error")
            }
            return
        }

        switch response.result
        {
        case let .failure(error):
            Swift.print("\(error)")
            // Do whatever here
            return

        case let .success(data):
            // First make sure you got back a dictionary if that's what you expect
            guard let json = data as? [String: AnyObject] else
            {
                Swift.print("errorrrrr")
                return
            }
            Swift.print("=\(postName)=\n \(json)")

            NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName),
                                            object: json)
        }

        Swift.print("\(response)")
        Swift.print("\(response.result)")
        Swift.print("\(response.data!)")

        //            print(response.metrics)
        //           print(response.timeline)
    }

    debugPrint(requestObj) // dump curl

  //  return [String: Any]() // json //replyData;
}

private  func dealWithTrustPolicy(
    _ url: URLConvertible // ,      // a string
)
{
    // let certificates = getCertificates()
    let certificates = ServerTrustPolicy.certificates() // alamo extension
    Swift.print("~~~ \(certificates) ---")

    let trustPolicy = ServerTrustPolicy.pinCertificates(
        certificates: certificates,
        validateCertificateChain: true, // true
        validateHost: true
    ) // true

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
    static let shared = MexRegisterClient()     // JT 18.11.25

    var tokenserveruri = "" // set by RegisterClient    // JT 18.11.25
    var sessioncookie = ""  // set by RegisterClient    // JT 18.11.25 used by getApp and verifyLoc

    var theRequest: DataRequest?
    
    public static let COLOR_NEUTRAL: UInt32 = 0xFF67_6798
    public static let COLOR_VERIFIED: UInt32 = 0xFF00_9933
    public static let COLOR_FAILURE: UInt32 = 0xFFFF_3300
    public static let COLOR_CAUTION: UInt32 = 0xFF00_B33C // Amber: ffbf00;

    init()
    {
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "RegisterClient1"), object: nil, queue: nil)
        { notification in
            // Swift.print("RegisterClient \(notification)")

            let d = notification.object as! [String: Any]
            self.registerClientResult(d)

            self.getLocaltionUpdates()

            SKToast.show(withMessage: "Client registered")
        }

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "FindCloudlet1"), object: nil, queue: nil)
        { notification in
            // Swift.print("RegisterClient \(notification)")

            let d = notification.object as! [String: Any]

            Swift.print("FindCloudlet1 \(d)")

            //   useCloudlets(d)

            //    var cloudlets = [String:Cloudlet]()
            Swift.print("~~~~~")

         ///   var uri = ""
            //  var boundsBuilder = GMSCoordinateBounds()

            //  var marker:GMSMarker?

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
                if cld.key == "cloudlet_location"
                {
                    let dd = cld.value as! [String: Any]

                    Swift.print("••• \(dd)")

                    let loc = CLLocationCoordinate2D(
                        latitude: Double((dd["lat"] as! NSNumber).stringValue)!,
                        longitude: Double((dd["long"] as! NSNumber).stringValue)!
                    )

                    theMap!.animate(toLocation: loc)
                    SKToast.show(withMessage: "Found cloest cloudlet")

                    break
                }
            }
        }

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "GetToken"), object: nil, queue: nil)
        { notification in
            Swift.print("GetToken \(notification)")

            let tokenReply = notification.object as! String

            Swift.print("VerifyLocation: Retrieved token: [ \(tokenReply) ]")

            let loc = retrieveLocation()

            var verifyLocationRequest = createVerifyLocationRequest(MexUtil().carrierNameDefaultInUse, loc, "")

            // Update request with the new token:
            // json tokenizedRequest;
            var tokenizedRequest = [String: Any]()

            tokenizedRequest["ver"] = verifyLocationRequest["ver"]
            tokenizedRequest["SessionCookie"] = verifyLocationRequest["SessionCookie"]
            tokenizedRequest["CarrierName"] = verifyLocationRequest["CarrierName"]
            tokenizedRequest["GpsLocation"] = verifyLocationRequest["GpsLocation"]
            tokenizedRequest["VerifyLocToken"] = tokenReply
            

            Swift.print("VeriyLocation actual call...")
           // let reply = ""
            let baseuri = MexUtil().generateBaseUri(MexUtil().carrierNameDefaultInUse, MexUtil().dmePort)
            let verifylocationAPI: String = "/v1/verifylocation"

            let uri = baseuri + verifylocationAPI

            postRequest(uri, tokenizedRequest, "VeriyLocation1")
        }

        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "VeriyLocation1"), object: nil, queue: nil)
        { notification in
            // Swift.print("RegisterClient \(notification)")

            let d = notification.object
            Swift.print("VeriyLocation1 result \(d!)")

            SKToast.show(withMessage: "VeriyLocation result: \(d!)")
        }
    } // init

    deinit
    {
        NotificationCenter.default.removeObserver(self)
    }

    func getLocaltionUpdates()
    {
        Swift.print("getLocaltionUpdates")

//        Locator.currentPosition(usingIP: .smartIP, onSuccess:   // not working
//            { loc in
//            print("Found location: \(loc)")
//
//            if userMarker == nil
//            {
//                doUserMarker(loc.coordinate)
//            }
//             userMarker!.position = loc.coordinate
//
//        }) { err, _ in
//            print("\(err)")
//        }

        resetUserLocation(true)

        //     gpsInitialized = true;

//      let locator = Locator.subscribeSignificantLocations(onUpdate: { newLocation in
//            print("New location \(newLocation)")
//
//        if userMarker == nil
//        {
//            self.doUserMarker(newLocation.coordinate)
//        }
//        userMarker!.position = newLocation.coordinate
//
//
//        }) { (err, lastLocation) -> (Void) in
//            print("Failed with err: \(err)")
//        }
    }

    // MARK: create requests
    
    func createRegisterClientRequest() -> [String: Any]
    {
        let u = MexUtil()
        //   json regClientRequest;
        var regClientRequest = [String: String]()

        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = u.appName
        regClientRequest["DevName"] = u.devName
        regClientRequest["AppVers"] = u.appVersionStr

        return regClientRequest
    }



    // Carrier name can change depending on cell tower.
    func createFindCloudletRequest(_ carrierName: String, _ gpslocation: [String: Any]) -> [String: Any]
    {
        //    findCloudletRequest;
        var findCloudletRequest = [String: Any]()

        findCloudletRequest["vers"] = 1
        findCloudletRequest["SessionCookie"] = sessioncookie
        findCloudletRequest["CarrierName"] = carrierName
        findCloudletRequest["GpsLocation"] = gpslocation

        return findCloudletRequest
    }
    
    
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
            
            sessioncookie = (registerClientReply["SessionCookie"] as! String)
            
            Swift.print("Token Server URI: " + (registerClientReply["TokenServerURI"] as! String) + "\n")
            tokenserveruri = (registerClientReply["TokenServerURI"] as! String)
            Swift.print("")
        }
    }
    

}


// MARK: -
// MARK: MexGetAppInst

private class MexGetAppInst
{
    static let shared = MexGetAppInst()
    
    var theRequest: DataRequest?
    
    init()
    {
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "GetAppInstlist1"), object: nil, queue: nil)
        { notification in
            // Swift.print("RegisterClient \(notification)")
            
            let d = notification.object as! [String: Any]
            
            
            Swift.print("GetAppInstlist1 \(d)")
            
            // useCloudlets(d)
            
            //  theMap!.clear()       // JT 18.11.12 todo main thread, where
            
            var cloudlets = [String: Cloudlet]()
            Swift.print("~~~~~")
            
            //var uri = ""
            var boundsBuilder = GMSCoordinateBounds()
            
            var marker: GMSMarker?
            
            for (index, cld) in d.enumerated()
            {
                Swift.print("\n•\n \(index):\(cld)")
                
                Swift.print("\(index): \(cld.key)")
                
                if cld.key == "Cloudlets" // "cloudlet_location"
                {
                    //       let ddd = cld.value
                    let a = cld.value as! [[String: Any]]
                    
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
                        
                        let dd = d["Appinstances"] as! [[String: Any]]
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
                let padding: CGFloat  = 125.0 // offset from edges of the map in pixels
                
                //                let update = GMSCameraUpdate.fit(boundsBuilder, withPadding: 64)
                //                theMap!.moveCamera(update)
                
                theMap!.animate(with: .fit(boundsBuilder, withPadding: 64.0))
                // BUG padding is being ignored todo
            }
            else
            {
                Swift.print("No cloudlets. Don't zoom in")
                
                //  cu = CameraUpdateFactory.newLatLng(mUserLocationMarker.getPosition());
                
                //      theMap!.animate(toLocation: marker!.position)   //  user position
            }
        }
        
    }
    
    deinit
    {
        NotificationCenter.default.removeObserver(self)
    }
    
    // Carrier name can change depending on cell tower.
    func createGetAppInstListRequest(_ carrierName: String, _ gpslocation: [String: Any]) -> [String: Any]
    {
        //   json findCloudletRequest;
        var appInstListRequest = [String: Any]()
        
        appInstListRequest["vers"] = 1
        appInstListRequest["SessionCookie"] = MexRegisterClient.shared.sessioncookie
        appInstListRequest["CarrierName"] = carrierName
        appInstListRequest["GpsLocation"] = gpslocation
        
        return appInstListRequest
    }
    
    
}


func doUserMarker(_ loc: CLLocationCoordinate2D)
{
    // requestWhenInUseAuthorization()

    userMarker = GMSMarker(position: loc)
    userMarker!.title = "You are here"
    userMarker!.snippet = "Drag to spoof" //                         marker!.snippet = "Tap for details"

    //  marker.map = self.mGoogleMap
    let iconTemplate = UIImage(named: "ic_marker_mobile-web")

    let tint = getColorByHex(MexRegisterClient.COLOR_NEUTRAL)
    let tinted = iconTemplate!.imageWithColor(tint)

    let resized = tinted.imageResize(sizeChange: CGSize(width: 60, height: 60))
    userMarker!.icon = resized
    userMarker!.map = theMap

    userMarker!.isDraggable = true // drag to test spoofing
}

// MARK: registerClientNow

func registerClientNow() // called by top right menu
{
   // mexRegisterClient1 = MexRegisterClient()

    Swift.print("registerClientNow")
    Swift.print("Register MEX client.")
    Swift.print("====================\n")

    let baseuri = MexUtil().generateBaseUri(MexUtil().getCarrierName(), MexUtil().dmePort)
    Swift.print("\(baseuri)")

    let registerClientRequest = MexRegisterClient.shared.createRegisterClientRequest()
    
    let urlStr = baseuri + MexUtil().registerAPI
    postRequest(urlStr, registerClientRequest, "RegisterClient1")
    
}


// MARK: -


func getAppInstNow()    // called by top right menu
{
    //mexGetAppInst = MexGetAppInst()
    
    Swift.print("GetAppInstList")
    Swift.print(" MEX client.")
    Swift.print("====================\n")
    
    let baseuri = MexUtil().generateBaseUri(MexUtil().getCarrierName(), MexUtil().dmePort)
    Swift.print("\(baseuri)")
    
    let loc = retrieveLocation()
    
    let getAppInstListRequest = MexGetAppInst.shared.createGetAppInstListRequest(MexUtil().carrierNameDefault3, loc)
    
    postRequest(baseuri + MexUtil().appinstlistAPI, getAppInstListRequest, "GetAppInstlist1")
    
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
        Swift.print("\(findCloudletReply["FQDN"])")
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
    let jd: [String: Any]? = ["latitude": lat, "longitude": lng]

    let hostName: String = MexUtil().generateDmeHostPath(MexUtil().getCarrierName()).replacingOccurrences(of: "dme", with: "locsim")

    let urlString: URLConvertible = "http://\(hostName):8888/updateLocation"

    Swift.print("\(urlString)")
    // let headers2:HTTPHeaders? =  MexUtil().headers

    Alamofire.request(urlString,
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
// MARK: doVerifyLocation

func doVerifyLocation() // call by top right menu
{
    // Produces a new request. Now with sessioncooke and token initialized.
    
    Swift.print("Verify Location of this Mex client.")
    Swift.print("===================================\n\n")
    
    let baseuri = MexUtil().generateBaseUri(MexUtil().carrierNameDefaultInUse, MexUtil().dmePort)
    let loc = retrieveLocation()
    
    let verifyLocationRequest = createVerifyLocationRequest(MexUtil().carrierNameDefaultInUse, loc, "")
    
    VerifyLocation(baseuri, verifyLocationRequest)
}


private func createVerifyLocationRequest(_ carrierName: String,
                                         _ gpslocation: [String: Any],
                                         _ verifyloctoken: String)
    -> [String: Any]
{
    
    var verifyLocationRequest = [String: Any]() // // json verifyLocationRequest;
    
    verifyLocationRequest["ver"] = 1
    verifyLocationRequest["SessionCookie"] = MexRegisterClient.shared.sessioncookie
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
    
    postRequest(uri, [String: Any](), "GetToken")   // async
}



// MARK: -
// MARK: findNearestCloudlet

func findNearestCloudlet() //   called by top right menu
{
    Swift.print("Finding nearest Cloudlet appInsts matching this Mex client.")
    Swift.print("===========================================================")
    
    let baseuri = MexUtil().generateBaseUri(MexUtil().getCarrierName(), MexUtil().dmePort)
    let loc = retrieveLocation()
    
    let findCloudletRequest = MexRegisterClient.shared.createFindCloudletRequest(MexUtil().carrierNameDefault3, loc)
    
    postRequest(baseuri + MexUtil().findcloudletAPI, findCloudletRequest,  "FindCloudlet1")
    
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

// MARK: -

// used by: GetToken, getAppInstNow, verify  loc

private func retrieveLocation() -> [String: Any]
{
    var location = [String: Any]() //     //  json location;
    
    if userMarker != nil    // get app isnt sets userMarker
    {
        location["lat"] = userMarker!.position.latitude // -122.149349;
        location["long"] = userMarker!.position.longitude // 37.459609;
    }
    else
    {
        location["lat"] = -122.149349
        location["long"] = 37.459609
    }
    
    return location
}
