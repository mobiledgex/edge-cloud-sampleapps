//#include <curl/curl.h>
//
//#include <sstream>
//#include <iostream>
//
//#include <nlohmann/json.hpp>
//
//#include <algorithm>
//
//#include "test_credentials.hpp"

//using namespace std;
//using namespace std::chrono;
//using namespace nlohmann;

import Security // JT 18.11.08
import Alamofire
import Foundation
import GoogleMaps   // JT 18.10.23


var sessionManager: SessionManager?    // JT 18.11.08
var mexClient1: MexRestClient1?         // JT 18.11.11
var userMarker: GMSMarker?  // JT 18.11.11

var mexClient2: MexRestClient2?         // JT 18.11.12


class MexUtil   // JT 18.11.11
{
    let carrierNameDefault :String  = "gddt2"
    let carrierNameDefault3 :String  = "GDDT" // JT 18.11.09
   let baseDmeHost :String  = "dme.mobiledgex.net"
    
    let baseDmeHost2 :String  = " locsim.mobiledgex.net"   // JT 18.11.11
    let carrierNameDefault4 :String  = "mexdemo"    // JT 18.11.11

    
    // API Paths:
    let registerAPI :String = "/v1/registerclient"
    let findcloudletAPI :String = "/v1/findcloudlet"
    let appinstlistAPI :String = "/v1/getappinstlist"   // JT 18.11.12 to example yet

    let timeoutSec : UInt64 = 5000
    let dmePort: UInt  = 38001
    let appName = "EmptyMatchEngineApp"; // Your application name
    let devName = "EmptyMatchEngineApp"; // Your developer name
    let appVersionStr = "1.0";

    let headers: HTTPHeaders = [
        //  "Authorization": "Bearer /*PRIVATE KEY*/"
        "Accept" : "application/json",
        "Content-Type" : "application/json",
        "Charsets" : "utf-8",
        ]   // JT 18.11.08
    
    // Retrieve the carrier name of the cellular network interface.
    func getCarrierName() ->String
    {
        return carrierNameDefault;
    }
    
    func generateDmeHostPath(_ carrierName: String) ->String
    {
        if (carrierName == "")
        {
            return carrierNameDefault + "." + baseDmeHost
        }
        return carrierName + "." + baseDmeHost
    }
    
    func generateBaseUri(_ carrierName: String, _ port: UInt) ->String
    {
        return "https://\(generateDmeHostPath(carrierName)):\(dmePort)"  // JT 18.11.07
    }
 }

// MARK: -


func postRequest(_ uri: String,
                 _ request: [String:Any],
                 _ responseData: String,
                 _ postName: String  // JT 18.11.09
    )  -> [String:Any] // ->json  // JT 18.11.08
{
    
    Swift.print( "URI to post to:\n \(uri)\n")  // JT 18.11.07
    Swift.print("\(request)")
    
    // let url = URL(string: uri)  // JT 18.11.08
    
    //theRequest =
    aRequest( uri, parameters: request, headers: headers)  // JT 18.11.08
    //    print(theRequest)   // JT 18.11.08
    Swift.print("==========\n")
    
    // return SessionManager.default.request(
    sessionManager!.request(
        uri,
        method: .post,
        parameters: request,
        encoding: JSONEncoding.default,
        headers: headers
        ).responseJSON  { response in
            
            debugPrint(   "\n••\n\(response.request)\n")   // JT 18.11.09
            
            
            guard response.result.isSuccess else
            {
                print("\nError while fetching remote -: \(String(describing: response.result.error))")
                
                //     completion(nil)
                return
                //[String:Any]() //json //replyData;
            }
            
            switch response.result
            {
            case .failure(let error):
                // Do whatever here
                return
                
            case .success(let data):
                // First make sure you got back a dictionary if that's what you expect
                guard let json = data as? [String : AnyObject] else {
                    Swift.print("errorrrrr")
                    return
                    
                }
                Swift.print("=\(postName)=\n \(json)")
                
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName),
                                                object: json )  // JT 18.11.09
            }
            
            Swift.print("\(response)")
            Swift.print("\(response.result)")
            Swift.print("\(response.data!)")
            
            //            print(response.metrics)
            //           print(response.timeline)
            
    }   // JT 18.11.08
    
    
    
    return [String:Any]() //json //replyData;
}
// MARK: -

func retrieveLocation() ->[String:Any]  // JT 18.11.11
{
    //  json location;
    
    var location = [String:Any]()    // JT 18.11.07
    
    location["lat"] = 37.0  // -122.149349;
    location["long"] = 137.2    //37.459609;
    //        location["horizontal_accuracy"] = 5;
    //        location["vertical_accuracy"] = 20;
    //        location["altitude"] = 100;
    //        location["course"] = 0;
    //        location["speed"] = 2;
    //        location["timestamp"] = "2008-09-08T22:47:31-07:00"; // currentGoogleTimestamp();
    
    return location;
}

