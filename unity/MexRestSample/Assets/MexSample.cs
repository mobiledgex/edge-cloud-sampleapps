using UnityEngine;

using System;
using System.IO;

using System.Threading.Tasks;
using System.Runtime.Serialization.Json;

using DistributedMatchEngine;

public class MexSample : MonoBehaviour
{
  public string carrierName { get; set; } = "mexdemo"; // carrierName depends on the the subscriber SIM card and roaming carriers, and must be supplied a platform API.
  public string appName { get; set; } = "EmptyMatchEngineApp";
  public string devName { get; set; } = "EmptyMatchEngineApp";
  public string appVers { get; set; } = "1.0";
  public string developerAuthToken { get; set; } = ""; // This is an opaque string value supplied by the developer.

  public string host { get; set; } = "mexdemo.dme.mobiledgex.net"; // Demo host, with some edge cloudlets.
  public UInt32 port { get; set; } = 38001;

  public string authToken; // Supplied by developer

  // For demoonstartion purposes in the sample, we need a copy of DME somewhere
  // for individual button scripts to access.
  public DistributedMatchEngine.MatchingEngine dme { get; set; } = new MatchingEngine();

  StatusContainer statusContainer;
  LocationService locationService;

  // Use this for initialization
  void Start()
  {
    statusContainer = GameObject.Find("/UICanvas/SampleOutput").GetComponent<StatusContainer>();
  }

  // Update is called once per frame
  void Update()
  {

  }

  // Get the ephemerial carriername from device specific properties.
  public async Task<string> getCurrentCarrierName()
  {
    var dummy = await Task.FromResult(0);
    return carrierName;
  }

  public async void RunSampleFlow()
  {
    try
    {
      carrierName = await getCurrentCarrierName();

      Console.WriteLine("RestSample!");
      statusContainer.Post("RestSample!");

      dme = new MatchingEngine();
      port = 38001;  // MatchingEngine.defaultDmeRestPort;
      statusContainer.Post("RestSample Port:" + port);

      // Start location and await:
      var location = await LocationService.RetrieveLocation();
      statusContainer.Post("RestSample Location Task started.");

      var registerClientRequest = dme.CreateRegisterClientRequest(carrierName, appName, devName, appVers, developerAuthToken);

      // Await synchronously.

      statusContainer.Post("RegisterClient.");
      DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(RegisterClientRequest));
      MemoryStream ms = new MemoryStream();
      // have object?
      if (registerClientRequest == null)
      {
        statusContainer.Post("Weird, RegisterClient create is null");
      }
      statusContainer.Post("RegisterClient: AppName" + registerClientRequest.AppName);

      serializer.WriteObject(ms, registerClientRequest);

      string jsonStr = Util.StreamToString(ms);
      statusContainer.Post(" --> RegisterClient as string: " + jsonStr);
      Debug.Log(" --> RegisterClient as string: " + jsonStr);

      statusContainer.Post(" RegisterClient to host: " + host + ", port: " + port);

      var registerClientReply = await dme.RegisterClient(host, port, registerClientRequest);
      Console.WriteLine("Reply: Session Cookie: " + registerClientReply.SessionCookie);

      statusContainer.Post("RegisterClient TokenServerURI: " + registerClientReply.TokenServerURI);

      // Do Verify and FindCloudlet in parallel tasks:

      var verifyLocationRequest = dme.CreateVerifyLocationRequest(carrierName, location);
      var findCloudletRequest = dme.CreateFindCloudletRequest(carrierName, devName, appName, appVers, location);
      var getLocationRequest = dme.CreateGetLocationRequest(carrierName);


      // Async:
      var findCloudletTask = dme.FindCloudlet(host, port, findCloudletRequest);
      //var verfiyLocationTask = me.VerifyLocation(host, port, verifyLocationRequest);


      var getLocationTask = dme.GetLocation(host, port, getLocationRequest);

      // Awaits:
      var findCloudletReply = await findCloudletTask;
      Console.WriteLine("FindCloudlet Reply: " + findCloudletReply.status);
      statusContainer.Post("FindCloudlet Status: " + findCloudletReply.status);

      // The following requires a valid SIM card from a MobiledgeX enabled carrier
      // to validate the device location. It will attempt to contact the carrier,
      // and will timeout if not within the carrier network.
      //var verifyLocationReply = await verfiyLocationTask;
      //Console.WriteLine("VerifyLocation Reply: " + verifyLocationReply.gps_location_status);
      //statusContainer.Post("VerifyLocation Status: " + verifyLocationReply.gps_location_status);

      var getLocationReply = await getLocationTask;
      var carrierLocation = getLocationReply.NetworkLocation;
      Console.WriteLine("GetLocationReply: longitude: " + carrierLocation.longitude + ", latitude: " + carrierLocation.latitude);
      statusContainer.Post("GetLocationReply: longitude: " + carrierLocation.longitude + ", latitude: " + carrierLocation.latitude);
    }
    catch (InvalidTokenServerTokenException itste)
    {
      Console.WriteLine(itste.StackTrace);
      statusContainer.Post("Token Exception: " + itste.ToString());
      statusContainer.Post(itste.StackTrace);
    }
    catch (Exception e)
    {
      Console.WriteLine(e.StackTrace);
      statusContainer.Post("Exception: " + e.ToString());
      statusContainer.Post(e.StackTrace);
    }
  }

}