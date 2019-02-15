//
//  MatchingEngineSDK.swift
//  MatchingEngineSDK
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

import Alamofire // dependency
import NSLogger // JT

/// MexSDK MobiledeX SDK APIs

// MARK: -

public class MexSDK
{
    public static let shared = MexSDK() /// singleton access: MexSDK.shared.whatever

    var sessionManager: SessionManager? // alamofire: creeated based on host trust
    // need different SessionManager for each host

    let headers: HTTPHeaders = [
        "Accept": "application/json",
        "Content-Type": "application/json", // This is the default
        "Charsets": "utf-8",
    ] /// headers HTTPHeaders:

    private init() // singleton
    {} // init

    // MARK: -

    // MARK: postRequest

    /// Async https request
    ///
    /// - Parameters:
    ///   - uri:  url
    ///   - request: json/Dictionary
    ///   - postName:  "commandName" posted for observers
    ///
    /// - Returns: Future for later success/failure

    public func postRequest(_ uri: String,
                            _ request: [String: Any], // Dictionary/json
                            _ postName: String = "postRequestReplyLogger") // this is posted after results
        -> Future<[String: AnyObject], Error>
    {
        let promise = Promise<[String: AnyObject], Error>() // completion callback

        // - Logging
        
        Swift.print("URI to post to:\n \(uri)\n")

        if ((false)) // DEBUG
        {
            Swift.print("\(request)")
            /// logw("•uri:\n\(uri)\n") // JT 18.11.26 log to file
        }
        LogMarker(postName)

        Logger.shared.log(.network, .info, postName + " request\n \(request) \n") // JT 19.01.04

        //  logw("•request:\n\(request)\n") // JT 18.11.26 log to file
        // --
        
        dealWithTrustPolicy(uri) // certs

        let requestObj = MexSDK.shared.sessionManager!.request(
            uri,
            method: .post,
            parameters: request,
            encoding: JSONEncoding.default,
            headers: MexSDK.shared.headers
        ).responseJSON
        { response in
            if (false)
            {
                debugPrint("\n••\n\(response.request!)\n") // curl
            }

            guard response.result.isSuccess else
            { // failed
                //  print("\nError while fetching remote -: \(String(describing: response.result.error))")

                let msg = String(describing: response.result.error)

                // hack parse error
                if msg.contains("dt-id=") // special case // && postName == "GetToken"
                { // not really an error
                    let dtId = msg.components(separatedBy: "dt-id=")
                    let s1 = dtId[1].components(separatedBy: ",")
                    let token = s1[0]
                    Swift.print("\(token)")

                    NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName), object: token) // "GetToken"

                    promise.succeed(value: ["token": token as AnyObject])
                }
                else
                {
                    Swift.print("post error \(response.result.error!)")
                }
                return
            }

            switch response.result
            {
            case let .failure(error):
                Swift.print("\(error)")
                // Do whatever here
                // If error
                promise.fail(error: error)
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
                promise.succeed(value: json)

                NotificationCenter.default.post(name: NSNotification.Name(rawValue: postName),
                                                object: json)
                // - Logging
                //      Log.logger.name = "curl"  // J

                // let curl = response.request.debugDescription
                // logw("\n \(curl)\n")
                // logw("postName:\(postName)\nresult:\n\(json)") //   log to file

                Logger.shared.log(.network, .info, postName + " reply json\n \(json) \n")
            }

            //  Swift.print("\(response)")
            Swift.print("result \(response.result)")
            Swift.print("data \(response.data!)")

