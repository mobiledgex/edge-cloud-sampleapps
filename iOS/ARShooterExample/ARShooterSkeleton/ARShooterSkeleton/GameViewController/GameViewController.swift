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
//  ViewController.swift
//  ARShooter

import ARKit
import SocketIO

enum BitMaskCategory: Int {
    case bullet = 1
    case target = 2
}

class GameViewController: UIViewController {
    
    @IBOutlet weak var scoreTextView: UITextView!
    @IBOutlet weak var sceneView: ARSCNView!
    @IBOutlet weak var infoLabel: UILabel!
    @IBOutlet weak var mappingStatusLabel: UILabel!
    @IBOutlet weak var sendMapButton: UIButton!
    
    let configuration = ARWorldTrackingConfiguration()
    var power: Float = 10
    var number: Int = 0
    var peerNumber: Int = 0
    var Target: SCNNode?
    var worldMapConfigured = false
    
    // Variables passed in from LoginViewController
    var userName: String?
    var gameID: String?
    var peers = [String: Int]()
    var host: String? // Host from findCloudlet (MatchingEngine)
    var port: Int? // Port from findCloudlet (MatchingEngine)
    
    // SocketIO Variables
    var manager: SocketManager?
    var socket: SocketIOClient!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Initialize socket and connect
        socket = manager!.defaultSocket
        setUpSocketCallbacks()
        socket.connect()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        configuration.planeDetection = .horizontal
        self.sceneView.session.run(configuration)
        sceneView.session.delegate = self // set session delegate to self
        sceneView.delegate = self
        self.sceneView.autoenablesDefaultLighting = true
        self.sceneView.scene.physicsWorld.contactDelegate = self // executes the physics world function
        self.sceneView.debugOptions = [ARSCNDebugOptions.showFeaturePoints]
        UIApplication.shared.isIdleTimerDisabled = true // disable the screen from getting dimmed, as the user will be taking some time to scan the world
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        sceneView.session.pause()
    }
    
    func renderBullet(transform: SCNMatrix4) -> SCNNode {
        let orientation = SCNVector3(-transform.m31,-transform.m32,-transform.m33) // orientation is encoded in the third column vector. -Z axis points from camera out
        let location = SCNVector3(transform.m41,transform.m42,transform.m43) // position of the camera/user. Translation vector is encoded in the fourth column vector
        let position = location
        let bullet = SCNNode(geometry: SCNSphere(radius: 0.1))
        bullet.position = position
        let body = SCNPhysicsBody(type: .dynamic, shape: SCNPhysicsShape(node: bullet, options: nil)) // needs the physics body in order to shoot the bullet, dynamic in order for our bullet to be affected by forces , shape of a bullet
        body.isAffectedByGravity = false
        bullet.physicsBody = body
    bullet.physicsBody?.applyForce(SCNVector3(orientation.x*power,orientation.y*power,orientation.z*power), asImpulse: true) // makes the sphere shoot like a bullet
        bullet.physicsBody?.categoryBitMask = BitMaskCategory.bullet.rawValue
        bullet.physicsBody?.contactTestBitMask = BitMaskCategory.target.rawValue
        return bullet
    }
    
    
    // sends bullet when user taps on the screen
    @IBAction func sendBullets(_ sender: UITapGestureRecognizer) {
        if !worldMapConfigured {
            return
        }
        guard let sceneView = sender.view as? ARSCNView else{return} // makes sure that the view you tapped on is the scene view
        guard let pointOfView = sceneView.pointOfView else{return} // receives the point of view of the sceneView
        let transform = pointOfView.transform // gets the transform matrix from point of view (4x4 matrix)
        let bullet = renderBullet(transform: transform)
        bullet.name = userName!
        bullet.geometry?.firstMaterial?.diffuse.contents = UIColor.red
        bullet.runAction(SCNAction.sequence([SCNAction.wait(duration: 2.0), SCNAction.removeFromParentNode()])) // makes it as soon as the bullet is shot, it is removed after 2 seconds
        let anchor = ARAnchor(name: userName!, transform: simd_float4x4(transform))
        print("send bullet anchor.name is \(anchor.name)")
        self.sceneView.scene.rootNode.addChildNode(bullet)
        guard let data = try? NSKeyedArchiver.archivedData(withRootObject: anchor, requiringSecureCoding: true) else{fatalError("can't encode anchor")}
        // send bullet to server
        socket.emit("bullet", gameID!, data)
    }
    
    // eggs are added to the AR World when add targets button is clicked
    @IBAction func addTargets(_ sender: Any) {
        for i in -2...2 {
            self.addegg(x: Float(i), y: 0, z: -2)
        }
    }
    
    // creates the eggnode and displays the egg image into ARSCNView
    func addegg(x: Float, y: Float, z: Float){
        let eggScene = SCNScene(named: "Media.scnassets/egg.scn")
        let eggNode = (eggScene?.rootNode.childNode(withName: "egg", recursively: false))! // creates the egg node
        eggNode.scale = SCNVector3(x: 0.5, y: 0.5, z: 0.5) // Make egg smaller
        eggNode.position = SCNVector3(x,y,z)
        eggNode.physicsBody = SCNPhysicsBody(type: .static, shape: SCNPhysicsShape(node: eggNode, options: nil)) // makes it so that the objecgt is in the form of a egg
        eggNode.physicsBody?.categoryBitMask = BitMaskCategory.target.rawValue
        eggNode.physicsBody?.contactTestBitMask = BitMaskCategory.bullet.rawValue
        self.sceneView.scene.rootNode.addChildNode(eggNode)
    }
    
    // Shares the AR World map with server, which will forward to other devices
    @IBAction func shareSession(_ sender: UIButton) {
        // Set origin to an anchor, so it is more stable
        guard let anchor = self.sceneView.session.currentFrame?.anchors.first else {
            return
        }
        self.sceneView.session.setWorldOrigin(relativeTransform: anchor.transform)
        // Gets world map and sends to server
        sceneView.session.getCurrentWorldMap { worldMap, error in
            guard let map = worldMap
                else { print("Error: \(error!.localizedDescription)"); return }
            guard let data = try? NSKeyedArchiver.archivedData(withRootObject: map, requiringSecureCoding: true)
                else { fatalError("can't encode map") }
            Swift.print("sending world map")
            self.worldMapConfigured = true
            self.socket.emit("worldMap", self.gameID!, data)
        }
    }
}

// method to add SCNVectors together
func +(left: SCNVector3, right: SCNVector3) -> SCNVector3{
    return SCNVector3Make(left.x + right.x, left.y + right.y, left.z + right.z)}
