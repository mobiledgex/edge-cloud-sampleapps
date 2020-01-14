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

using MobiledgeXPingPongGame;
using DistributedMatchEngine;

using System.Threading.Tasks;
using DistributedMatchEngine.PerformanceMetrics;

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
  public MatchingEngine me;
  public NetTest netTest;

  /*
   * These are "carrier independent" settings for demo use:
   */
  public string carrierName { get; set; } = "TDG"; // carrierName depends on the available subscriber SIM card and roaming carriers, and must be supplied by platform API.
  public string devName { get; set; } = "MobiledgeX"; // Your developer name.
  public string appName { get; set; } = "PingPong"; // Your appName, if you have created this in the MobiledgeX console.
  public string appVers { get; set; } = "1.0";
  public string developerAuthToken { get; set; } = ""; // This is an opaque string value supplied by the developer.

  // Override if there is a sdk demo DME host to use.
  public string dmeHost { get; set; } = MatchingEngine.fallbackDmeHost;
  public uint dmePort { get; set; } = MatchingEngine.defaultDmeRestPort;

  // Set to true and define the DME if there's no SIM card to find appropriate geolocated MobiledgeX DME (client is PC, UnityEditor, etc.)...
  public bool useDemo { get; set; } = false;

  public MobiledgeXIntegration()
  {
    // Set the platform specific way to get SIM carrier information.
    pIntegration = new PlatformIntegration();

    // The following is to allow Get{TCP, TLS, UDP}Connection APIs to return the configured
    // edge network path to your MobiledgeX AppInsts. Other connections will use the system
    // default network route.
    NetInterface netInterface = new SimpleNetInterface(pIntegration.NetworkInterfaceName);

    // Platform integration needs to initialize first:
    me = new MatchingEngine(pIntegration, netInterface);

    // Optional NetTesting.
    netTest = new NetTest(me);
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
  // Call once, or when the carrier changes. May throw DistributedMatchEngine.HttpException.
  public async Task<bool> Register()
  {
    // If MEX is reachable on your SIM card:
    string aCarrierName = GetCarrierName();
    string eCarrierName;
    if (useDemo)
    {
      eCarrierName = carrierName;
    }
    else
    {
      if (aCarrierName == null)
      {
        Debug.Log("Missing CarrierName for RegisterClient.");
        return false;
      }
      eCarrierName = aCarrierName;
    }

    RegisterClientRequest req = me.CreateRegisterClientRequest(eCarrierName, devName, appName, appVers, "" /* developer specific string blob */);
    Debug.Log("CarrierName: " + req.carrier_name);
    Debug.Log("DevName: " + req.dev_name);
    Debug.Log("AppName: " + req.app_name);
    Debug.Log("AppVers: " + req.app_vers);

    RegisterClientReply reply;
    if (useDemo)
    {
      reply = await me.RegisterClient(dmeHost, dmePort, req);
    }
    else
    {
      reply = await me.RegisterClient(req);
    }

    return (reply.status == ReplyStatus.RS_SUCCESS);
  }

  public async Task<FindCloudletReply> FindCloudlet()
  {

    // Location is ephemeral, so retrieve a new location from the platform. May return 0,0 which is
    // technically valid, though less likely real, as of writing.
    Loc loc = await GetLocationFromDevice();

    // If MEX is reachable on your SIM card:
    string aCarrierName = me.carrierInfo.GetCurrentCarrierName();
    string eCarrierName;
    if (useDemo) // There's no host (PC, UnityEditor, etc.)...
    {
      eCarrierName = carrierName;
    }
    else
    {
      if (aCarrierName == "" || aCarrierName == null)
      {
        Debug.Log("Missing CarrierName for FindCloudlet.");
        return null;
      }
      eCarrierName = aCarrierName;
    }

    FindCloudletRequest req = me.CreateFindCloudletRequest(eCarrierName, devName, appName, appVers, loc);

    FindCloudletReply reply;
    if (useDemo)
    {
      reply = await me.FindCloudlet(dmeHost, dmePort, req);
    }
    else
    {
      reply = await me.FindCloudlet(req);
    }

    return reply;
  }

  public async Task<bool> VerifyLocation()
  {
    Loc loc = await GetLocationFromDevice();

    // If MEX is reachable on your SIM card:
    string aCarrierName = pIntegration.GetCurrentCarrierName();
    string eCarrierName;
    if (useDemo) // There's no host (PC, UnityEditor, etc.)...
    {
      eCarrierName = carrierName;
    }
    else
    {
      eCarrierName = aCarrierName;
    }

    VerifyLocationRequest req = me.CreateVerifyLocationRequest(eCarrierName, loc);

    VerifyLocationReply reply;
    if (useDemo)
    {
      reply = await me.VerifyLocation(dmeHost, dmePort, req);
    }
    else
    {
      reply = await me.VerifyLocation(req);
    }

    // The return is not binary, but one can decide the particular app's policy
    // on pass or failing the location check. Not being verified or the country
    // not matching at all is on such policy decision:

    // GPS and Tower Status:
    switch (reply.gps_location_status) {
      case VerifyLocationReply.GPSLocationStatus.LOC_ROAMING_COUNTRY_MISMATCH:
      case VerifyLocationReply.GPSLocationStatus.LOC_ERROR_UNAUTHORIZED:
      case VerifyLocationReply.GPSLocationStatus.LOC_ERROR_OTHER:
      case VerifyLocationReply.GPSLocationStatus.LOC_UNKNOWN:
        return false;
    }

    switch (reply.tower_status) {
      case VerifyLocationReply.TowerStatus.NOT_CONNECTED_TO_SPECIFIED_TOWER:
      case VerifyLocationReply.TowerStatus.TOWER_UNKNOWN:
        return false;
    }

    // Distance? A negative value means no verification was done.
    if (reply.gps_location_accuracy_km < 0f)
    {
      return false;
    }

    // A per app policy decision might be 0.5 km, or 25km, or 100km:
    if (reply.gps_location_accuracy_km < 100f)
    {
      return true;
    }

    // Too far for this app.
    return false;
  }

}