            //            print(response.metrics)
            //           print(response.timeline)
        }

        if (false)  // DEBUG
        {
            debugPrint(requestObj) // dump curl
        }

        Logger.shared.log(.network, .info, postName + " curl\n \(requestObj.debugDescription) \n")

        return promise.future
    }

    // in general
    //

    ///  Deal with certificates, trust
    ///
    /// - Parameter url:

    func dealWithTrustPolicy(
        _ url: URLConvertible // a string
    )
    {
        // let certificates = getCertificates()
        let certificates = ServerTrustPolicy.certificates() // alamo extension
        Swift.print("~~~certificates: \(certificates) ---")
        //  Logger.shared.log(.network, .info,  " certificates:\n \(certificates) \n" )    // JT 19.01.05
        Logger.shared.log(.network, .info, " add these certificates to your curl below --cacert mex-ca.crt --cert mex-client.crt")

        let trustPolicy = ServerTrustPolicy.pinCertificates(
            certificates: certificates,
            validateCertificateChain: true,
            validateHost: true
        )

        do
        {
            let whoToTrust = try url.asURL().host
            //     Swift.print("\(whoToTrust)")

            let trustPolicies = [whoToTrust!: trustPolicy] // [String: ServerTrustPolicy]

            let policyManager = ServerTrustPolicyManager(policies: trustPolicies)

            MexSDK.shared.sessionManager = SessionManager(
                configuration: .default,
                serverTrustPolicyManager: policyManager
            ) // JT 18.12.26
        }
        catch
        {
            Swift.print("dealWithTrustPolicy asURL throws: trust betraied")
        }
    }

    // MARK: -

    // requests

    /// API createRegisterClientRequest
    ///
    /// - Parameters:
    ///   - ver: "1"
    ///   - appName: "appName"
    ///   - devName:  "devName"
    ///   - appVers: "appVers""
    /// - Returns: API Dictionary/json

    public func createRegisterClientRequest( appName: String, devName: String, appVers: String)
        -> [String: Any] // Dictionary/json
    {
        var regClientRequest = [String: String]() // Dictionary/json regClientRequest

        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = appName
        regClientRequest["DevName"] = devName
        regClientRequest["AppVers"] = appVers

        return regClientRequest
    }

    /// createGetAppInstListRequest
    ///
    /// - Parameters:
    ///   - carrierName: <#carrierName description#>   // Carrier name can change depending on cell tower.
    ///   - gpslocation: <#gpslocation description#>
    ///   - sessioncookie: <#sessioncookie description#>
    ///
    /// - Returns: API Dictionary/json

    public func createGetAppInstListRequest(carrierName: String, gpslocation: [String: Any], sessioncookie: String) -> [String: Any] // JT 18.12.26
    {
        //   json findCloudletRequest;
        var appInstListRequest = [String: Any]() // Dictionary/json

        appInstListRequest["vers"] = 1
        appInstListRequest["SessionCookie"] = sessioncookie
        appInstListRequest["CarrierName"] = carrierName
        appInstListRequest["GpsLocation"] = gpslocation

        return appInstListRequest
    }

    /// <#Description#>
    ///
    /// - Parameters:
    ///   - carrierName: <#carrierName description#>
    ///   - gpslocation: <#gpslocation description#>
    ///   - verifyloctoken: <#verifyloctoken description#>
    ///   - sessioncookie: <#sessioncookie description#>
    ///
    /// - Returns: API json/Dictionary

    public func createVerifyLocationRequest(_ carrierName: String,
                                            _ gpslocation: [String: Any],
                                            _ verifyloctoken: String,
                                            sessioncookie: String) // JT 18.12.26
        -> [String: Any] // json/Dictionary
    {
        var verifyLocationRequest = [String: Any]() // Dictionary/json

        verifyLocationRequest["ver"] = 1
        verifyLocationRequest["SessionCookie"] = sessioncookie
        verifyLocationRequest["CarrierName"] = carrierName
        verifyLocationRequest["GpsLocation"] = gpslocation
        verifyLocationRequest["VerifyLocToken"] = verifyloctoken

        return verifyLocationRequest
    }

    // Carrier name can change depending on cell tower.
    //

    /// createFindCloudletRequest
    ///
    /// - Parameters:
    ///   - carrierName: <#carrierName description#>
    ///   - gpslocation: <#gpslocation description#>
    ///   - sessioncookie: <#sessioncookie description#>
    /// - Returns: API  Dictionary/json

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
} // end MexSDK


// MARK:- MexUtil

// common
//

public class MexUtil // common to Mex... below
{
    public static let shared = MexUtil() // singleton
    
    
    // url  //  dme.mobiledgex.net:38001
    let baseDmeHost: String = "dme.mobiledgex.net"
    public let dmePort: UInt = 38001
    
    public let carrierNameDefault_TDG: String = "TDG"
    //    let carrierNameDefault_mexdemo: String = "mexdemo"
    
