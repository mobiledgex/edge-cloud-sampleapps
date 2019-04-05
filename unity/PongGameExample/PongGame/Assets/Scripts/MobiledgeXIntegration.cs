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
  public string appName { get; set; } = "MobiledgeX SDK Demo";
  public string appVers { get; set; } = "1.0";
  public string developerAuthToken { get; set; } = ""; // This is an opaque string value supplied by the developer.

  public string host { get; set; } = "mexdemo.dme.mobiledgex.net"; // Demo host, with some edge cloudlets.
  public uint port { get; set; } = 38001;



  public MobiledgeXIntegration()
  {
    pIntegration = new PlatformIntegration();
    me = new MatchingEngine();
  }



  // These are just thin wrappers over the SDK to how how to use them:
  // Call once, or when the carrier changes:
  public async Task<bool> Register()
  {
    RegisterClientRequest req = me.CreateRegisterClientRequest(carrierName, devName, appName, appVers, "" /* developer specific string blob */);

    // If MEX is reachable on your SIM card:
    string aCarrierName = pIntegration.GetCurrentCarrierName();
    string aHost = "";
    if (aCarrierName != "")
    {
      aHost = me.generateDmeBaseUri(carrierName);
    }

    // Calling with pre-assigned values:
    RegisterClientReply reply = await me.RegisterClient(host, port, req);

    return (reply.Status == ReplyStatus.RS_SUCCESS.ToString());
  }

  public async Task<FindCloudletReply> FindCloudlet()
  {

    // Location is ephemerail, so retrieve a new location from the platform. May return 0,0 which is
    // technically valid, though less likely real, as of writing.
    Loc loc = await LocationService.RetrieveLocation();

    // If in UnityEditor, 0f and 0f are hard zeros as there is no locaiton service.
    if (loc.longitude == 0f && loc.latitude == 0f)
    {
      // Likely not in the ocean. We'll chose something for demo FindCloudlet purposes:
      loc.longitude = -121.8863286;
      loc.latitude = 37.3382082;
    }

    FindCloudletRequest req = me.CreateFindCloudletRequest(carrierName, devName, appName, appVers, loc);

    // If MEX is reachable on your SIM card:
    string aCarrierName = pIntegration.GetCurrentCarrierName();
    string aHost = "";
    if (aCarrierName != "")
    {
      aHost = me.generateDmeBaseUri(carrierName);
    }

    // Calling with pre-assigned values:
    FindCloudletReply reply = await me.FindCloudlet(host, port, req);



    return reply;
  }

}
