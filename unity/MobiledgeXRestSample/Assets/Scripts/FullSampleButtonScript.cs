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

﻿using UnityEngine;

using UnityEngine.UI;
using DistributedMatchEngine;

public class FullSampleButtonScript : MonoBehaviour {
    public Button startMexSampleButton;
    public MexSample mexSample;

    void Start () {
        Button btn = startMexSampleButton.GetComponent<Button>();
        btn.onClick.AddListener(TaskOnClick);
    }

  void TaskOnClick()
  {
    Debug.Log("You have clicked the button!");
    mexSample.RunSampleFlow();
  }
}
