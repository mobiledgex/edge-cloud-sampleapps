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
//  ARSCNViewDelegate.swift
//  ARShooter

import Foundation
import ARKit

extension GameViewController: ARSCNViewDelegate {
    
    // Part of ARSCNView Delegate class
    func renderer(_ renderer: SCNSceneRenderer, didAdd node: SCNNode, for anchor: ARAnchor) {
        // Renders other player's bullets when receive data from server
        if !worldMapConfigured {
            return
        }
        let bullet = renderBullet(transform: SCNMatrix4.init(anchor.transform))
        bullet.geometry?.firstMaterial?.diffuse.contents = UIColor.blue
        bullet.name = anchor.name
        bullet.runAction(SCNAction.sequence([SCNAction.wait(duration: 2.0), SCNAction.removeFromParentNode()])) // makes it as soon as the bullet is shot, it is removed after 2 seconds
        node.addChildNode(bullet)
    }
}
