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

using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

// We need this one for importing our IOS functions
using System.Runtime.InteropServices;

namespace MexPongGame
{
  public interface ICarrierInfo
  {
    string GetCurrentCarrierName();
  }

  public class PlatformIntegration : ICarrierInfo
  {
    [DllImport("__Internal")]
    private static extern string _getCurrentCarrierName();

#if UNITY_ANDROID // PC android target builds to through here as well.
  public string GetCurrentCarrierName()
  {
    string networkOperatorName = "";
    
    return networkOperatorName;
  }
#elif UNITY_IOS
    public string GetCurrentCarrierName()
    {
      string networkOperatorName = "";
      
      return networkOperatorName;
    }
#else
  public string GetCurrentCarrierName()
  {
    Debug.Log("GetCurrentCarrierName is NOT IMPLEMENTED");
    return "";
  }
#endif

  }
}