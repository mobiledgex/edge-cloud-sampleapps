//
//  MobiledgeX.SDK.swift
//  MobiledgeXSDKDemo.IOS
//
//  Created by Jean Tantra, Metatheory.com on 12/26/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import Foundation

import Alamofire    // dependency
import NSLogger   // JT

/// MexSDK MobiledeX SDK APIs

public class MexSDK // JT 19.01.29
{
    static let shared = MexSDK()     /// singleton access: MexSDK.shared.whatever

    var sessionManager: SessionManager? // alamofire: creeated based on host trust
    // need different SessionManager for each host

    let headers: HTTPHeaders = [
        "Accept": "application/json",
        "Content-Type": "application/json", // This is the default
        "Charsets": "utf-8",
        ]   /// headers HTTPHeaders:
    
    var appName = "" /// Your application name. was "EmptyMatchEngineApp"
    var devName = "" /// Your developer name
    
    let appVersionStr = "1.0"   // used by createRegisterClientRequest

    private init() // singleton
    {
        
    } // init
    
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
  
    func postRequest(_ uri: String,
                             _ request: [String: Any],  // Dictionary/json
        _ postName: String = "postRequestReplyLogger") // this is posted after results
        -> Future<[String: AnyObject], Error>
    {
        let promise = Promise<[String: AnyObject], Error>() // completion callback
        
        Swift.print("URI to post to:\n \(uri)\n")

        if false
        {
            Swift.print("\(request)")
            ///logw("•uri:\n\(uri)\n") // JT 18.11.26 log to file
        }
            LogMarker(postName) // JT 19.01.04

        Logger.shared.log(.network, .info, postName + " request\n \(request) \n" )   // JT 19.01.04

        //  logw("•request:\n\(request)\n") // JT 18.11.26 log to file
        
        dealWithTrustPolicy(uri) // certs
       // Swift.print("==========\n")
        
        let requestObj = MexSDK.shared.sessionManager!.request(
            uri,
            method: .post,
            parameters: request,
            encoding: JSONEncoding.default,
            headers: MexSDK.shared.headers
            ).responseJSON
            { response in
                if false
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
                    //logw("\n \(curl)\n")      // JT 18.12.05
                    //logw("postName:\(postName)\nresult:\n\(json)") //   log to file
                    
                    Logger.shared.log(.network, .info, postName + " reply json\n \(json) \n" )   // JT 19.01.04

                }
                
              //  Swift.print("\(response)")
                Swift.print("result \(response.result)")
                Swift.print("data \(response.data!)")
                
                //            print(response.metrics)
                //           print(response.timeline)
        }
        
        if false    // JT 18.12.28
        {
            debugPrint(requestObj) // dump curl
        }
        
        Logger.shared.log(.network, .info, postName + " curl\n \(requestObj.debugDescription) \n" )     // JT 19.01.05

        //Logger.shared.log(.network, .noise, postName + " xxx \n" )     // JT 19.01.05 gray

        return promise.future
    }
    
    // in general
    //
    
      ///  Deal with certificates, trust
      ///
      /// - Parameter url:
    
      func dealWithTrustPolicy(
        _ url: URLConvertible  // a string
        )
    {
        // let certificates = getCertificates()
        let certificates = ServerTrustPolicy.certificates() // alamo extension
        Swift.print("~~~certificates: \(certificates) ---")
      //  Logger.shared.log(.network, .info,  " certificates:\n \(certificates) \n" )    // JT 19.01.05
      Logger.shared.log(.network, .info,  " add these certificates to your curl below --cacert mex-ca.crt --cert mex-client.crt" )

        let trustPolicy = ServerTrustPolicy.pinCertificates(
            certificates: certificates,
            validateCertificateChain: true,
            validateHost: true
        )
        
        do
        {
            let whoToTrust = try url.asURL().host
            //     Swift.print("\(whoToTrust)")
            
            let trustPolicies = [whoToTrust!: trustPolicy]  // [String: ServerTrustPolicy]
            
            let policyManager = ServerTrustPolicyManager(policies: trustPolicies)
            
            
            MexSDK.shared.sessionManager = SessionManager(
                configuration: .default,
                serverTrustPolicyManager: policyManager
            )   // JT 18.12.26
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
    
    func createRegisterClientRequest( ver: String = "1", appName: String, devName: String, appVers: String)
        -> [String: Any] // Dictionary/json
    {
        var regClientRequest = [String: String]()     // Dictionary/json regClientRequest
        
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
    
    func createGetAppInstListRequest(_ carrierName: String, _ gpslocation: [String: Any], sessioncookie: String) -> [String: Any]   // JT 18.12.26
    {
        //   json findCloudletRequest;
        var appInstListRequest = [String: Any]()    // Dictionary/json
        
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
    
    func createVerifyLocationRequest(_ carrierName: String,
                                     _ gpslocation: [String: Any],
                                     _ verifyloctoken: String,
                                      sessioncookie: String)    // JT 18.12.26
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
    
    func createFindCloudletRequest(_ carrierName: String, _ gpslocation: [String: Any], sessioncookie: String ) -> [String: Any]
    {
        //    findCloudletRequest;
        var findCloudletRequest = [String: Any]() // Dictionary/json
        
        findCloudletRequest["vers"] = 1
        findCloudletRequest["SessionCookie"] = sessioncookie
        findCloudletRequest["CarrierName"] = carrierName
        findCloudletRequest["GpsLocation"] = gpslocation
        
        return findCloudletRequest
    }
 
}


// MARK: -

public extension Dictionary // JT 19.01.29
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