    public var baseDmeHostInUse: String = "TDG" // baseDmeHost
    public var carrierNameInUse: String = "mexdemo" // carrierNameDefault_mexdemo
    
    // API Paths:   See Readme.txt for curl usage examples
    public let registerAPI: String = "/v1/registerclient"
    public let appinstlistAPI: String = "/v1/getappinstlist"
    public let findcloudletAPI: String = "/v1/findcloudlet"
    
    
    
    public var closestCloudlet = ""
    
    private init() //   singleton called as of first access to shared
    {
        baseDmeHostInUse = baseDmeHost // dme.mobiledgex.net
        
     }
    
    // Retrieve the carrier name of the cellular network interface.
    public func getCarrierName() -> String
    {
        return carrierNameInUse
    }
    
    public func generateDmeHostPath(_ carrierName: String) -> String
    {
        if carrierName == ""
        {
            return carrierNameInUse + "." + baseDmeHostInUse
        }
        return carrierName + "." + baseDmeHostInUse
    }
    
    public func generateBaseUri(_ carrierName: String, _: UInt) -> String
    {
        return "https://\(generateDmeHostPath(carrierName)):\(dmePort)"
    }
} // end MexUtil


// MARK: -

// MARK: MexRegisterClient

//  gets sessioncookie  used by getApps and verifyLoc
//
public class MexRegisterClient
{
    public static let shared = MexRegisterClient() // singleton

    public var tokenserveruri = "" // set by RegisterClient
    public var sessioncookie = "" // set by RegisterClient    // used by getApp and verifyLoc

    // Color of user todo
    public static let COLOR_NEUTRAL: UInt32 = 0xFF67_6798 // default
    public static let COLOR_VERIFIED: UInt32 = 0xFF00_9933
    public static let COLOR_FAILURE: UInt32 = 0xFFFF_3300
    public static let COLOR_CAUTION: UInt32 = 0xFF00_B33C // Amber: ffbf00;

    public var future: Future<[String: AnyObject], Error>? // async result (captured by async?)

    var futureEdge: Future<[String: AnyObject], Error>? // async result (captured by async?)
    var futureCloud: Future<[String: AnyObject], Error>? // async result (captured by async?)

    private init() // singleton
    {} // init

    deinit
    {
        NotificationCenter.default.removeObserver(self)
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

//    func createRegisterClientRequest()
//        -> [String: Any] // Dictionary/json
//    {
//        let u = MexSDK.shared // JT 18.12.26
//
//        var regClientRequest = [String: String]() // Dictionary/json regClientRequest
//
//        regClientRequest["ver"] = "1"
//        regClientRequest["AppName"] = u.appName
//        regClientRequest["DevName"] = u.devName
//        regClientRequest["AppVers"] = u.appVersionStr
//
//        return regClientRequest
//    }

    func createRegisterClientRequest(appName: String, devName: String, appVers: String)
        -> [String: Any] // Dictionary/json
    {
        var regClientRequest = [String: String]() // Dictionary/json regClientRequest

        regClientRequest["ver"] = "1"
        regClientRequest["AppName"] = appName
        regClientRequest["DevName"] = devName
        regClientRequest["AppVers"] = appVers

        return regClientRequest
    }

    // MARK: registerClientNow

    
    public func registerClientNow(appName: String, devName: String,  appVers: String) // called by top right menu  //  "1.0"
    {
        Swift.print("registerClientNow") // log
        Swift.print("Register MEX client.") // log
        Swift.print("====================\n") // log

        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        Swift.print("\(baseuri)") // log

        // 🔵 API
        let registerClientRequest = MexRegisterClient.shared.createRegisterClientRequest(appName:appName, devName: devName, appVers: appVers)

        let urlStr = baseuri + MexUtil.shared.registerAPI

        let future =
            MexSDK.shared.postRequest(urlStr, registerClientRequest, "RegisterClient1")

        MexRegisterClient.shared.future = future

        MexRegisterClient.shared.future!.on(
            success:
            {
                print("RegisterClient1 received value: \($0)")

                let registerClientReply = $0 as [String: Any]

                self.sessioncookie = (registerClientReply["SessionCookie"] as! String) // save for later

                self.tokenserveruri = (registerClientReply["TokenServerURI"] as! String) // save for later

                Logger.shared.log(.network, .info, " save sessioncookie for later: \n \(self.sessioncookie)  \n") // Log

                Logger.shared.log(.network, .info, " save tokenserveruri for later: \n \(self.tokenserveruri)  \n") // Log

                MexRegisterClient.shared.registerClientResult(registerClientReply) // Log

                // MexRegisterClient.shared.getLocaltionUpdates()

                NotificationCenter.default.post(name: NSNotification.Name(rawValue: "Client registered"), object: nil)
                NotificationCenter.default.post(name: NSNotification.Name(rawValue: "getLocaltionUpdates"), object: nil)

                // SKToast.show(withMessage: "Client registered")  // UI

                ////Log.logger.name = "RegisterClient"    // Log
                ////logw("\nRegisterClient result: \(registerClientReply)")   // Log
            },
            failure: { print("RegisterClient failed with error: \($0)")

            },
            completion: { _ = $0 // print("completed with result: \($0)")
            }
        )
    }


} // end MexRegisterClient

// MARK: -

// MARK: MexGetAppInst

public class MexGetAppInst
{
    public static let shared = MexGetAppInst()

