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
  public string carrierName { get; set; } = "TDG"; // carrierName depends on the the available subscriber SIM card and roaming carriers, and must be supplied a platform API.
  public string devName { get; set; } = "MobiledgeX"; // Your developer name.
  public string appName { get; set; } = "MobiledgeX SDK Demo"; // Your appName, if you have created this in the MobiledgeX console.
  public string appVers { get; set; } = "1.0";
  public string developerAuthToken { get; set; } = ""; // This is an opaque string value supplied by the developer.

  public string dmeHost { get; set; } = MatchingEngine.fallbackDmeHost;
  public uint port { get; set; } = MatchingEngine.defaultDmeRestPort;

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

  // These are just thin wrappers over the SDK to how how to use them:
  // Call once, or when the carrier changes:
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
        Debug.Log("Missing CarrierName for FindCloudlet.");
        return false;
      }
      eCarrierName = aCarrierName;
    }

    RegisterClientRequest req = me.CreateRegisterClientRequest(eCarrierName, devName, appName, appVers, "" /* developer specific string blob */);
    Debug.Log("CarrierName: " + req.carrier_name);
    Debug.Log("DevName: " + req.dev_name);
    Debug.Log("AppName: " + req.app_name);
    Debug.Log("AppVers: " + req.app_vers);

    // Calling with pre-assigned values for demo DME server, since eHost may not exist for the SIM card.
    RegisterClientReply reply = await me.RegisterClient(req);

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

    // Calling with pre-assigned values:
    FindCloudletReply reply = await me.FindCloudlet(req);


    return reply;
  }

  public async Task<bool> VerifyLocation()
  {
    Loc loc = await GetLocationFromDevice();

    // If MEX is reachable on your SIM card:
    string aCarrierName = pIntegration.GetCurrentCarrierName();
    string eHost; // Ephemeral DME host (depends on the SIM).
    string eCarrierName;
    if (useDemo) // There's no host (PC, UnityEditor, etc.)...
    {
      eCarrierName = carrierName;
    }
    else
    {
      eCarrierName = aCarrierName;
    }

    // Ask the demo server host (not eHost) to verify it:
    VerifyLocationRequest req = me.CreateVerifyLocationRequest(eCarrierName, loc);

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

  static Timestamp createTimestamp(int futureSeconds)
  {
    long ticks = DateTime.Now.Ticks;
    long sec = ticks / TimeSpan.TicksPerSecond; // Truncates.
    long remainderTicks = ticks - (sec * TimeSpan.TicksPerSecond);
    int nanos = (int)(remainderTicks / TimeSpan.TicksPerMillisecond) * 1000000;

    var timestamp = new Timestamp
    {
      seconds = (sec + futureSeconds).ToString(),
      nanos = nanos
    };

    return timestamp;
  }


  static List<QosPosition> CreateQosPositionList(Loc firstLocation, double direction_degrees, double totalDistanceKm, double increment)
  {
    var req = new List<QosPosition>();
    double kmPerDegreeLong = 111.32; // at Equator
    double kmPerDegreeLat = 110.57; // at Equator
    double addLongitude = (Math.Cos(direction_degrees / (Math.PI / 180)) * increment) / kmPerDegreeLong;
    double addLatitude = (Math.Sin(direction_degrees / (Math.PI / 180)) * increment) / kmPerDegreeLat;
    double i = 0d;
    double longitude = firstLocation.longitude;
    double latitude = firstLocation.latitude;

    long id = 1;

    while (i < totalDistanceKm)
    {
      longitude += addLongitude;
      latitude += addLatitude;
      i += increment;

      // FIXME: No time is attached to GPS location, as that breaks the server!
      var qloc = new QosPosition
      {
        positionid = id.ToString(),
        gps_location = new Loc
        {
          longitude = longitude,
          latitude = latitude,
          timestamp = createTimestamp(100)
        }
      };


      req.Add(qloc);
      id++;
    }

    return req;
  }
  public async Task<QosPositionKpiStream> GetQosPositionKpi()
  {
    Loc loc = await GetLocationFromDevice();

    // Create a list of quality of service position requests:
    var firstLoc = new Loc
    {
      longitude = 8.5821,
      latitude = 50.11
    };
    var requestList = CreateQosPositionList(firstLoc, 45, 2, 1);

    var qosPositionRequest = me.CreateQosPositionRequest(requestList, 0, null);
    var qosReplyStream = await me.GetQosPositionKpi(qosPositionRequest);
    if (qosReplyStream == null)
    {
      Console.WriteLine("Reply result missing: " + qosReplyStream);
    }
    return qosReplyStream;
  }
}