class MexRestClient2    // JT 18.11.12
{
    var theRequest: DataRequest?    // JT 18.11.12
    
    
    init()
    {
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "GetAppInstlist1"), object: nil, queue: nil)
        {   notification in
            //Swift.print("RegisterClient \(notification)")
            
            let d = notification.object  as! [String : Any] // JT 18.11.09
            
            Swift.print("GetAppInstlist1 \(d)")    // JT 18.11.09
            
           // useCloudlets(d) // JT 18.11.09
            
            theMap!.clear()  // JT 18.11.12
            
            var cloudlets = [String:Cloudlet]()  // JT 18.11.11
            Swift.print("~~~~~")
            
            var uri = ""
            var boundsBuilder = GMSCoordinateBounds()   // JT 18.11.06
            
            var marker:GMSMarker?   // JT 18.11.11
            
            for ( index, cld) in d.enumerated()
            {
                Swift.print("\n•\n \(index):\(cld)") // JT 18.11.11
                
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
                    //       let ddd = cld.value
                    let dd = cld.value as! [String:Any]
                    
                    Swift.print("••• \(dd)")
                    //                    let cloudlet_location = dd["cloudlet_location"] // JT 18.11.11
                    let loc = CLLocationCoordinate2D(
                        latitude: Double((dd["lat"] as! NSNumber).stringValue)!,
                        longitude: Double((dd["long"] as! NSNumber).stringValue )!
                    )
                    
                    boundsBuilder.includingCoordinate(loc)      // JT 18.11.11
                    
                    marker  = GMSMarker(position: loc)    // JT 18.11.05
                    marker!.title = "cloudletName\(index)"
                    marker!.snippet = "Details"
                    //  marker.map = self.mGoogleMap // JT 18.11.05
                    let iconTemplate = UIImage(named: "ic_marker_cloudlet-web")  // JT 18.11.01
                    
                    let tint = getColorByHex(MexRestClient1.COLOR_NEUTRAL)    // JT 18.11.05
                    let tinted  = iconTemplate!.imageWithColor(tint )
                    
                    let resized = tinted.imageResize( sizeChange: CGSize(width:40, height:30))
                    marker?.icon = resized  // JT 18.11.05
                    
                    let cloudlet = Cloudlet( "cloudletName\(index)", "EmptyMatchEngineApp",
                                             "carrierName",
                                             loc,
                                             0,
                                             uri,
                                             marker!,
                                             0,
                                             0
                    )
                    marker?.map = theMap // JT 18.11.11
                    cloudlets[ "cloudletName\(index)"] = cloudlet   // JT 18.11.11
                }
                
            }
            Swift.print("~~~~~]\n\(cloudlets)")
            
            CloudletListHolder.getSingleton().setCloudlets(mCloudlets: cloudlets);
            
            if !(boundsBuilder.southWest == boundsBuilder.northEast)
            {
                //Log.d(TAG, "Using cloudlet boundaries");
                Swift.print("Using cloudlet boundaries")
                let padding:Int = 240; // offset from edges of the map in pixels
                /// cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                
                let update = GMSCameraUpdate.fit( boundsBuilder, withPadding: 50.0)
                theMap!.moveCamera(update)   // JT 18.11.06
                
                
                // viewMap.camera = camera
            } else {
                //Log.d(TAG, "No cloudlets. Don't zoom in");
                Swift.print("No cloudlets. Don't zoom in")
                
                //  cu = CameraUpdateFactory.newLatLng(mUserLocationMarker.getPosition());
                
                theMap!.animate(toLocation: marker!.position)   // JT 18.11.11 yodo user position
                
            }
            
        }
        
        //  doUserMarker()
      //  getLocaltionUpdates()   // JT 18.11.11
    }   // JT 18.11.07

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    // Carrier name can change depending on cell tower.
    func createGetAppInstListRequest(_ carrierName: String, _ gpslocation : [String:Any]) ->  [String:Any]
    {
        //   json findCloudletRequest;
        var appInstListRequest = [String:Any]()    // JT 18.11.07
        
        appInstListRequest["vers"] = 1;
        appInstListRequest["SessionCookie"] = sessioncookie;
        appInstListRequest["CarrierName"] = carrierName;
        appInstListRequest["GpsLocation"] = gpslocation;
        
        return appInstListRequest;
    }
    
    func getAppInstList(_ baseuri: String, _ request: [String:Any], _ reply: inout String) ->[String:Any]
    {
        Swift.print("request \(request)")
        let jreply = postRequest(baseuri + MexUtil().appinstlistAPI, request, reply, "GetAppInstlist1")
        
        return jreply;
    }
}

class MexRestClient1
{
 
