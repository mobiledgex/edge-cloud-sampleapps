// Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
// MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//  LoginViewController.swift
//  ARShooter


import UIKit
import MobiledgeXiOSLibrary
import Promises
import SocketIO

class LoginViewController: UIViewController {
    
    @IBOutlet weak var gameIDField: UITextField!
    @IBOutlet weak var userNameField: UITextField!
    @IBOutlet weak var submitButton: UIButton!
    
    var userName: String?
    var gameID: String?
    
    // MatchingEngine variables
    var matchingEngine: MobiledgeXiOSLibrary.MatchingEngine!
    
    var dmeHost: String?
    var dmePort: UInt16?
    
    var appName: String?
    var appVers: String?
    var orgName: String!
    var carrierName: String?
    var authToken: String?
    var uniqueIDType: MobiledgeXiOSLibrary.MatchingEngine.IDTypes?
    var uniqueID: String?
    var cellID: UInt32?
    var tags: [MobiledgeXiOSLibrary.MatchingEngine.Tag]?
    var host: String?
    var port: UInt16?
    var internalPort: UInt16 = 3838 // internal port I specified when deploying my app
    var location: MobiledgeXiOSLibrary.MatchingEngine.Loc?
    
    var demo = true
    
    var manager: SocketManager?
    
    // MatchingEngine API return objects
    var registerPromise: Promise<MobiledgeXiOSLibrary.MatchingEngine.RegisterClientReply>?
    var findCloudletPromise: Promise<MobiledgeXiOSLibrary.MatchingEngine.FindCloudletReply>?
    var verifyLocationPromise: Promise<MobiledgeXiOSLibrary.MatchingEngine.VerifyLocationReply>?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        userNameField.delegate = self
        gameIDField.delegate = self
        
        setUpMatchingEngineParameters()
        DispatchQueue.main.async {
            self.callMatchingEngineAPIs()
            self.getWebsocketConnection()
        }
    }
    
    func setUpMatchingEngineParameters() {
        SKToast.show(withMessage: "MatchingEngine not setup yet")
    } 
    
    // This function shows a couple of the MatchingEngine APIs
    func callMatchingEngineAPIs() {
        SKToast.show(withMessage: "RegisterClient not implemented yet")
    }
    
    // This function shows the GetConnection workflow
    func getWebsocketConnection() {
        SKToast.show(withMessage: "GetConnection workflow not implemented yet")
    }
    
    private func moveToGameViewController(gameViewController: GameViewController) {
        // switch to GameViewController
        addChild(gameViewController)
        view.addSubview(gameViewController.view)
        gameViewController.didMove(toParent: self)
    }
    
    @IBAction func pressSubmit(_ sender: UIButton) {
        let gameViewController = self.storyboard?.instantiateViewController(withIdentifier: "game") as! GameViewController
        // Make sure gameID and userName are not nil
        if gameID == nil {
            if gameIDField.text == "" {
                return
            }
        }
        if userName == nil {
            if userNameField.text == "" {
                return
            }
        }
        userNameField.isEnabled = false
        gameIDField.isEnabled = false
        
        if self.manager == nil {
            print("No websocket connection yet")
            return
        }
        
        SKToast.show(withMessage: "Pass MatchingEngine and GameState variables to GameViewController")
        moveToGameViewController(gameViewController: gameViewController)
    }
}
