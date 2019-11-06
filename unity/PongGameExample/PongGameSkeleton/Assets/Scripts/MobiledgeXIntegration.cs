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
using System.Collections.Generic;
using UnityEngine;

using MexPongGame;
using DistributedMatchEngine;

using System.Threading.Tasks;

/*
 * MobiledgeX MatchingEngine SDK integration has an additional application side
 * "PlatformIntegration.cs/m" file for Android, IOS, or other platform integration
 * with Unity.
 * 
 * This is necessary to retrieve carrier information so that the SDK can provide
 * Edge Cloudlet discovery.
 */
public class MobiledgeXIntegration
{
  PlatformIntegration pIntegration;
  MatchingEngine me;

  /*
   * These are "carrier independent" settings for demo use:
   */
  public string carrierName { get; set; } = "TDG"; // carrierName depends on the available subscriber SIM card and roaming carriers, and must be supplied by platform API.
  public string devName { get; set; } = "MobiledgeX"; // Your developer name.
  public string appName { get; set; } = "Pong"; // Your appName, if you have created this in the MobiledgeX console.
  public string appVers { get; set; } = "1.0";
  public string developerAuthToken { get; set; } = ""; // This is an opaque string value supplied by the developer.

  public string dmeHost { get; set; } = MatchingEngine.fallbackDmeHost;
  public uint dmePort { get; set; } = MatchingEngine.defaultDmeRestPort;

  // Set to true and define the DME if there's no SIM card to find appropriate geolocated MobiledgeX DME (client is PC, UnityEditor, etc.)...
  public bool useDemo { get; set; } = false;

  public MobiledgeXIntegration()
  {
    pIntegration = new PlatformIntegration();
    me = new MatchingEngine();

    // Set the platform specific way to get SIM carrier information.
    me.carrierInfo = pIntegration;
  }

  public string GetCarrierName()
  {
    return me.carrierInfo.GetCurrentCarrierName();
  }

  public async Task<Loc> GetLocationFromDevice()
  {
    // Location is ephemeral, so retrieve a new location from the platform. May return 0,0 which is
    // technically valid, though less likely real, as of writing.
    Loc loc = await LocationService.RetrieveLocation();

    // If in UnityEditor, 0f and 0f are hard zeros as there is no location service.
    if (loc.longitude == 0f && loc.latitude == 0f)
    {
      // Likely not in the ocean. We'll chose something for demo FindCloudlet purposes:
      loc.longitude = -121.8863286;
      loc.latitude = 37.3382082;
    }
    return loc;
  }

  // These are just thin wrappers over the SDK to show how to use them:
  // Call once, or when the carrier changes:
  public async Task<bool> Register()
  {
    Debug.Log("Register is NOT IMPLEMENTED");
    return false;
  }

  public async Task<FindCloudletReply> FindCloudlet()
  {
    Debug.Log("FindCloudlet is NOT IMPLEMENTED");
    FindCloudletReply reply = null;

    return reply;
  }

  public async Task<bool> VerifyLocation()
  {
    Debug.Log("VerifyLocation is NOT IMPLEMENTED");
    return false;
  }
}
