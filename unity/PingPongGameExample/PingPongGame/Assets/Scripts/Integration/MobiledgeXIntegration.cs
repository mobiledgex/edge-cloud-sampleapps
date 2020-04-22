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
using System.Net.WebSockets;

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
  public string carrierName { get; set; } = MatchingEngine.wifiCarrier; // carrierName depends on the available subscriber SIM card and roaming carriers, and must be supplied by platform API.
  public string orgName { get; set; } = "MobiledgeX"; // Your organization name.
  public string appName { get; set; } = "PingPong"; // Your appName, if you have created this in the MobiledgeX console.
  public string appVers { get; set; } = "2020-02-03"; // Your app version uploaded to the docker registry.
  public string authToken { get; set; } = ""; // This is an opaque string value supplied by the developer.
  public uint cellID { get; set; } = 0;
  public string uniqueIDType { get; set; } = "";
  public string uniqueID { get; set; } = "";
  public Tag[] tags { get; set; } = new Tag[0];

  public MobiledgeXIntegration()
  {
    // Set the platform specific way to get SIM carrier information.
    pIntegration = new PlatformIntegration();

    // Platform integration needs to initialize first:
    me = new MatchingEngine(pIntegration.CarrierInfo, pIntegration.NetInterface, pIntegration.UniqueID);

    // Optional NetTesting.
    netTest = new NetTest(me);
  }

  public void useWifiOnly(bool useWifi)
  {
    me.useOnlyWifi = useWifi;
  }

  public string GetCarrierName()
  {
    return me.carrierInfo.GetMccMnc();
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
    if (me.useOnlyWifi)
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

    RegisterClientRequest req = me.CreateRegisterClientRequest(eCarrierName, orgName, appName, appVers, authToken, cellID, uniqueIDType, uniqueID, tags);
    Debug.Log("CarrierName: " + req.carrier_name);
    Debug.Log("orgName: " + req.org_name);
    Debug.Log("AppName: " + req.app_name);
    Debug.Log("AppVers: " + req.app_vers);

    RegisterClientReply reply = await me.RegisterClient(req);

    return (reply.status == ReplyStatus.RS_SUCCESS);
  }

  public async Task<FindCloudletReply> FindCloudlet()
  {

    // Location is ephemeral, so retrieve a new location from the platform. May return 0,0 which is
    // technically valid, though less likely real, as of writing.
    Loc loc = await GetLocationFromDevice();

    // If MEX is reachable on your SIM card:
    string aCarrierName = GetCarrierName();
    string eCarrierName;
    if (me.useOnlyWifi) // There's no host (PC, UnityEditor, etc.)...
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

    FindCloudletRequest req = me.CreateFindCloudletRequest(eCarrierName, loc);

    FindCloudletReply reply = await me.FindCloudlet(req);

    return reply;
  }

  public async Task<bool> VerifyLocation()
  {
    Loc loc = await GetLocationFromDevice();

    // If MEX is reachable on your SIM card:
    string aCarrierName = GetCarrierName();
    string eCarrierName;
    if (me.useOnlyWifi) // There's no host (PC, UnityEditor, etc.)...
    {
      eCarrierName = carrierName;
    }
    else
    {
      eCarrierName = aCarrierName;
    }

    VerifyLocationRequest req = me.CreateVerifyLocationRequest(eCarrierName, loc, cellID, tags);

    VerifyLocationReply reply = await me.VerifyLocation(req);

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

  // Typical developer workflow to get connection to application backend
  public async Task<ClientWebSocket> GetWebsocketConnection(string path)
  {
    if (!IsEdgeEnabled(GetConnectionProtocols.Websocket))
    {
      throw new Exception("Device is not edge enabled. Please switch to cellular connection or use server in public cloud");
    }

    Loc loc = await GetLocationFromDevice();

    carrierName = GetCarrierName();
    if (carrierName == null)
    {
      throw new Exception("Unable to find carrier information for edge connection");
    }

    FindCloudletReply findCloudletReply = await me.RegisterAndFindCloudlet(carrierName, orgName, appName, appVers, authToken, loc, cellID, uniqueIDType, uniqueID, tags);
    if (findCloudletReply == null)
    {
      throw new Exception("Unable to find cloudlet to connect to");
    }

    Dictionary<int, AppPort> appPortsDict = me.GetTCPAppPorts(findCloudletReply);
    int public_port = findCloudletReply.ports[0].public_port; // We happen to know it's the first one.
    AppPort appPort = appPortsDict[public_port];
    return await me.GetWebsocketConnection(findCloudletReply, appPort, public_port, 5000, path);
  }

  // Edge requires connections to run over cellular interface
  public  bool IsEdgeEnabled(GetConnectionProtocols proto)
  {
    me.useOnlyWifi = false;
    if (me.useOnlyWifi)
    {
      Debug.Log("useOnlyWifi must be false to enable edge connection");
      return false;
    }

    if (proto == GetConnectionProtocols.TCP || proto == GetConnectionProtocols.UDP)
    {
      if (!me.netInterface.HasCellular())
      {
        Debug.Log(proto + " connection requires a cellular interface to run connection over edge.");
        return false;
      }
    }
    else
    {
      // Connections where we cannot bind to cellular interface default to wifi if wifi is up
      // We need to make sure wifi is off
      if (!me.netInterface.HasCellular() || me.netInterface.HasWifi())
      {
        Debug.Log(proto + " connection requires the cellular interface to be up and the wifi interface to be off to run connection over edge.");
        return false;
      }
    }
    
    string cellularIPAddress = me.netInterface.GetIPAddress(me.netInterface.GetNetworkInterfaceName().CELLULAR);
    if (cellularIPAddress == null)
    {
      Debug.Log("Unable to find ip address for local cellular interface.");
      return false;
    }

    return true;
  }

  public enum GetConnectionProtocols
  {
    TCP,
    UDP,
    HTTP,
    Websocket
  }
}
