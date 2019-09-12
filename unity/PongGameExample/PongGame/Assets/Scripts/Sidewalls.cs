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

namespace MexPongGame
{
  public class Sidewalls : MonoBehaviour
  {
    GameObject gameManagerObj;


    private void Start()
    {
      gameManagerObj = GameObject.FindGameObjectWithTag("GameManager");

    }

    void OnTriggerEnter2D(Collider2D hitInfo)
    {
      if (hitInfo.name == "Ball")
      {
        GameManager gameManager = gameManagerObj.GetComponent<GameManager>();

        string wallName = transform.name;

        // async check scoring on "goal" wall hit, and send to server.
        gameManager.Score(wallName); 

        hitInfo.gameObject.SendMessage("RestartGame", 1.0f, SendMessageOptions.RequireReceiver);
      }
    }

  }
}
