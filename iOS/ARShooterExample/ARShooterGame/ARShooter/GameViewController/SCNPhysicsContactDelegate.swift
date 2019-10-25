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
//  SCNPhysicsContactDelegate.swift
//  ARShooter

import Foundation
import SceneKit

extension GameViewController: SCNPhysicsContactDelegate {
    
    // Called when some contact happens (Case: Bullet hits another bullet) (TODO: Bitwise & instead of ==)
    func physicsWorld(_ world: SCNPhysicsWorld, didEnd contact: SCNPhysicsContact) {
        DispatchQueue.main.async { // Run on background thread
            let nodeA = contact.nodeA
            let nodeB = contact.nodeB
            if nodeA.physicsBody?.categoryBitMask == BitMaskCategory.bullet.rawValue {  // if node A has the bitmask category of a bullet
                self.Target = nodeB
                if nodeA.name != nil {
                    self.peers[nodeA.name!]! += 1
                }
            } else if nodeB.physicsBody?.categoryBitMask == BitMaskCategory.bullet.rawValue {
                self.Target = nodeA
                if nodeB.name != nil {
                    self.peers[nodeB.name!]! += 1
                }
            }
            self.scoreTextView.text = self.peers.description
        }
        let confetti = SCNParticleSystem(named: "Media.scnassets/Confetti.scnp", inDirectory: nil) // gets the confetti scene particle from the Media.scnassets file
        confetti?.loops = false
        confetti?.particleLifeSpan = 4 // Lifespan of animation in seconds
        confetti?.emitterShape = Target?.geometry
        let confettiNode = SCNNode()
        confettiNode.addParticleSystem(confetti!)
        confettiNode.position = contact.contactPoint // places confetti exactly where the collision occured
        self.sceneView.scene.rootNode.addChildNode(confettiNode)
        Target?.removeFromParentNode() // makes the box (egg) disappear
    }
}
