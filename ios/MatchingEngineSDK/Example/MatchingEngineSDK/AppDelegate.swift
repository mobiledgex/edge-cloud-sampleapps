//
//  AppDelegate.swift
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

// Note: this demo app is meant to demonstrate mobiledgex apis

import UIKit

import GoogleMaps
import GoogleSignIn

import NSLogger // JT 19.01.07 instramentation


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate
{
    var services: Any? //   = GMSServices.sharedServices()

    #warning ("Action item: you need to replace  the values assigned: kAPIKey/kClientID ")      // JT 19.02.11
    //    #error ("Action item: you need to replace  the values assigned: kAPIKey/kClientID ")      // JT 19.02.11

    // replace the following two values with your own
    //
    let kAPIKey = "AIzaSyCNWqii1sVJ0NGU12UvRBbvDhqBqpcSyP0" // JT my personal APIKey - todo: use your own
    let kClientID = "406366254062-ci2micbnconnti5hhb7ltku9natmegct.apps.googleusercontent.com" // JT   for google signin, tmp use mine

    var window: UIWindow?

    
    /// Where it all starts
    ///
    /// Do Google GIDSignIn and GMSServices
   /// init loggng options
    
    func application(_: UIApplication, didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]?) -> Bool
    {
        // Swift.print("\(#function)")

        #if true    // GIDSignIn
            GIDSignIn.sharedInstance().clientID = kClientID
            GIDSignIn.sharedInstance().delegate = self
        #endif
        
        GMSServices.provideAPIKey(kAPIKey) // for maps
        services = GMSServices.sharedServices()

        // rotate log
  ////      Log.logger.rename(0)  // usage: logw("•• write to the log") // JT 19.01.28

        // ---
        // NSLogger options
        
        enum loggerOption : UInt32 {
            case kLoggerOption_LogToConsole                        = 0x01
            case kLoggerOption_BufferLogsUntilConnection            = 0x02
            case kLoggerOption_BrowseBonjour                        = 0x04
            case kLoggerOption_BrowseOnlyLocalDomain                = 0x08
            case kLoggerOption_UseSSL                                = 0x10
        };  // JT 19.01.07
        
        let options:UInt32 =
            loggerOption.kLoggerOption_BufferLogsUntilConnection.rawValue
                | loggerOption.kLoggerOption_BrowseBonjour.rawValue
                |  loggerOption.kLoggerOption_BrowseOnlyLocalDomain.rawValue
        let lptr :OpaquePointer? = nil
        LoggerSetOptions( lptr,  options)   // JT 19.01.04  -Dont capture console
        
        return true
    }

    func application(_: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool
    {
        // Swift.print("\(#function)")
        return GIDSignIn.sharedInstance().handle(url as URL?,
                                                 sourceApplication: options[UIApplication.OpenURLOptionsKey.sourceApplication] as? String,
                                                 annotation: options[UIApplication.OpenURLOptionsKey.annotation])
    }



    func applicationWillResignActive(_: UIApplication)
    {
        // Swift.print("\(#function)")

        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_: UIApplication)
    {
        // Swift.print("\(#function)")

        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_: UIApplication)
    {
        // Swift.print("\(#function)")

        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_: UIApplication)
    {
        // Swift.print("\(#function)")

        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_: UIApplication)
    {
        // Swift.print("\(#function)")

        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }

    // MARK: -
    
    
//    Sign out the user
//
//    You can use the signOut method of the GIDSignIn object to sign out your user on the current device, for example:
//
//    @IBAction func didTapSignOut(_ sender: AnyObject) {
//        GIDSignIn.sharedInstance().signOut()
//    }
}

extension  AppDelegate: GIDSignInDelegate
{
    func sign(_: GIDSignIn!,
              didSignInFor user: GIDGoogleUser!,
              withError error: Error!)
    {
        // Swift.print("\(#function)")

        if error != nil
        {
            // Swift.print("Error: GIDSignIn: \(error)")
            // Perform any operations on signed in user here.
            print("GIDSignIn: \(error.localizedDescription)") //   todo ignore Cancel
            // ...
        }
        else
        {
            let userId: String = user.userID // For client-side use only!
            let idToken: String = user.authentication.idToken // Safe to send to the server
            let fullName: String = user.profile.name
//            let givenName: String = user.profile.givenName
//            let familyName: String = user.profile.familyName
            let email: String = user.profile.email
          
            Swift.print("\(userId), \(idToken), \(fullName), \(email)")
            // todo what to save
            Swift.print("GIDSignIn \(user!), what todo with result?")
        }
    }
    
}

