using System.Collections;
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
  public string carrierName { get; set; } = "TDG"; // carrierName depends on the the subscriber SIM card and roaming carriers, and must be supplied a platform API.
  public string devName { get; set; } = "MobiledgeX";
  public string appName { get; set; } = "PongGameHackathonApp";
  public string appVers { get; set; } = "1.0";
  public string developerAuthToken { get; set; } = ""; // This is an opaque string value supplied by the developer.

  public string host { get; set; } = "mexdemo.dme.mobiledgex.net"; // Demo DME host, with some edge cloudlets.
  public uint port { get; set; } = 38001;
  public bool useDemo { get; set; } = true;

  public MobiledgeXIntegration()
  {
    pIntegration = new PlatformIntegration();
    me = new MatchingEngine();
  }

  public string GetCarrierName()
  {
    return pIntegration.GetCurrentCarrierName();
  }

  public async Task<Loc> GetLocationFromDevice()
  {
    // Location is ephemeral, so retrieve a new location from the platform. May return 0,0 which is
    // technically valid, though less likely real, as of writing.
    Loc loc = await LocationService.RetrieveLocation();

    // If in UnityEditor, 0f and 0f are hard zeros as there is no locaiton service.
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
    string aCarrierName = pIntegration.GetCurrentCarrierName();
    string eHost; // Ephemeral DME host (depends on the SIM).
    string eCarrierName;
    if (aCarrierName == "" || useDemo) // There's no host (PC, UnityEditor, etc.)...
    {
      eHost = host;
      eCarrierName = carrierName;
    }
    else
    {
      eHost = me.GenerateDmeBaseUri(aCarrierName);
      eCarrierName = aCarrierName;
    }
    Debug.Log("DME Host Generated is: " + eHost);

    RegisterClientRequest req = me.CreateRegisterClientRequest(eCarrierName, devName, appName, appVers, "" /* developer specific string blob */);
    Debug.Log("CarrierName: " + req.CarrierName);
    Debug.Log("DevName: " + req.DevName);
    Debug.Log("AppName: " + req.AppName);
    Debug.Log("AppVers: " + req.AppVers);

    // Calling with pre-assigned values for demo DME server, since eHost may not exist for the SIM card.
    RegisterClientReply reply = await me.RegisterClient(eHost, port, req);

    return (reply.Status == ReplyStatus.RS_SUCCESS);
  }

  public async Task<FindCloudletReply> FindCloudlet()
  {

    // Location is ephemeral, so retrieve a new location from the platform. May return 0,0 which is
    // technically valid, though less likely real, as of writing.
    Loc loc = await GetLocationFromDevice();

    // If MEX is reachable on your SIM card:
    string aCarrierName = pIntegration.GetCurrentCarrierName();
    string eHost; // Ephemeral DME host (depends on the SIM).
    string eCarrierName;
    if (aCarrierName == "" || useDemo) // There's no host (PC, UnityEditor, etc.)...
    {
      eHost = host;
      eCarrierName = carrierName;
    }
    else
    {
      eHost = me.GenerateDmeBaseUri(aCarrierName);
      eCarrierName = aCarrierName;
    }
    Debug.Log("DME Host Generated is: " + eHost);

    FindCloudletRequest req = me.CreateFindCloudletRequest(eCarrierName, devName, appName, appVers, loc);

    // Calling with pre-assigned values:
    FindCloudletReply reply = await me.FindCloudlet(eHost, port, req);


    return reply;
  }

  public async Task<bool> VerifyLocation()
  {
    Loc loc = await GetLocationFromDevice();

    // If MEX is reachable on your SIM card:
    string aCarrierName = pIntegration.GetCurrentCarrierName();
    string eHost; // Ephemeral DME host (depends on the SIM).
    string eCarrierName;
    if (aCarrierName == "" || useDemo) // There's no host (PC, UnityEditor, etc.)...
    {
      eHost = host;
      eCarrierName = carrierName;
    }
    else
    {
      eHost = me.GenerateDmeBaseUri(aCarrierName);
      eCarrierName = aCarrierName;
    }

    // Ask the demo server host (not eHost) to verify it:
    VerifyLocationRequest req = me.CreateVerifyLocationRequest(eCarrierName, loc);

    VerifyLocationReply reply = await me.VerifyLocation(eHost, port, req);

    // The return is not binary, but one can decide the particular app's policy
    // on pass or failing the location check. Not being verified or the country
    // not matching at all is on such policy decision:

    // GPS and Tower Status:
    switch (reply.gps_location_status) {
      case VerifyLocationReply.GPS_Location_Status.LOC_ROAMING_COUNTRY_MISMATCH:
      case VerifyLocationReply.GPS_Location_Status.LOC_ERROR_UNAUTHORIZED:
      case VerifyLocationReply.GPS_Location_Status.LOC_ERROR_OTHER:
      case VerifyLocationReply.GPS_Location_Status.LOC_UNKNOWN:
        return false;
    }

    switch (reply.tower_status) {
      case VerifyLocationReply.Tower_Status.NOT_CONNECTED_TO_SPECIFIED_TOWER:
      case VerifyLocationReply.Tower_Status.TOWER_UNKNOWN:
        return false;
    }

    // Distance? A negative value means no verification was done.
    if (reply.GPS_Location_Accuracy_KM < 0f)
    {
      return false;
    }

    // A per app policy decision might be 0.5 km, or 25km, or 100km:
    if (reply.GPS_Location_Accuracy_KM < 100f)
    {
      return true;
    }

    // Too far for this app.
    return false;
  }

}