    public var future: Future<[String: AnyObject], Error>?

    private init() // singleton
    {}

    deinit
    {
        //        NotificationCenter.default.removeObserver(self)
    }

    public func getAppInstNow(gpslocation: [String: Any]) // called by top right menu
    {
        Swift.print("GetAppInstList")
        Swift.print(" MEX client.")
        Swift.print("====================\n")

        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        Swift.print("\(baseuri)")

        // let loc = retrieveLocation()

        let getAppInstListRequest = MexSDK.shared.createGetAppInstListRequest(
            carrierName: MexUtil.shared.carrierNameDefault_TDG,
            gpslocation: gpslocation,
            sessioncookie: MexRegisterClient.shared.sessioncookie
        ) // JT 19.01.31

        // 🔵
        let urlStr = baseuri + MexUtil.shared.appinstlistAPI
        future = MexSDK.shared.postRequest(urlStr, getAppInstListRequest, "GetAppInstlist1")

        future!.on(success:
            {
                print("GetAppInstlist1 received value: \($0)")

                let d = $0 as [String: Any]

                NotificationCenter.default.post(name: NSNotification.Name(rawValue: "processAppInstList"), object: d)

            },
                   failure: { print("failed with error: \($0)") },
                   completion: { _ = $0 // print("completed with result: \($0)" )

        })
    }
} // end MexGetAppInst

// MARK: -

// MARK: findNearestCloudlet

public class MexFindNearestCloudlet
{
    public static let shared = MexFindNearestCloudlet()

    private init() //   singleton called as of first access to shared
    {
        Swift.print("MexFindNearestCloudlet")
    }

    public func findNearestCloudlet(gpslocation: [String: Any]) //   called by top right menu
    {
        Swift.print("Finding nearest Cloudlet appInsts matching this Mex client.")
        Swift.print("===========================================================")

        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.getCarrierName(), MexUtil.shared.dmePort)
        //  let loc = retrieveLocation()

        let findCloudletRequest = MexSDK.shared.createFindCloudletRequest(MexUtil.shared.carrierNameDefault_TDG, gpslocation)

        let urlStr = baseuri + MexUtil.shared.findcloudletAPI
        //        Log.logger.name = "FindCloudlet"
        //       logw("\n findCloudlet url\n \(urlStr)")
        //        logw("\n findCloudletRequest:\n \(findCloudletRequest)")

        // 🔵 API
        MexRegisterClient.shared.future = MexSDK.shared.postRequest(urlStr, findCloudletRequest, "FindCloudlet1")

        MexRegisterClient.shared.future!.on(
            success:
            {
                print("GetToken received value: \($0)")

                let d = $0 as [String: Any]

                //                Log.logger.name = "FindCloudlet"
                //                logw("\n FindCloudlet result:\n \(d)")

                NotificationCenter.default.post(name: NSNotification.Name(rawValue: "processFindCloudletResult"), object: d)

            },
            failure: { print("failed with error: \($0)") },
            completion: { _ = $0 // print("completed with result: \($0)")
            }
        )
    }
} // end MexFindNearestCloudlet

// MARK: -

// MARK: MexVerifyLocation

