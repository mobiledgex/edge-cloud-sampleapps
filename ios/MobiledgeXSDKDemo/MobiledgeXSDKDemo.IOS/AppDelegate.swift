//
//  AppDelegate.swift
//  MobiledgeXSDKDemo.IOS
//
//  Created by meta30 on 10/23/18.
//  Copyright © 2018 MobiledgeX. All rights reserved.
//

import GoogleMaps // JT 18.10.23
import GoogleSignIn // JT 18.10.24 #import <Google/SignIn.h>
// import ProtoRPC    // JT 18.10.23
import SideMenu // JT 18.11.12
import UIKit

var services: Any? // JT 18.10.25

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate //, GIDSignInDelegate // JT 18.10.24 GIDSignInUIDelegate
{
    let kAPIKey = "AIzaSyCNWqii1sVJ0NGU12UvRBbvDhqBqpcSyP0" // JT 18.10.20  // JT 18.10.22
    let kClientID = "406366254062-ci2micbnconnti5hhb7ltku9natmegct.apps.googleusercontent.com" // JT 18.11.17 for google signin, tmp use mine

    var window: UIWindow?

    func application(_: UIApplication, didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]?) -> Bool
    {
        // Override point for customization after application launch.

        #if true // JT 18.10.24   // JT 18.11.17
            GIDSignIn.sharedInstance().clientID = kClientID // JT 18.11.17
            GIDSignIn.sharedInstance().delegate = self

        #endif
        GMSServices.provideAPIKey(kAPIKey) // JT 18.10.25 for maps
        services = GMSServices.sharedServices()

        // setupSideMenu()     // JT 18.11.12 nogo, would like

          // JT 18.11.25 rotate log
         Log.logger.rename(0)  // JT 18.11.25
        // This writes to the log
        logw("•• write to the log")    // JT 18.11.22 and console

        return true
    }

    func application(_: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool // JT 18.11.17
    {
        return GIDSignIn.sharedInstance().handle(url as URL?,
                                                 sourceApplication: options[UIApplication.OpenURLOptionsKey.sourceApplication] as? String,
                                                 annotation: options[UIApplication.OpenURLOptionsKey.annotation])
    }

    fileprivate func setupSideMenu() // JT 18.11.12
    {
        // Define the menus
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let nav1 = storyboard.instantiateViewController(withIdentifier: "Settings") as? UISideMenuNavigationController // LeftMenuNavigationController

        let nav2 = storyboard.instantiateViewController(withIdentifier: "RightMenuNavigationController") as? UISideMenuNavigationController // RightMenuNavigationController

        SideMenuManager.default.menuLeftNavigationController = nav1
        SideMenuManager.default.menuRightNavigationController = nav2

        // Enable gestures. The left and/or right menus must be set up above for these to work.
        // Note that these continue to work on the Navigation Controller independent of the View Controller it displays!
        SideMenuManager.default.menuAddPanGestureToPresent(toView: (nav2?.navigationBar)!)
        SideMenuManager.default.menuAddScreenEdgePanGesturesToPresent(toView: (nav2?.view)!) // JT 18.09.22
    }

    func applicationWillResignActive(_: UIApplication)
    {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_: UIApplication)
    {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_: UIApplication)
    {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_: UIApplication)
    {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_: UIApplication)
    {
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
        if error != nil
        {
            // Swift.print("Error: GIDSignIn: \(error)")  // JT 18.11.17
            // Perform any operations on signed in user here.
            print("GIDSignIn: \(error.localizedDescription)") // JT 18.11.17 todo ignore Cancel
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
            // JT 18.11.17 todo what to save
            Swift.print("GIDSignIn \(user!), what todo with result?") // JT 18.11.17
        }
    }
    
}