    var theRequest: DataRequest?    // JT 18.11.09
    public static let  COLOR_NEUTRAL: UInt32 = 0xff676798;
    public static let  COLOR_VERIFIED: UInt32 = 0xff009933;
    public static let  COLOR_FAILURE: UInt32 = 0xffff3300;
    public static let  COLOR_CAUTION: UInt32 = 0xff00b33c; //Amber: ffbf00;
    
    
    init()
    {
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "RegisterClient1"), object: nil, queue: nil)
        {   notification in
            //Swift.print("RegisterClient \(notification)")
            
            let d = notification.object  as! [String : Any] // JT 18.11.09
            registerClientResult(d )
            
            doGetCloudlets()
        }
        
        NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "FindCloudlet1"), object: nil, queue: nil)
        {   notification in
            //Swift.print("RegisterClient \(notification)")
            
            let d = notification.object  as! [String : Any] // JT 18.11.09
            
            Swift.print("FindCloudlet1 \(d)")    // JT 18.11.09
            
            useCloudlets(d) // JT 18.11.09
            
            var cloudlets = [String:Cloudlet]()  // JT 18.11.11
            Swift.print("~~~~~")
            
            var uri = ""
            var boundsBuilder = GMSCoordinateBounds()   // JT 18.11.06
            
            var marker:GMSMarker?   // JT 18.11.11
            
            for ( index, cld) in d.enumerated()
            {
                Swift.print("\n•\n \(index):\(cld)") // JT 18.11.11
                
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
             //       let ddd = cld.value
                    let dd = cld.value as! [String:Any]
                    
                    Swift.print("••• \(dd)")
//                    let cloudlet_location = dd["cloudlet_location"] // JT 18.11.11
                    let loc = CLLocationCoordinate2D(
                        latitude: Double((dd["lat"] as! NSNumber).stringValue)!,
                        longitude: Double((dd["long"] as! NSNumber).stringValue )!
                    )
                    
                    boundsBuilder.includingCoordinate(loc)      // JT 18.11.11
                    
                    marker  = GMSMarker(position: loc)    // JT 18.11.05
                    marker!.title = "cloudletName\(index)"
                    marker!.snippet = "Details"
                  //  marker.map = self.mGoogleMap // JT 18.11.05
                    let iconTemplate = UIImage(named: "ic_marker_cloudlet-web")  // JT 18.11.01
                    
                    let tint = getColorByHex(MexRestClient1.COLOR_NEUTRAL)    // JT 18.11.05
                    let tinted  = iconTemplate!.imageWithColor(tint )
                    
                    let resized = tinted.imageResize( sizeChange: CGSize(width:40, height:30))
                    marker?.icon = resized  // JT 18.11.05
                    
                    let cloudlet = Cloudlet( "cloudletName\(index)", "EmptyMatchEngineApp",
                                            "carrierName",
                                            loc,
                                            0,
                                            uri,
                                            marker!,
                                            0,
                                            0
                    )
                    marker?.map = theMap // JT 18.11.11
                    cloudlets[ "cloudletName\(index)"] = cloudlet   // JT 18.11.11
                }
                
            }
            Swift.print("~~~~~]\n\(cloudlets)")

            CloudletListHolder.getSingleton().setCloudlets(mCloudlets: cloudlets);
            
            if !(boundsBuilder.southWest == boundsBuilder.northEast)
            {
                //Log.d(TAG, "Using cloudlet boundaries");
                Swift.print("Using cloudlet boundaries")
                let padding:Int = 240; // offset from edges of the map in pixels
                /// cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                
                let update = GMSCameraUpdate.fit( boundsBuilder, withPadding: 50.0)
                theMap!.moveCamera(update)   // JT 18.11.06
                
                
                // viewMap.camera = camera
            } else {
                //Log.d(TAG, "No cloudlets. Don't zoom in");
                Swift.print("No cloudlets. Don't zoom in")
                
                //  cu = CameraUpdateFactory.newLatLng(mUserLocationMarker.getPosition());
                
                theMap!.animate(toLocation: marker!.position)   // JT 18.11.11 yodo user position
                
            }
            
        }
        
      //  doUserMarker()
        getLocaltionUpdates()   // JT 18.11.11
    }   // JT 18.11.07

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    func doUserMarker( _ loc: CLLocationCoordinate2D)
    {
       // requestWhenInUseAuthorization()
        
        userMarker  = GMSMarker(position: loc)    // JT 18.11.05
        userMarker!.title = "cloudletName\(index)"
        userMarker!.snippet = "Click for details"
        //  marker.map = self.mGoogleMap // JT 18.11.05
        let iconTemplate = UIImage(named: "ic_marker_mobile-web")  // JT 18.11.01
        
        let tint = getColorByHex(MexRestClient1.COLOR_NEUTRAL)    // JT 18.11.05
        let tinted  = iconTemplate!.imageWithColor(tint )
        
        let resized = tinted.imageResize( sizeChange: CGSize(width:40, height:30))
        userMarker!.icon = resized  // JT 18.11.05
        userMarker!.map = theMap // JT 18.11.11
    }
    func getLocaltionUpdates()
    {
        Swift.print("getLocaltionUpdates")
        Locator.subscribePosition(accuracy: .house, onUpdate:
            {  newLocation in
                //print("New location received: \(newLocation)")    // JT 18.11.12
                if userMarker == nil
                {
                    self.doUserMarker(newLocation.coordinate)
                }
                userMarker!.position = newLocation.coordinate
        }, onFail: {  err, last in
            print("subscribePosition: Failed with error: \(err)")
    })
    
   //     gpsInitialized = true;
        
      let locator = Locator.subscribeSignificantLocations(onUpdate: { newLocation in
            print("New location \(newLocation)")
        
        if userMarker == nil
        {
            self.doUserMarker(newLocation.coordinate)
        }
        userMarker!.position = newLocation.coordinate

        
        //            if self.gpsInitialized == false
//            {
//                self.getCloudlets();
//            }
//
        }) { (err, lastLocation) -> (Void) in
            print("Failed with err: \(err)")
        }
    }
    
    func createRegisterClientRequest() ->[String:Any]   // JT 18.11.08
    {
        let u = MexUtil()
        //   json regClientRequest;
        var regClientRequest = [String:String]()    // JT 18.11.07
        
        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = u.appName
        regClientRequest["DevName"] = u.devName
        regClientRequest["AppVers"] = u.appVersionStr
        
        return regClientRequest;
    }
    

    func RegisterClient(_ baseuri: String, _ request: [String:Any],_ reply: inout String) ->[String:Any]
    {
        //    let jreply = postRequest(baseuri + registerAPI, request.dump(), reply)  //, getReplyCallback);
        let urlStr = baseuri + MexUtil().registerAPI
        var jreply = postRequest( urlStr, request , reply, "RegisterClient1")  //, getReplyCallback);    // JT 18.11.08
//        if jreply.count > 0
//        {
//            tokenserveruri = jreply["TokenServerURI"] as! String
//            sessioncookie = jreply["SessionCookie"] as! String;
//
//        }
        
        return jreply;      // JT 18.11.11 async do this is empty
    }
   

    
    
    func aRequest(
        _ url: URLConvertible,
        method: HTTPMethod = .post,  // .get,   // JT 18.11.08
        parameters: Parameters? = nil,
        encoding: ParameterEncoding = JSONEncoding.default,  //URLEncoding.default,
        headers: HTTPHeaders? = nil)
        //-> DataRequest
    {
        // enableCertificatePinning
        // let certificates = getCertificates()
        let certificates = ServerTrustPolicy.certificates()  // JT 18.11.09
        Swift.print("~~~ \(certificates) ---")  // JT 18.11.09
        let trustPolicy = ServerTrustPolicy.pinCertificates(
            certificates: certificates,
            validateCertificateChain: true, // true
            validateHost: true) // true
        
        let trustPolicies = [ "gddt2.dme.mobiledgex.net" : trustPolicy ] // "www.apple.com"
        let policyManager = ServerTrustPolicyManager(policies: trustPolicies)
        
        sessionManager = SessionManager(
            configuration: .default,
            serverTrustPolicyManager: policyManager
        )
    }
    
    //-----
    func FindCloudlet(_ baseuri: String, _ request: [String:Any], _ reply: inout String) ->[String:Any]
    {
        Swift.print("FindCloudlet request \(request)")   // JT 18.11.09
        let jreply = postRequest(baseuri + MexUtil().findcloudletAPI, request, reply, "FindCloudlet1")           // JT 18.11.11
        return jreply;
    }
    
    // Carrier name can change depending on cell tower.
    func createFindCloudletRequest(_ carrierName: String, _ gpslocation : [String:Any]) ->  [String:Any]
    {
        //   json findCloudletRequest;
        var findCloudletRequest = [String:Any]()    // JT 18.11.07
        
        findCloudletRequest["vers"] = 1;
        findCloudletRequest["SessionCookie"] = sessioncookie;
        findCloudletRequest["CarrierName"] = carrierName;
        findCloudletRequest["GpsLocation"] = gpslocation;
        
        return findCloudletRequest;
    }

    
}

func createVerifyLocationRequest( _ carrierName: String,
                                  _ gpslocation : [String:Any], _ verifyloctoken: String)
    -> [String:Any]
{
    // json verifyLocationRequest;
    var verifyLocationRequest = [String:Any]()    // JT 18.11.07
    
    verifyLocationRequest["ver"] = 1;
    verifyLocationRequest["SessionCookie"] = sessioncookie;
    verifyLocationRequest["CarrierName"] = carrierName;
    verifyLocationRequest["GpsLocation"] = gpslocation;
    verifyLocationRequest["VerifyLocToken"] = verifyloctoken;
    
    return verifyLocationRequest;
}
func getAppInstNow()
{
    mexClient2 = MexRestClient2()

    Swift.print("GetAppInstList")
    Swift.print(" MEX client.")
    Swift.print("====================\n")
    
    let baseuri = MexUtil().generateBaseUri(MexUtil().getCarrierName(), MexUtil().dmePort);
    Swift.print("\(baseuri)")
    var unused:String = ""
    let loc =  retrieveLocation();

    
           var getAppInstListRequest = mexClient2!.createGetAppInstListRequest(MexUtil().carrierNameDefault3, loc);
        
        var reply = mexClient2!.GetAppInstList(baseuri, getAppInstListRequest, &unused);

}

func registerClientNow()    // JT 18.11.11
{
    mexClient1 = MexRestClient1()

    Swift.print("registerClientNow")
    Swift.print("Register MEX client.")
    Swift.print("====================\n")
    
    let baseuri = MexUtil().generateBaseUri(MexUtil().getCarrierName(), MexUtil().dmePort);
    Swift.print("\(baseuri)")
    var strRegisterClientReply:String = ""
    let registerClientRequest = mexClient1!.createRegisterClientRequest()
    
    let registerClientReply = mexClient1!.RegisterClient(baseuri, registerClientRequest, &strRegisterClientReply)
   // Swift.print("\(registerClientReply)")
}

// MARK: -

class MexRestClient //: URLSessionDelegate    // JT 18.11.08
{
    let carrierNameDefault :String  = "gddt2"
    let carrierNameDefault3 :String  = "GDDT" // JT 18.11.09
    let baseDmeHost :String  = "dme.mobiledgex.net"