public class MexVerifyLocation
{
    public static let shared = MexVerifyLocation()

    var future: Future<[String: AnyObject], Error>? // async result (captured by async?)
    var futureForGetToken: Future<[String: AnyObject], Error>? // async result (captured by async?)

    var location = [String: Any]()

    private init() // singleton
    {}

    public func doVerifyLocation(gpslocation: [String: Any]) // call by top right menu
    {
        location = gpslocation
        // Produces a new request. Now with sessioncooke and token initialized.

        Swift.print("Verify Location of this Mex client.")
        Swift.print("===================================\n\n")

 
        if MexRegisterClient.shared.tokenserveruri.count == 0
        {
            Swift.print("ERROR: TokenURI is empty!")
            //     let empty = [String: Any]()
            return
        }
        else
        {
            Swift.print("VerifyLocation getToken \(MexRegisterClient.shared.tokenserveruri)") // todo

            if MexRegisterClient.shared.tokenserveruri.count != 0
            {
                futureForGetToken = getToken(MexRegisterClient.shared.tokenserveruri) // async, will call verify after getting token

                futureForGetToken!.on(
                    success:
                    {
                        print("GetToken received value: \($0)")

                        let d = $0 as [String: Any]

                        let verifyLocToken = d["token"] as! String

                        Logger.shared.log(.network, .info, " got verifyLocToken: \n \(verifyLocToken) used by verifyLocation  \n") // JT 19.01.05
                        self.verifyLocation(verifyLocToken)

                    },
                    failure: { print("failed with error: \($0)") },
                    completion: { _ = $0 // print("completed with result: \($0)")
                    }
                )
            }
            else
            {
                Swift.print("No URI to get token!")
            }
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

    private func getToken(_ uri: String) -> Future<[String: AnyObject], Error> // async // JT 19.01.31
    {
        Swift.print("In Get Token")

        Swift.print("\(uri)")

        let promise = MexSDK.shared.postRequest(uri, [String: Any](), "GetToken") // async
        // NOTE special case: "GetToken" fails and its error result is parsed and returned as success

        return promise // JT 19.01.31
    }

    func verifyLocation(_ tokenReply: String)
    {
        Swift.print("VerifyLocation: Retrieved token: [ \(tokenReply) ]")

        //  let loc = retrieveLocation()

        let verifyLocationRequest = createVerifyLocationRequest(MexUtil.shared.carrierNameInUse, location, "")

        // Update request with the new token:
        // json tokenizedRequest;
        var tokenizedRequest = [String: Any]() // Dictionary/json
        tokenizedRequest += verifyLocationRequest // Dictionary/json

        tokenizedRequest["VerifyLocToken"] = tokenReply

        Swift.print("Verifylocation actual call...")

        let baseuri = MexUtil.shared.generateBaseUri(MexUtil.shared.carrierNameInUse, MexUtil.shared.dmePort)
        let verifylocationAPI: String = "/v1/verifylocation"

        let uri = baseuri + verifylocationAPI

        //        Log.logger.name = "Verifylocation"
        //        logw("\n VerifylocationRequest url:\n \(uri)")
        //        logw("\n VerifylocationRequest:\n \(tokenizedRequest)")

        // 🔵
        future = MexSDK.shared.postRequest(uri, tokenizedRequest, "Verifylocation1")

        future!.on(success: { print("Verifylocation1 received value: \($0)")
            let d = $0 as [String: Any]
            //            Log.logger.name = "Verifylocation"
            //            logw("\n VerifylocationResult:\n \(d)")    //  to file and console

            // Swift.print("Verifylocation1 result \(d)")

            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "Verifylocation success"), object: d) // JT 19.01.31

        },
                   failure: { print("Verifylocation failed with error: \($0)")

            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "Verifylocation failure"), object: nil) // JT 19.01.31
        },
                   completion: { _ = $0 // print("completed with result: \($0)")

        })
    }
} // end MexVerifyLocation

// MARK: -

public extension Dictionary
{
    static func += (lhs: inout [Key: Value], rhs: [Key: Value])
    {
        lhs.merge(rhs) { $1 }
    }

    static func + (lhs: [Key: Value], rhs: [Key: Value]) -> [Key: Value]
    {
        return lhs.merging(rhs) { $1 }
    }
}
