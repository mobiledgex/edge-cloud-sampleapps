/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System.Collections;
using System.Collections.Generic;
using UnityEngine;

using System;

namespace MexPongGame
{
  public class PlayerControls : MonoBehaviour
  {
    public string uuid;
    public bool ownPlayer = false;

    // Keyboard input:
    public KeyCode moveUp = KeyCode.W;
    public KeyCode moveDown = KeyCode.S;
    public float speed = 15f;
    public float boundX = 4.0f; //
    public float boundY = 2.25f;

    public Rigidbody2D rb2d;

    float width;
    float height;

    void Awake()
    {
      // Swap positions for horizontal.
      width = (float)Screen.height / 2f;
      height = (float)Screen.width / 2f;
    }

    // Start is called before the first frame update
    void Start()
    {
      rb2d = GetComponent<Rigidbody2D>();

      // Not Smart, but we're just going to register the client.
    }

    // Update is called once per frame
    // Note: rigid body physics is set to kinematics.
    void Update()
    {
      if (!ownPlayer)
      {
        return;
      }
      // "Boost" mode.
      if (Input.touchCount > 0)
      {
        Touch touch = Input.GetTouch(0);

        // Move the cube if the screen has the finger moving.
        if (touch.phase == TouchPhase.Moved)
        {
          Vector3 p = Camera.main.ScreenToWorldPoint(touch.position);
          p.x = transform.position.x;
          p.z = transform.position.z;
          rb2d.MovePosition(p);
        }
      }

      if (uuid == "")
      {
        return; // Not a player.
      }

      var vel = rb2d.velocity;
      if (Input.GetKey(moveUp))
      {
        vel.y = speed;
      }
      else if (Input.GetKey(moveDown))
      {
        vel.y = -speed;
      }
      else
      {
        vel.y = 0; // No more input.
      }
      rb2d.velocity = vel;

      // Bound Limits:
      var pos = transform.position;
      if (pos.y > boundY)
      {
        pos.y = boundY;
      }
      else if (pos.y < -boundY)
      {
        pos.y = -boundY;
      }
      transform.position = pos;
    }

    public void setPosition(Position position)
    {
      var pos = transform.position;
      pos.x = position.x;
      pos.y = position.y;
      transform.position = pos;
    }

    public void setVelocity(Velocity velocity)
    {
      var vel = rb2d.velocity;
      vel.x = velocity.x;
      vel.y = velocity.y;
      rb2d.velocity = vel;
    }
  }
}