    // API Paths:
    let registerAPI :String = "/v1/registerclient"
    let findcloudletAPI :String = "/v1/findcloudlet"
    let getlocatiyonAPI :String = "/v1/getlocation"
    let appinstlistAPI :String = "/v1/getappinstlist"   // JT 18.11.12 to example yet
    let dynamiclocgroupAPI :String = "/v1/dynamiclocgroup"

    let timeoutSec : UInt64 = 5000
    let dmePort: UInt  = 38001
    let appName = "EmptyMatchEngineApp"; // Your application name
    let devName = "EmptyMatchEngineApp"; // Your developer name
    let appVersionStr = "1.0";

    // SSL files:

    let CaCertFile = "mex-ca.crt";
    let ClientCertFile = "mex-client.crt";
    let ClientPrivKey = "mex-client.key";

    var theRequest: DataRequest?    // JT 18.11.09
    
    init() {}   // JT 18.11.07

    // Retrieve the carrier name of the cellular network interface.
    func getCarrierName() ->String
    {
        return carrierNameDefault;
    }

    func generateDmeHostPath(_ carrierName: String) ->String
    {
        if (carrierName == "")
        {
            return carrierNameDefault + "." + baseDmeHost
        }
        return carrierName + "." + baseDmeHost
    }

    func generateBaseUri(_ carrierName: String, _ port: UInt) ->String
    {
//        stringstream ss;
//        ss << "https://" << generateDmeHostPath(carrierName) << ":" << dmePort;
//
//        return ss.str();

        return "https://\(generateDmeHostPath(carrierName)):\(dmePort)"  // JT 18.11.07
    }

   // json currentGoogleTimestamp()
    func currentGoogleTimestamp() ->[String:Double]  // JT 18.11.07
  {
        // Google's protobuf timestamp format.
 //       let microseconds = std::chrono::system_clock::now().time_since_epoch();
 //       let ts_sec = duration_cast<std::chrono::milliseconds>(microseconds);
  //      let ts_ns = duration_cast<std::chrono::nanoseconds>(microseconds);

    let secs = Date().timeIntervalSince1970 // JT 18.11.07
     //   json googleTimestamp;
        var googleTimestamp = [String:Double]()    // JT 18.11.07
//        googleTimestamp["seconds"] = ts_sec.count();
//        googleTimestamp["nanos"] = ts_sec.count();

    googleTimestamp["seconds"] = secs   // JT 18.11.07
    googleTimestamp["nanos"] = secs

        return googleTimestamp;
    }

    // A C++ GPS location provider/binding is needed here.
    func retrieveLocation() ->[String:Any]
    {
      //  json location;
    
    var location = [String:Any]()    // JT 18.11.07

        location["lat"] = 37.0  // -122.149349;
        location["long"] = 137.2    //37.459609;
//        location["horizontal_accuracy"] = 5;
//        location["vertical_accuracy"] = 20;
//        location["altitude"] = 100;
//        location["course"] = 0;
//        location["speed"] = 2;
//        location["timestamp"] = "2008-09-08T22:47:31-07:00"; // currentGoogleTimestamp();

        return location;
    }

    func createRegisterClientRequest() ->[String:Any]   // JT 18.11.08
    {
     //   json regClientRequest;
    var regClientRequest = [String:String]()    // JT 18.11.07

        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = appName
        regClientRequest["DevName"] = devName
        regClientRequest["AppVers"] = appVersionStr

        return regClientRequest;
    }

    // Carrier name can change depending on cell tower.
  //  json createVerifyLocationRequest(const string &carrierName, const json gpslocation, const string verifyloctoken)
    func createVerifyLocationRequest( _ carrierName: String,
    _ gpslocation : [String:Any], _ verifyloctoken: String)
    -> [String:Any]
    {
       // json verifyLocationRequest;
        var verifyLocationRequest = [String:Any]()    // JT 18.11.07

        verifyLocationRequest["ver"] = 1;
        verifyLocationRequest["SessionCookie"] = sessioncookie;
        verifyLocationRequest["CarrierName"] = carrierName;
        verifyLocationRequest["GpsLocation"] = gpslocation;
        verifyLocationRequest["VerifyLocToken"] = verifyloctoken;

        return verifyLocationRequest;
    }

    // Carrier name can change depending on cell tower.
    func createFindCloudletRequest(_ carrierName: String, _ gpslocation : [String:Any]) ->  [String:Any]
    {
     //   json findCloudletRequest;
        var findCloudletRequest = [String:Any]()    // JT 18.11.07

        findCloudletRequest["vers"] = 1;
        findCloudletRequest["SessionCookie"] = sessioncookie;
        findCloudletRequest["CarrierName"] = carrierName;
        findCloudletRequest["GpsLocation"] = gpslocation;
    
        return findCloudletRequest;
    }

    let headers: HTTPHeaders = [
      //  "Authorization": "Bearer /*PRIVATE KEY*/"
        "Accept" : "application/json",
        "Content-Type" : "application/json",
        "Charsets" : "utf-8",
    ]   // JT 18.11.08
    func postRequest(_ uri: String,
                     _ request: [String:Any],
                     _ responseData: String,
        _ postName: String  // JT 18.11.09
        )  -> [String:Any] // ->json  // JT 18.11.08
    {
 
        Swift.print( "URI to post to:\n \(uri)\n")  // JT 18.11.07
        Swift.print("\(request)")
 
       // let url = URL(string: uri)  // JT 18.11.08

         //theRequest =
        aRequest( uri, parameters: request, headers: headers)  // JT 18.11.08
    //    print(theRequest)   // JT 18.11.08
Swift.print("==========\n")
        
        // return SessionManager.default.request(
    sessionManager!.request(
            uri,
            method: .post,
            parameters: request,
            encoding: JSONEncoding.default,
            headers: headers
        ).responseJSON  { response in
            
           debugPrint(   "\n••\n\(response.request)\n")   // JT 18.11.09

            
            guard response.result.isSuccess else
            {
                print("\nError while fetching remote -: \(String(describing: response.result.error))")
                
                //     completion(nil)
                return
                    //[String:Any]() //json //replyData;
            }

            switch response.result
            {
            case .failure(let error):
                // Do whatever here
                return
                
            case .success(let data):
                // First make sure you got back a dictionary if that's what you expect
                guard let json = data as? [String : AnyObject] else {
Swift.print("errorrrrr")
                    return
                    
                }
                Swift.print("=\(postName)=\n \(json)")

                NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName),
                                                object: json )  // JT 18.11.09
            }
 
                
            
