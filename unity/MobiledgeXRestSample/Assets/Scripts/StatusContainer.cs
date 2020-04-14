/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
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

﻿using System.Collections.Generic;
using UnityEngine;

using UnityEngine.UI;

/* A (very) simple data container for scrollRect content. */
public class StatusContainer : MonoBehaviour
{

  public string status; // Need a list, or accessor.
  public Text scrollViewText;
  long count = 0;

  List<string> posts = new List<string>(3);

  // Use this for initialization
  void Start()
  {
    scrollViewText = GameObject.Find("/UICanvas/OutputScrollView/Viewport/Content/GRPCOutputText").GetComponent<Text>();
  }

  // Update is called once per frame
  void Update()
  {
    status = "[Sample Output]: " + count + "\n";
    foreach (string post in posts)
    {
      status += post + "\n";
    }
    count++;
    scrollViewText.text = status;
  }

  public void Post(string postText)
  {
    if (postText == null)
    {
      return;
    }
    // Trim first in line if needed.
    if (posts.Count == posts.Capacity)
    {
      posts.RemoveAt(0);
    }
    posts.Add(postText);
  }
}
