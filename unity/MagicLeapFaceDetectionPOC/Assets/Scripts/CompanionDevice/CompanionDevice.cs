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

using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using System.Text;
using System.Runtime.Serialization.Json;

using DistributedMatchEngine;
using MobiledgeX;
using System.Net.NetworkInformation;
using UnityEngine.XR.MagicLeap;

namespace CompanionApp
{
  public class CompanionDeviceDefinitionMissing : Exception
  {
    public CompanionDeviceDefinitionMissing(string message)
        : base(message)
    {
    }
  }

  public class EmptyStreamException : Exception
  {
    public EmptyStreamException(string message)
        : base(message)
    {
    }
  }

  // Library minimal logger without log levels:
  static class Log
  {
    // Stdout:
    public static void S(string TAG, string msg)
    {
      Console.WriteLine("[" + TAG + "] " + msg);
    }
    // Stderr:
    public static void E(string TAG, string msg)
    {
      TextWriter errorWriter = Console.Error;
      errorWriter.WriteLine("[" + TAG + "] " + msg);
    }

    // Debug Stdout:
    [ConditionalAttribute("DEBUG")]
    public static void D(string TAG, string msg)
    {
      Console.WriteLine("[" + TAG + "] " + msg);
    }
  }


  /* API wrapper */
  public class CompanionDevice
  {
    private const string TAG = "CompanionDevice";
    private const string deviceListAPI = "/device/list";
    private const string registerDeviceAPI = "/device/register";
    private const string dmeHostAPI = "/device/dmehostprefix";
    private const string locationAPI = "/location";

    private HttpClient httpClient;
    public const int DEFAULT_REST_TIMEOUT_MS = 10000;
    public const long TICKS_PER_MS = 10000;

    public const int DEFAULT_REST_PORT = 8030;
    public const int DEFAULT_WEBSOCKET_PORT = 8031;

    private int minimumHostLength = "x.x.x.x".Length;
    public string companionAppHost = null;
    public int port = DEFAULT_REST_PORT;
    public int websocketPort = DEFAULT_WEBSOCKET_PORT;

    // TODO: Handle set()/get() changes:
    public DeviceListReply LastDeviceListReply { get; private set; }
    public DeviceRegisterReply LastDeviceRegisterReply { get; private set; }
    public LocationReply LastLocationReply { get; private set; }
    public DmeHostPrefixReply LastDmeHostPrefixReply { get; private set; }

    // Available devices for use:
    private string protocolPrefix = "http://";
    public Device[] devices { get; private set; }

    public DeviceRegisterRequest ConfiguredDeviceRegisterRequest { get; set; }

    public MobiledgeXIntegration Mxi { get; private set; }
    public string CurrentAppInstHost { get; private set; }

    // Hide out here as a "external" platform.
    public class LuminNetworkInterfaceName : NetworkInterfaceName
    {
      public LuminNetworkInterfaceName()
      {
        CELLULAR = new System.Text.RegularExpressions.Regex("^wlan?");
        WIFI = new System.Text.RegularExpressions.Regex("^wlan?");
      }
    };
    public CompanionDevice()
    {
      httpClient = new HttpClient();
      if (httpClient == null)
      {
        throw new Exception("Could not create an HTTP client!");
      }
      httpClient.Timeout = TimeSpan.FromTicks(DEFAULT_REST_TIMEOUT_MS * TICKS_PER_MS);

      NetworkInterface[] interfaces = NetworkInterface.GetAllNetworkInterfaces();
      // The Network Interface name should point to whichever one the default network route points to.
      // This just prints the complete list so a developer can create a new interface profile that is
      // unknown to the MobiledgeX Unity SDK.
      foreach (NetworkInterface adapter in interfaces)
      {
        Logger.D("XXXXXXX Interface Name: {0}", adapter.Name);
      }

      // Override network interface to support FindCloudlet interface checks.
      Mxi = new MobiledgeXIntegration(
        netInterface: new SimpleNetInterface(new LuminNetworkInterfaceName())
        );

      // Override for no companion app:
      LastDmeHostPrefixReply = new DmeHostPrefixReply { dme_host_prefix = "wifi" };
      // Use fallback location:
      Mxi.useFallbackLocation = true;
      Mxi.SetFallbackLocation(latitude: 37.3382082, longitude: -121.8863286); // MLLocation doesn't seem to return value after Start(), with permissions enabled.
    }