            Swift.print("\(response)")
           Swift.print("\(response.result)")
            Swift.print("\(response.data!)")
         
//            print(response.metrics)
 //           print(response.timeline)

        }   // JT 18.11.08
        


        //enableCertificatePinning() // sets sessionManager
    
     //   let jsonData = try JSONSerialization.data(withJSONObject: request, options: .prettyPrinted)
            
 
        
        //cout << "Reply: [" << replyData.dump() << "]" << endl;
          //  Swift.print("json \(json)")
        return [String:Any]() //json //replyData;
    }


        func aRequest(
            _ url: URLConvertible,
            method: HTTPMethod = .post,  // .get,   // JT 18.11.08
            parameters: Parameters? = nil,
            encoding: ParameterEncoding = JSONEncoding.default,  //URLEncoding.default,
            headers: HTTPHeaders? = nil)
            //-> DataRequest
         {
            // enableCertificatePinning
           // let certificates = getCertificates()
            let certificates = ServerTrustPolicy.certificates()  // JT 18.11.09
Swift.print("~~~ \(certificates) ---")  // JT 18.11.09
            let trustPolicy = ServerTrustPolicy.pinCertificates(
                certificates: certificates,
                validateCertificateChain: true, // true
                validateHost: true) // true
            
            let trustPolicies = [ "gddt2.dme.mobiledgex.net" : trustPolicy ] // "www.apple.com"
            let policyManager = ServerTrustPolicyManager(policies: trustPolicies)
            
            sessionManager = SessionManager(
                configuration: .default,
                serverTrustPolicyManager: policyManager
            )
            
            
        }
   

    
        
//        func enableCertificatePinning()
//        {
//            let certificates = getCertificates()
//            let trustPolicy = ServerTrustPolicy.pinCertificates(
//                certificates: certificates,
//                validateCertificateChain: true,
//                validateHost: true)
//            let trustPolicies = [ "www.apple.com": trustPolicy ]
//            let policyManager = ServerTrustPolicyManager(policies: trustPolicies)
//
//            sessionManager = SessionManager(
//                configuration: .default,
//                serverTrustPolicyManager: policyManager
//            )
//        }
    
        func getCertificates() -> [SecCertificate]
        {
            // let url = Bundle.main.url(forResource: "appleCert", withExtension: "cer")!  // JT 18.11.08 todo
            let url = Bundle.main.url(forResource: "mex-ca", withExtension: "der")!  // JT 18.11.08 crt
          //  let url2 = Bundle.main.url(forResource: "mex-client", withExtension: "der")!  // JT 18.11.08
         //   let url3 = Bundle.main.url(forResource: "mex-clientkey", withExtension: "der")!  // JT 18.11.08
            
    
            let localCertificate = try! Data(contentsOf: url) as CFData
            guard let certificate = SecCertificateCreateWithData(nil, localCertificate)
                else { return [] }
            
//            let localCertificate2 = try! Data(contentsOf: url2) as CFData
//            guard let certificate2 = SecCertificateCreateWithData(nil, localCertificate2)
//                else { return [] }
//
//
//            let localCertificate3 = try! Data(contentsOf: url3) as CFData
//            guard let certificate3 = SecCertificateCreateWithData(nil, localCertificate3)
//                else { return [] }
            
            
            return [certificate]    // JT 18.11.08
          //  return [certificate, certificate2, certificate3]    // JT 18.11.08

        }
        
        
    // URLSessionDelegate
    func urlSession(_ session: URLSession,
                    didReceive challenge: URLAuthenticationChallenge,
                    completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) // JT 18.11.08
    {
        
        guard challenge.previousFailureCount == 0 else {
            challenge.sender?.cancel(challenge)
            // Inform the user that the user name and password are incorrect
            completionHandler(.cancelAuthenticationChallenge, nil)
            return
        }
        
        // Within your authentication handler delegate method, you should check to see if the challenge protection space has an authentication type of NSURLAuthenticationMethodServerTrust
        if challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust
            // and if so, obtain the serverTrust information from that protection space.
            && challenge.protectionSpace.serverTrust != nil
            && challenge.protectionSpace.host == "yourdomain.com" {
            let proposedCredential = URLCredential(trust: challenge.protectionSpace.serverTrust!)
            completionHandler(URLSession.AuthChallengeDisposition.useCredential, proposedCredential)
        }
    }

//    static size_t getReplyCallback(void *contentptr, size_t size, size_t nmemb, void *replyBuf) {
//        size_t dataSize = size * nmemb;
//        string *buf = ((string*)replyBuf);
//
//        if (contentptr != NULL && buf) {
//            string *buf = ((string*)replyBuf);
//            buf->append((char*)contentptr, dataSize);
//
//            cout << "Data Size: " << dataSize << endl;
//            //cout << "Current replyBuf: [" << *buf << "]" << endl;
//        }
//
//
//        return dataSize;
//    }

    // MARK: -
    
    func RegisterClient(_ baseuri: String, _ request: [String:Any],_ reply: inout String) ->[String:Any]
    {
    //    let jreply = postRequest(baseuri + registerAPI, request.dump(), reply)  //, getReplyCallback);
        let urlStr = baseuri + registerAPI
        var jreply = postRequest( urlStr, request , reply, "RegisterClient")  //, getReplyCallback);    // JT 18.11.08
        if jreply.count > 0
        {
            tokenserveruri = jreply["TokenServerURI"] as! String
            sessioncookie = jreply["SessionCookie"] as! String;

        }
  
        return jreply;
    }

    // string formatted json args and reply.
    func VerifyLocation(_ baseuri: String, _ request: [String:Any], _ reply: inout String)//  ->[String:Any] // json
    {
        if (tokenserveruri.count == 0) {
           // cerr << "TokenURI is empty!" << endl;
            Swift.print("TokenURI is empty!")
            let empty = [String:Any]()
            return
       //     return empty;
        }
        else
        {
            getToken(tokenserveruri)!;
        }
    }

    func FindCloudlet(_ baseuri: String, _ request: [String:Any], _ reply: inout String) ->[String:Any]
    {
        Swift.print("request \(request)")   // JT 18.11.09
        let jreply = postRequest(baseuri + findcloudletAPI, request, reply, "FindCloudlet") //, getReplyCallback);

        return jreply;
    }

    func getToken(_ uri: String) ->String?
    {
       // cout << "In Get Token" << endl;
        Swift.print("In Get Token")
        if (uri.count == 0) {
            //cerr << "No URI to get token!" << endl;
            Swift.print("No URI to get token!")
            return nil;
        }

//        // Since GPRC's Channel is somewhat hidden
//        // we can use CURL here instead.
//        CURL *curl = curl_easy_init();
//        if (curl == nil) {
//            cerr << "Curl could not be initialized." << endl;
//            return nil;
//        }
//        CURLcode res;
//        cout << "uri: " << uri << endl;
//        curl_easy_setopt(curl, CURLOPT_URL, uri.c_str());
//        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, false);  // Do not follow redirect.
//        curl_easy_setopt(curl, CURLOPT_HEADER, 1);              // Keep headers.
//
//        // Set return pointer (the token), for the header callback.
//        curl_easy_setopt(curl, CURLOPT_HEADERDATA, &(this->token));
//        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header_callback);
//
//        // SSL Setup:
//        curl_easy_setopt(curl, CURLOPT_SSLCERT, ClientCertFile.c_str());
//        curl_easy_setopt(curl, CURLOPT_SSLKEY, ClientPrivKey.c_str());
//        // CA:
//        curl_easy_setopt(curl, CURLOPT_CAINFO, CaCertFile.c_str());
//        // verify peer or disconnect
//        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1);
//
//        res = curl_easy_perform(curl);
//        if (res != CURLE_OK) {
//           cerr << "Error getting token: " << res << endl;
//        }
//
//        curl_easy_cleanup(curl);

//        let json = try CURLRequest( uri, .failOnError,
//                                    .postField(.init(name: "CURLOPT_HTTPHEADER",
//                                                     value: headers)),   // JT 18.11.07 todo key
//          //  .postField(.init(name: "CURLOPT_TIMEOUT", value: "timeoutSec")),
//
//            .postField(.followLocation(false))  // JT 18.11.07
//
//            .postField(.sslCert(ClientCertFile)),
//            .postField(.sslKey(ClientPrivKey))  // JT 18.11.07
//                .postField(.sslKey(CaCertFile))  // JT 18.11.07
//                .postField(.sslVerifyPeer(1))  // JT 18.11.07
//
//
//       //  .postField(.init(name: "file1",
//            //                        filePath: testFile.path,
//            //                       mimeType: "text/plain"))
//            )   // JT 18.11.07
//            .perform().bodyJSON
        
        
        Swift.print("")
        var reply:String = ""
        let jreply = postRequest( uri, [String:Any](), reply, "GetToken") //, getReplyCallback);

        return token;
    }

    var token: String = ""  // short lived carrier dt-id token.


    func parseParameter( _ queryParameter: String, _ keyFind: String) ->String
    {
        let value: String = ""
        var foundToken: String = ""
//        size_t vpos = queryParameter.find("=");
//
//        string key = queryParameter.substr(0, vpos);
//        cout << "Key: " << key << endl;
//        vpos += 1; // skip over '='
//        string valPart = queryParameter.substr(vpos, queryParameter.length() - vpos);
//        cout << "ValPart: " << valPart << endl;
        
        
        let a = queryParameter.components(separatedBy: "=")   // key, value
        let key = a[0]
let valPart = a[1]
        Swift.print("\(key) = \(valPart)")
        if ((key == keyFind) && a.count > 1 ) {

                foundToken = valPart // queryParameter.substr(vpos, queryParameter.length() - vpos);
            //    cout << "Found Token: " << foundToken << endl;
                Swift.print("Found Token: \(foundToken)")
        }
        return foundToken;
    }

