using UnityEngine;

using System;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using System.Runtime.Serialization.Json;

using DistributedMatchEngine;

public class MexSample : MonoBehaviour
{
  static string carrierName = "TDG";
  static string appName = "EmptyMatchEngineApp";
  static string devName = "EmptyMatchEngineApp";
  static string appVers = "1.0";
  static string developerAuthToken = "";

  static string host = "TDG.dme.mobiledgex.net";
  static UInt32 port = 38001;

  string authToken = ""; // MEX Developer supplied and updated authToken


  string dmeHost = "tdg2.dme.mobiledgex.net"; // DME server hostname or ip.
  int dmePort = 50051; // DME port.

  string developerName = "EmptyMatchEngineApp";
  string applicationName = "EmptyMatchEngineApp";
  string appVer = "1.0";

  DistributedMatchEngine.MatchingEngine me;

  StatusContainer statusContainer;

  // Use this for initialization
  void Start()
  {
    statusContainer = GameObject.Find("/UICanvas/SampleOutput").GetComponent<StatusContainer>();
    RunSampleFlow();
  }

  // Update is called once per frame
  void Update()
  {

  }

  // Get the ephemerial carriername from device specific properties.
  public async Task<string> getCurrentCarrierName()
  {
    var dummy = await Task.FromResult(0);
    return carrierName = "TDG";
  }

  public async void RunSampleFlow()
  {
    try
    {

      carrierName = await getCurrentCarrierName();

      Console.WriteLine("RestSample!");
      statusContainer.Post("RestSample!");

      me = new MatchingEngine();
      port = 38001;  // MatchingEngine.defaultDmeRestPort;
      statusContainer.Post("RestSample Port:" + port);

      // Start location task:
      var locTask = Util.GetLocationFromDevice();
      statusContainer.Post("RestSample Location Task started.");

      var registerClientRequest = me.CreateRegisterClientRequest(carrierName, appName, devName, appVers, developerAuthToken);

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

      statusContainer.Post(" RegisterClient to host: " + host + ", port: " + port);

      var registerClientReply = await me.RegisterClient(host, port, registerClientRequest);
      Console.WriteLine("Reply: Session Cookie: " + registerClientReply.SessionCookie);

      statusContainer.Post("RegisterClient TokenServerURI: " + registerClientReply);

      // Do Verify and FindCloudlet in parallel tasks:
      var loc = await locTask;

      var verifyLocationRequest = me.CreateVerifyLocationRequest(carrierName, loc);
      var findCloudletRequest = me.CreateFindCloudletRequest(carrierName, devName, appName, appVers, loc);
      var getLocationRequest = me.CreateGetLocationRequest(carrierName);


      // Async:
      var findCloudletTask = me.FindCloudlet(host, port, findCloudletRequest);
      var verfiyLocationTask = me.VerifyLocation(host, port, verifyLocationRequest);


      var getLocationTask = me.GetLocation(host, port, getLocationRequest);

      // Awaits:
      var findCloudletReply = await findCloudletTask;
      Console.WriteLine("FindCloudlet Reply: " + findCloudletReply.status);
      statusContainer.Post("FindCloudlet Status: " + findCloudletReply.status);

      var verifyLocationReply = await verfiyLocationTask;
      Console.WriteLine("VerifyLocation Reply: " + verifyLocationReply.gps_location_status);
      statusContainer.Post("VerifyLocation Status: " + verifyLocationReply.gps_location_status);

      var getLocationReply = await getLocationTask;
      var location = getLocationReply.NetworkLocation;
      Console.WriteLine("GetLocationReply: longitude: " + location.longitude + ", latitude: " + location.latitude);
      statusContainer.Post("GetLocationReply: longitude: " + location.longitude + ", latitude: " + location.latitude);
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