    public TimeSpan setCompanionAppTimeout(int timeout_in_ms)
    {
      if (timeout_in_ms <= 0)
      {
        httpClient.Timeout = TimeSpan.FromTicks(DEFAULT_REST_TIMEOUT_MS * TICKS_PER_MS);
      }
      else
      {
        httpClient.Timeout = TimeSpan.FromTicks(timeout_in_ms * TICKS_PER_MS);
      }

      return httpClient.Timeout;
    }

    public TimeSpan getCompanionAppTimeoutTimeSpan()
    {
      return httpClient.Timeout;
    }

    private async Task<Stream> PostRequest(string uri, string jsonStr)
    {
      // Choose network TBD
      Logger.D(TAG, "URI: " + uri);

      HttpContent stringContent = null;
      // static HTTPClient singleton, with instanced HttpContent is recommended for performance.
      if (jsonStr != null) {
        stringContent = new StringContent(jsonStr, Encoding.UTF8, "application/json");
        Logger.D(TAG, "Post Body: " + jsonStr);
      }
      var response = await httpClient.PostAsync(uri, stringContent);

      if (response == null)
      {
        throw new Exception("Null http response object!");
      }

      if (response.StatusCode != HttpStatusCode.OK)
      {
        try
        {
          response.EnsureSuccessStatusCode();
        }
        catch (Exception e)
        {
          throw new HttpException(e.Message, response.StatusCode, -1, e);
        }
      }

      // Normal path:
      Stream replyStream = await response.Content.ReadAsStreamAsync();
      return replyStream;
    }

    /**
     * From a session, select a device from a list, then register a particular device.
     */
    public async Task<DeviceListReply> DeviceList(UserInfo userInfo, string host, int port)
    {
      Logger.D(TAG, "DeviceList()");
      if (port <= 0)
      {
        port = DEFAULT_REST_PORT;
      }
      string uri = protocolPrefix + host + ":" + port + deviceListAPI;

      DeviceListRequest deviceListRequest = new DeviceListRequest
      {
        user_email = userInfo.user_email
      };
      Logger.D(TAG, "Posting to: " + uri);

      DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(DeviceListRequest));
      MemoryStream ms = new MemoryStream();
      serializer.WriteObject(ms, deviceListRequest);
      string jsonStr = Util.StreamToString(ms);

      Stream responseStream = await PostRequest(uri, jsonStr).ConfigureAwait(false);
      if (responseStream == null || !responseStream.CanRead)
      {
        throw new EmptyStreamException("DeviceRegisterReply stream is empty!");
      }

      if (responseStream == null || !responseStream.CanRead)
      {
        throw new EmptyStreamException("DeviceListReply stream is empty!");
      }

      DataContractJsonSerializer deserializer = new DataContractJsonSerializer(typeof(DeviceListReply));
      string responseStr = Util.StreamToString(responseStream);
      byte[] byteArray = Encoding.ASCII.GetBytes(responseStr);
      ms = new MemoryStream(byteArray);
      var reply = (DeviceListReply)deserializer.ReadObject(ms);

      if (reply != null)
      {
        devices = reply.devices;
        LastDeviceListReply = reply;
      }

      return reply;
    }