//    func parseToken( _ locationUri: String) ->String
//     {
//        // Looking for carrier dt-id: <token> in the URL, and ignoring everything else.
//        var pos = locationUri.find("?")
//        pos += 1;
//        let uriParameters = locationUri.substr(pos, locationUri.length() - pos);
//        pos = 0;
//        var start = 0;
//        var foundToken = ""
//
//        // Carrier token.
//        var keyFind:String = "dt-id";
//
//        var queryParameter: String = ""
//        repeat {
//            pos = uriParameters.find("&", start);
//            if (pos+1 >= uriParameters.length()) {
//                break;
//            }
//
//            if (pos == nil) {  // Last one, or no terminating &
//                queryParameter = uriParameters.substr(start, uriParameters.length() - start);
//                foundToken = parseParameter(queryParameter, keyFind);
//                break;
//            } else {
//                queryParameter = uriParameters.substr(start, pos - start);
//                foundToken = parseParameter(queryParameter, keyFind);
//            }
//
//            // Next.
//            start = pos+1;
//            if (foundToken != "") {
//                break;
//            }
//        } while (pos != nil )   //std::string::npos);   // JT 18.11.07
//
//        return foundToken;
//    }
//
//    func trimStringEol( _ stringBuf: String) ->String
//    {
//        let size = stringBuf.count
//
//        // HTTP/1.1 RFC 2616: Should only be "\r\n" (and not '\n')
//        if (size >= 2 && (stringBuf[size-2] == "\r" && stringBuf[size-1] == "\n")) {
//            let seol = size-2;
//            return stringBuf.substr(0,seol);
//        } else {
//            // Not expected EOL format, returning as-is.
//            return stringBuf;
//        }
//
//    }

 
    #if false
    // Callback function to retrieve headers, called once per header line.
//    func  header_callback(/* char * */ _ buffer: String, _ size: UInt64, _ n_items: UInt64, void *userdata) ->UInt64
//    {
//        let bufferLen = n_items * size;
//
//        // Need to get "Location: ....dt-id=ABCDEF01234"
//        var s = String(buffer);
//        s = trimStringEol(s);
//
//        var key = "";
//        var value = "";
//        string *token = (string *)userdata;
//
//        // split buffer:
//        let colonPos = s.firstIndex(of: ":"); //String firstIndex
//        var blankPos = 0
//
//        if (colonPos != nil) {
//            key = stringBuf.substr(0, colonPos);
//            if (key == "Location") {
//                // Skip blank
//                blankPos = stringBuf.firstIndex(of: " ") + 1;
//                if (//(blankPos != std::string::npos) &&
//                    (blankPos < stringBuf.length()))
//                {
//                    value = stringBuf.substr(blankPos, stringBuf.length() - blankPos);
//                    //cout << "Location Header Value: [" << value << "]" << endl;
//                    Swift.print("Location Header Value: [" + value + "]")
//                    *token = parseToken(value);
//                }
//            }
//        }
//
//        // Return number of bytes read thus far from reply stream.
//        return bufferLen;
//    }
    #endif

    
   
}

// MARK: -
func doVerifyLocation()
{
    // Produces a new request. Now with sessioncooke and token initialized.
    //        cout << "Verify Location of this Mex client." << endl;
    //        cout << "===================================" << endl
    //             << endl;
    
    Swift.print("Verify Location of this Mex client.")
    Swift.print("===================================\n\n")
    
    
    let baseuri = mexClient!.generateBaseUri(mexClient!.carrierNameDefault3, mexClient!.dmePort)
    let loc = mexClient!.retrieveLocation();
    var strVerifyLocationReply = ""
    var verifyLocationRequest = mexClient!.createVerifyLocationRequest(mexClient!.carrierNameDefault3, loc, "");
    
    var verifyLocationReply = mexClient!.VerifyLocation(baseuri, verifyLocationRequest, &strVerifyLocationReply);
    

}

// MARK: -


func doGetCloudlets()   // JT 18.11.09
{
    
    Swift.print("Finding nearest Cloudlet appInsts matching this Mex client.")
    Swift.print("===========================================================")
    
    let baseuri = MexUtil().generateBaseUri(MexUtil().getCarrierName(), MexUtil().dmePort);
    let loc = retrieveLocation();
    var strFindCloudletReply: String = ""
    let findCloudletRequest = mexClient1!.createFindCloudletRequest(MexUtil().carrierNameDefault3, loc); // JT 18.11.09 // JT 18.11.11
    var findCloudletReply = mexClient1!.FindCloudlet( baseuri,
                                                     findCloudletRequest,
                                                     &strFindCloudletReply) // async
    
   
    
    Swift.print("--- after: FindCloudlet")
    
}

