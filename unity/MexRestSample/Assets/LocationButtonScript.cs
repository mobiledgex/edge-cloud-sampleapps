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

﻿using UnityEngine;
using UnityEngine.UI;

public class LocationButtonScript : MonoBehaviour
{
  public Button locationUpdateButton;

  void Start()
  {
    Button btn = locationUpdateButton.GetComponent<Button>();
    btn.onClick.AddListener(TaskOnClick);
  }

  async void TaskOnClick()
  {
    Debug.Log("location button clicked!");
    var loc = await LocationService.RetrieveLocation();
    if (loc == null)
    {
      Debug.Log("No location returned!");
    }
    else
    {
      Debug.Log("Location: Lat: " + loc.latitude + ", Long: " + loc.longitude);
    }
  }

}