    /**
     * From Device List, user checks out a device from that list.
     */
    public async Task<DeviceRegisterReply> DeviceRegister(UserInfo info, Device device, string host, int port)
    {
      Logger.D(TAG, "RegisterDevice()");

      if (info == null || device == null)
      {
        Logger.D(TAG, "Cannot register. DeviceRegisterRequest is null");
        var r = new DeviceRegisterReply();
        r.message = "No Registration Info to send to server!";
        r.success = false;
        return r;
        //throw new CompanionDeviceDefinitionMissing("Cannot register. RegisterDeviceRequst is null.");
      }

      // Ensure some values:
      ConfiguredDeviceRegisterRequest = new DeviceRegisterRequest
      {
        user_name = info.user_name == null ? "" : info.user_name,
        user_email = info.user_email == null ? "" : info.user_email,
        user_id = info.user_id == null ? "" : info.user_id,
        device_uuid = device.device_uuid == null ? "" : device.device_uuid,
      };
      /*
      Logger.D("XXX", "RegisterDevice() has info.");

      Logger.D("XXX", "Registering Device dn: " + ConfiguredDeviceRegisterRequest.user_name);
      Logger.D("XXX", "Registering Device ue: " + ConfiguredDeviceRegisterRequest.user_email);
      Logger.D("XXX", "Registering Device uid: " + ConfiguredDeviceRegisterRequest.user_id);
      Logger.D("XXX", "Registering Device uuid: " + ConfiguredDeviceRegisterRequest.device_uuid);
      */
      
      DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(DeviceRegisterRequest));
      Logger.D(TAG, "created serializer.");
      MemoryStream ms = new MemoryStream();
      Logger.D(TAG, "created memstream.");
      serializer.WriteObject(ms, ConfiguredDeviceRegisterRequest);
      Logger.D(TAG, "wrote object.");
      string jsonStr = Util.StreamToString(ms);

      string uri = "http://" + host + ":" + port + registerDeviceAPI;
      Logger.D(TAG, "To this host: " + uri);
      Stream responseStream = await PostRequest(uri, jsonStr).ConfigureAwait(false);

      if (responseStream == null || !responseStream.CanRead)
      {
        throw new EmptyStreamException("DeviceRegisterReply stream is empty!");
      }

      DataContractJsonSerializer deserializer = new DataContractJsonSerializer(typeof(DeviceRegisterReply));
      string responseStr = Util.StreamToString(responseStream);
      byte[] byteArray = Encoding.ASCII.GetBytes(responseStr);
      ms = new MemoryStream(byteArray);
      var reply = (DeviceRegisterReply)deserializer.ReadObject(ms);

      if (reply != null)
      {
        LastDeviceRegisterReply = reply;
      }

      return reply;
    }

    /**
     * Returns the GPS location from the paired CompanionApp phone/device.
     */
    public async Task<LocationReply> GetLocation()
    {
      Logger.D(TAG, "GetLocation()");

      if (!MLLocation.IsStarted)
      {
        Logger.D(TAG, "Please start MagicLeap's MLLocation object from a GameObject/MonoBehavior before using this call.");
        // Hm, Debug: return something.
        Logger.D(TAG, "FIXME: Returning a hard coded value...");
        return new LocationReply
        {
          longitude = -121.955238,
          latitude = 37.354107
        };
      }

      MLLocation.Location locData = new MLLocation.Location();
      MLResult result = MLLocation.GetLastFineLocation(out locData);
      Logger.D(TAG, "MLLocation result: " + result); // Ensure location is allowed.
      if (!result.Equals(MLResult.Create(MLResult.Code.Ok)))
      {
        // Try coarse instead:
        result = MLLocation.GetLastCoarseLocation(out locData);
        if (!result.Equals(MLResult.Create(MLResult.Code.Ok)))
        {
          Logger.D(TAG, "Empty location returning");
          return new LocationReply { longitude = 0f, latitude = 0f };
        }
      }

      Logger.D(TAG, "New location returning: {" + locData.Longitude + ", " + locData.Latitude + "}");
      return new LocationReply
      {
        longitude = locData.Longitude,
        latitude = locData.Latitude
      };
    }

    // From CompanionApp.
#if false
      if (companionAppHost == null || companionAppHost.Length < minimumHostLength)
      {
        Logger.D(TAG, "Host not known yet for CompanionApp server!");
        return null;
      }

      string uri = "http://" + companionAppHost + ":" + port + locationAPI;
      Logger.D(TAG, "GetLocation(): uri: " + uri);
      Stream responseStream = await PostRequest(uri, "").ConfigureAwait(false);

      if (responseStream == null || !responseStream.CanRead)
      {
        throw new EmptyStreamException("LocationReply stream is empty!");
      }

      DataContractJsonSerializer deserializer = new DataContractJsonSerializer(typeof(LocationReply));
      string responseStr = Util.StreamToString(responseStream);
      Logger.D(TAG, "GetLocation(): reply: " + responseStr);
      byte[] byteArray = Encoding.ASCII.GetBytes(responseStr);
      MemoryStream ms = new MemoryStream(byteArray);
      var reply = (LocationReply)deserializer.ReadObject(ms);

      if (reply != null)
      {
        LastLocationReply = reply;
      }

      Mxi.SetFallbackLocation(reply.longitude, reply.latitude);

      return reply;
    }