func useCloudlets(_ findCloudletReply: [String:Any]) // JT 18.11.09
{
    if (findCloudletReply.count == 0) {
        // cout << "REST VerifyLocation Status: NO RESPONSE" << endl;
        Swift.print("REST VerifyLocation Status: NO RESPONSE")
    }
    else {
        //            cout << "REST FindCloudlet Status: "
        //                 << "Version: " << findCloudletReply["ver"]
        //                 << ", Location Found Status: " << findCloudletReply["status"]
        //                 << ", Location of cloudlet. Latitude: " << findCloudletReply["cloudlet_location"]["lat"]
        //                 << ", Longitude: " << findCloudletReply["cloudlet_location"]["long"]
        //                 << ", Cloudlet FQDN: " << findCloudletReply["fqdn"] << endl;
        
        let loooc = findCloudletReply["cloudlet_location"] as! [String:Any]
        let latN = loooc["lat"] as? NSNumber          // JT 18.11.09 ZZZ
        let lat = "\(latN!)"
        let longN = loooc["long"] as? NSNumber
        let long = "\(longN!)"

        let line1 = "REST FindCloudlet Status: "
        let ver = findCloudletReply["ver"] as? NSNumber
        let line2 = "Version: " + "\(ver)"  // JT 18.11.09
        let line3 = ", Location Found Status: " + (findCloudletReply["status"] as! String)
        let line4 = ", Location of cloudlet. Latitude: " + lat
        let line5 = ", Longitude: "   + long
        Swift.print("\(findCloudletReply["FQDN"])")
        let line6 = ", Cloudlet FQDN: " + (findCloudletReply["FQDN"]  as! String)//<< endl;
        
        Swift.print(line1 + line2 + line3 + line4 + line5 + line6)  // JT 18.11.07
        let ports:[[String:Any]]  = findCloudletReply["ports"]   as! [[String:Any]] // as! [String : Any]   // json
        let size = ports.count    // size_t
        for appPort in ports
        {
            Swift.print("\(appPort)")   // JT 18.11.08
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

var mexClient: MexRestClient?   // JT 18.11.09
var tokenserveruri = ""
var sessioncookie = ""

func doMEXTest()    // JT 18.11.08
{
    Swift.print("Hello C++ MEX REST Lib")
    
    NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "RegisterClient"), object: nil, queue: nil)
    {   notification in
        //Swift.print("RegisterClient \(notification)")
        
        let d = notification.object  as! [String : Any] // JT 18.11.09
        registerClientResult(d )
        
       // doVerifyLocation()  // JT 18.11.09 chain
        doGetCloudlets()
    }
    
    
    NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "VerifyLocation"), object: nil, queue: nil)
    {   notification in
        //Swift.print("RegisterClient \(notification)")
        
        let verifyLocationReply = notification.object  as! [String : Any] // JT 18.11.09
        
        // Print some reply values out:
        if (verifyLocationReply.count == 0) {
            //cout << "REST VerifyLocation Status: NO RESPONSE" << endl;
            Swift.print("REST VerifyLocation Status: NO RESPONSE")
        }
        else {
            // cout << "[" << verifyLocationReply.dump() << "]" << endl;
            Swift.print("verifyLocationReply: [ \(verifyLocationReply) ]")
        }
    }
    
    NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "Token"), object: nil, queue: nil)
    {   notification in
        Swift.print("Token \(notification)")
        
        let tokenReply = notification.object  as! [String : Any] // JT 18.11.09
        
        //cout << "VerifyLocation: Retrieved token: [" << token << "]" << endl;
        Swift.print("VerifyLocation: Retrieved token: [ \(tokenReply) ]")
        
        let loc = mexClient!.retrieveLocation();

        var verifyLocationRequest = mexClient!.createVerifyLocationRequest(mexClient!.getCarrierName(), loc, "");

        // Update request with the new token:
        // json tokenizedRequest;
        var tokenizedRequest = [String:Any]()
        
        tokenizedRequest["ver"] = verifyLocationRequest["ver"];
        tokenizedRequest["SessionCookie"] = verifyLocationRequest["SessionCookie"];
        tokenizedRequest["CarrierName"] = verifyLocationRequest["CarrierName"];
        tokenizedRequest["GpsLocation"] = verifyLocationRequest["GpsLocation"];
        tokenizedRequest["VerifyLocToken"] = tokenizedRequest   // "" //token;    // JT 18.11.09 todo
        
        //cout << "VeriyLocation actual call..." << endl;
        Swift.print("VeriyLocation actual call...")
        var reply = ""
        var baseuri = mexClient!.generateBaseUri(mexClient!.getCarrierName(), mexClient!.dmePort);
        let verifylocationAPI :String = "/v1/verifylocation"

        let jreply = mexClient!.postRequest(baseuri + verifylocationAPI, tokenizedRequest, reply, "VeriyLocation") //, getReplyCallback);
       // return jreply;
    }
    
    NotificationCenter.default.addObserver(forName: NSNotification.Name(rawValue: "FindCloudlet"), object: nil, queue: nil)
    {   notification in
        //Swift.print("RegisterClient \(notification)")
        
        let d = notification.object  as! [String : Any] // JT 18.11.09
        
        Swift.print("FindCloudlet \(d)")    // JT 18.11.09
        
         useCloudlets(d) // JT 18.11.09

     }
    
 //   curl_global_init(CURL_GLOBAL_DEFAULT);

    // Credentials, Mutual Authentication:

    mexClient = MexRestClient()
    do {
        var loc = mexClient!.retrieveLocation()
Swift.print("\(loc)")   // JT 18.11.08
 //       cout << "Register MEX client." << endl;
   //     cout << "====================" << endl
    //         << endl;
        Swift.print("Register MEX client.")
        Swift.print("====================\n")

        var baseuri = mexClient!.generateBaseUri(mexClient!.getCarrierName(), mexClient!.dmePort);
        Swift.print("\(baseuri)")
        var strRegisterClientReply:String = ""
        let registerClientRequest = mexClient!.createRegisterClientRequest()
        
        let registerClientReply = mexClient!.RegisterClient(baseuri, registerClientRequest, &strRegisterClientReply)
        Swift.print("\(registerClientReply)")
return  // JT 18.11.08


    

 
        Swift.print("Finding nearest Cloudlet appInsts matching this Mex client.")
        Swift.print("===========================================================")
        
        baseuri = mexClient!.generateBaseUri(mexClient!.getCarrierName(), mexClient!.dmePort);
        loc = mexClient!.retrieveLocation();
        var strFindCloudletReply: String = ""
        var findCloudletRequest = mexClient!.createFindCloudletRequest(mexClient!.getCarrierName(), loc);
        var findCloudletReply = mexClient!.FindCloudlet( baseuri,
                                                        findCloudletRequest,
                                                        &strFindCloudletReply)

        if (findCloudletReply.count == 0) {
           // cout << "REST VerifyLocation Status: NO RESPONSE" << endl;
            Swift.print("REST VerifyLocation Status: NO RESPONSE")
        }
        else {
//            cout << "REST FindCloudlet Status: "
//                 << "Version: " << findCloudletReply["ver"]
//                 << ", Location Found Status: " << findCloudletReply["status"]
//                 << ", Location of cloudlet. Latitude: " << findCloudletReply["cloudlet_location"]["lat"]
//                 << ", Longitude: " << findCloudletReply["cloudlet_location"]["long"]
//                 << ", Cloudlet FQDN: " << findCloudletReply["fqdn"] << endl;
            
            let loooc = findCloudletReply["cloudlet_location"] as! [String:Any]
            let lat = ( loooc["lat"] as! String)
            let long = ( loooc["long"] as! String)
     let line1 = "REST FindCloudlet Status: "
            let line2 = "Version: " + (findCloudletReply["ver"] as! String)
          let line3 = ", Location Found Status: " + (findCloudletReply["status"] as! String)
            let line4 = ", Location of cloudlet. Latitude: " + lat
            let line5 = ", Longitude: "   + long
            let line6 = ", Cloudlet FQDN: " + (findCloudletReply["fqdn"]  as! String)//<< endl;
           
            Swift.print(line1 + line2 + line3 + line4 + line5 + line6)  // JT 18.11.07
            let ports:[String:Any]  = findCloudletReply["ports"] as! [String : Any]   // json
            let size = ports.count    // size_t
            for appPort in ports
            {
                Swift.print("\(appPort)")   // JT 18.11.08
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

      //  cout << endl;
        Swift.print("")

    }
//    catch (std::runtime_error &re) {
//        cerr << "Runtime error occurred: " << re.what() << endl;
//    } catch (std::exception &ex) {
//        cerr << "Exception ocurred: " << ex.what() << endl;
//    } catch (char *what) {
//        cerr << "Exception: " << what << endl;
//    }
    catch   {
      //  cerr << "Unknown failure happened." << endl;
        Swift.print("Unknown failure happened.")
    }

   // curl_global_cleanup();
    //return 0;
}

func registerClientResult(_ registerClientReply: [String:Any])
{
    if (registerClientReply.count == 0)
    {
        //   cerr << "REST RegisterClient Error: NO RESPONSE." << endl;
        Swift.print("REST RegisterClient Error: NO RESPONSE.")
        // return 1; // JT 18.11.08
    } else {
        //            cout << "REST RegisterClient Status: "
        //                 << "Version: " << registerClientReply["ver"]
        //                 << ", Client Status: " << registerClientReply["status"]
        //                 << ", SessionCookie: " << registerClientReply["SessionCookie"]
        //                 << endl
        //                 << endl;
        
        let line1 = "\nREST RegisterClient Status: \n"
        let ver = registerClientReply["Ver"] as? NSNumber
        let line2 = "Version: " + "\(ver!)"   // JT 18.11.08
        let line3 = ",\n Client Status:" + (registerClientReply["Status"] as! String)
        let line4 = ",\n SessionCookie:" + (registerClientReply[ "SessionCookie"] as! String)
        
        Swift.print( line1 + line2 + line3 + line4 + "\n\n" )
        
        sessioncookie =  (registerClientReply[ "SessionCookie"] as! String) // JT 18.11.09
        
        Swift.print("Token Server URI: " + (registerClientReply["TokenServerURI"] as! String) + "\n")
        tokenserveruri = (registerClientReply["TokenServerURI"] as! String)
    }
    

    
}

// MARK: -


// JT 18.11.05 todo util
func getColorByHex(rgbHexValue:UInt32, alpha:Double = 1.0) -> UIColor   // JT 18.11.05
{
    let red = Double((rgbHexValue & 0xFF0000) >> 16) / 256.0
    let green = Double((rgbHexValue & 0xFF00) >> 8) / 256.0
    let blue = Double((rgbHexValue & 0xFF)) / 256.0
    
    return UIColor(red: CGFloat(red), green: CGFloat(green), blue: CGFloat(blue), alpha: CGFloat(alpha))
}

func getColorByHex(_ rgbHexValue:UInt32 ) -> UIColor   // JT 18.11.05
{
    let alpha = Double((rgbHexValue & 0xFF000000) >> 24) / 256.0
    let red = Double((rgbHexValue & 0xFF0000) >> 16) / 256.0
    let green = Double((rgbHexValue & 0xFF00) >> 8) / 256.0
    let blue = Double((rgbHexValue & 0xFF)) / 256.0
    
    return UIColor(red: CGFloat(red), green: CGFloat(green), blue: CGFloat(blue), alpha: CGFloat(alpha))
}



extension UIImage
{
    func imageWithColor(_ color1: UIColor) -> UIImage {
        UIGraphicsBeginImageContextWithOptions(self.size, false, self.scale)
        color1.setFill()
        
        let context = UIGraphicsGetCurrentContext()
        context?.translateBy(x: 0, y: self.size.height)
        context?.scaleBy(x: 1.0, y: -1.0)
        context?.setBlendMode(CGBlendMode.normal)
        
        let rect = CGRect(origin: .zero, size: CGSize(width: self.size.width, height: self.size.height))
        context?.clip(to: rect, mask: self.cgImage!)
        context?.fill(rect)
        
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return newImage!
    }
}

// JT 18.11.05 to do move to util

func textToImage(drawText text: String, inImage image: UIImage, atPoint point: CGPoint) -> UIImage
{
    let textColor = UIColor.white
    let textFont = UIFont(name: "Helvetica Bold", size: 20)!
    
    let scale = UIScreen.main.scale
    UIGraphicsBeginImageContextWithOptions(image.size, false, scale)
    
    let textFontAttributes = [
        NSAttributedString.Key.font: textFont,
        NSAttributedString.Key.foregroundColor: textColor,
        ] as [NSAttributedString.Key : Any]
    image.draw(in: CGRect(origin: CGPoint.zero, size: image.size))
    
    let rect = CGRect(origin: point, size: image.size)
    text.draw(in: rect, withAttributes: textFontAttributes)
    
    let newImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    
    return newImage!
}

extension CLLocationCoordinate2D: Equatable {}

public func ==(lhs: CLLocationCoordinate2D, rhs: CLLocationCoordinate2D) -> Bool {
    return lhs.latitude == rhs.latitude && lhs.longitude == rhs.longitude
}




extension UIImage
{
    
    func imageResize (sizeChange:CGSize)-> UIImage{
        
        let hasAlpha = true
        let scale: CGFloat = 0.0 // Use scale factor of main screen
        
        UIGraphicsBeginImageContextWithOptions(sizeChange, !hasAlpha, scale)
        self.draw(in: CGRect(origin: CGPoint.zero, size: sizeChange))
        
        let scaledImage = UIGraphicsGetImageFromCurrentImageContext()
        return scaledImage!
    }
    
}