#endif

    /**
     * Returns the DME Host this device should use to contact over the CompanionApp WiFi hostspot.
     */
    public async Task<DmeHostPrefixReply> GetDmeHost()
    {
      Logger.D(TAG, "GetDmeHost()");

      return LastDmeHostPrefixReply; // This was overridden in constructor to a regional DME.

      if (companionAppHost == null || companionAppHost.Length < minimumHostLength)
      {
        Logger.D(TAG, "Host not known yet for CompanionApp server!");
        return null;
      }

      string uri = "http://" + companionAppHost + ":" + port + dmeHostAPI; // FIXME: TLS port + cert + add to keychain.
      Logger.D(TAG, "GetDmeHost(): uri: " + uri);
      Stream responseStream = await PostRequest(uri, null).ConfigureAwait(false);

      if (responseStream == null || !responseStream.CanRead)
      {
        throw new EmptyStreamException("DmeHostReply stream is empty!");
      }

      DataContractJsonSerializer deserializer = new DataContractJsonSerializer(typeof(DmeHostPrefixReply));
      string responseStr = Util.StreamToString(responseStream);
      Logger.D(TAG, "GetDmeHost(): reply: " + responseStr);
      byte[] byteArray = Encoding.ASCII.GetBytes(responseStr);
      MemoryStream ms = new MemoryStream(byteArray);
      var reply = (DmeHostPrefixReply)deserializer.ReadObject(ms);

      if (reply != null)
      {
        LastDmeHostPrefixReply = reply;

        // TODO: public function to set new CompanionApp info instead of default:
        Mxi.carrierName = LastDmeHostPrefixReply.dme_host_prefix;
        DmeHost = Mxi.carrierName + "." + MatchingEngine.baseDmeHost;
      }
      return reply;
    }

    public string DmeHost = "";
    public string AppInstHost = "";
    public async Task<string> RegisterAndFindCloudlet()
    {
      Logger.D(TAG, "RegisterAndFindCloudlet()");
      Logger.D(TAG, "Current ServerHost set to: " + CurrentAppInstHost);
      // FIXME: Calling the PlatformIntegration template layer here directly.
      // FIXME: Override the override if needed due to AppInst cloudlet location.
      // LastDmeHostPrefixReply.dme_host_prefix = MatchingEngine.wifiCarrier;
      Mxi.carrierName = LastDmeHostPrefixReply.dme_host_prefix;
      DmeHost = LastDmeHostPrefixReply.dme_host_prefix + "." + MatchingEngine.baseDmeHost; // Change the target host, use default port.
      Logger.D(TAG, "DmeHost is: " + DmeHost);

      bool found;
      try
      {
          found = await Mxi.RegisterAndFindCloudlet(DmeHost, MatchingEngine.defaultDmeRestPort).ConfigureAwait(false);
          if (!found)
          {
              Logger.D(TAG, "Can't findCloudlet!");
              return null;
          }
          Logger.D(TAG, "Found host: " + found + ", fqdn: " + Mxi.FindCloudletReply.fqdn);
          AppPort ap = Mxi.GetAppPort(LProto.L_PROTO_TCP); // First one.
          AppInstHost = Mxi.GetUrl("https", ap); // The default is TLS enabled.
          if (CurrentAppInstHost != null && CurrentAppInstHost.Equals(AppInstHost)) // Mxi stores the actual copy, as the intergation layer.
          {
              Logger.D(TAG, "No new cloudlet Host.");
          }
          else
          {
              CurrentAppInstHost = AppInstHost;
              // TODO: FindCloudlet + AppPort is more correct and complete here if the server ends up on an L7 Path.
              Logger.D(TAG, "New edge AppInst host located: " + AppInstHost);
          }
      }
      catch (Exception e)
      {
        Logger.D(TAG, "Exception during RegisterAndFindCloudlet: " + e.Message);
      }
      return AppInstHost;
    }
  };

}
