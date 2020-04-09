/**
 * Copyright 2020 MobiledgeX, Inc. All rights and licenses reserved.
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
using System.IO;
using System.Runtime.Serialization.Json;
using System.Threading.Tasks;

using UnityEngine;
using UnityEngine.UI;

using DistributedMatchEngine;

public class FindCloudletButtonScript : MonoBehaviour
{
  public Button findCloudletButton;
  MexSample mexSample;
  StatusContainer statusContainer;

  // Start is called before the first frame update
  void Start()
  {
    Button btn = findCloudletButton.GetComponent<Button>();
    btn.onClick.AddListener(TaskOnClick);

    statusContainer = GameObject.Find("/UICanvas/SampleOutput").GetComponent<StatusContainer>();
    mexSample = GameObject.Find("/UICanvas/MexSampleObject").GetComponent<MexSample>();
  }

  // Update is called once per frame
  void Update()
  {

  }

  async void TaskOnClick()
  {
    Debug.Log("FindCloudlet button!");
    var ok = await DoFindCloudlet();
  }

  async Task<bool> DoFindCloudlet()
  {
    bool ok = false;
    MatchingEngine dme = mexSample.dme;
    FindCloudletReply reply = null;

    try
    {
      var deviceSourcedLocation = await LocationService.RetrieveLocation();
      if (deviceSourcedLocation == null)
      {
        Debug.Log("FindCloudlet must have a device sourced location to send.");
        return false;
      }
      Debug.Log("Device sourced location: Lat: " + deviceSourcedLocation.latitude + "  Long: " + deviceSourcedLocation.longitude);

      var findCloudletRequest = dme.CreateFindCloudletRequest(
          mexSample.carrierName,
          deviceSourcedLocation,
          mexSample.orgName,
          mexSample.appName,
          mexSample.appVers
          );

      if (findCloudletRequest == null)
      {
        Debug.Log("Failed to create request.");
        ok = false;
        return ok;
      }

      reply = await dme.FindCloudlet(mexSample.host, mexSample.port, findCloudletRequest);
      statusContainer.Post("FindCloudlet Reply status: " + reply.status);
    }
    catch (System.Net.WebException we)
    {
      Console.WriteLine(we.StackTrace);
      statusContainer.Post(we.Source + ", WebException: " + we.Message);
      statusContainer.Post(we.StackTrace);
    }
    finally
    {
      if (reply != null)
      {
        DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(FindCloudletReply));
        MemoryStream ms = new MemoryStream();
        serializer.WriteObject(ms, reply);
        string jsonStr = Util.StreamToString(ms);

        statusContainer.Post("GPS Cloudlet location: " + jsonStr + ", fqdn: " + reply.fqdn);

        // The list of registered edge cloudlets that the app can use:
        if (reply.ports.Length == 0)
        {
          statusContainer.Post("There are no app ports for this app's edge cloudlets.");
        }
        else
        {
          statusContainer.Post("Cloudlet app ports:");
          foreach (var appPort in reply.ports)
          {
            statusContainer.Post(
                    "Protocol: " + appPort.proto +
                    ", internal_port: " + appPort.internal_port +
                    ", public_port: " + appPort.public_port +
                    ", path_prefix: " + appPort.path_prefix +
                    ", fqdn_prefix: " + appPort.fqdn_prefix
            );
          }
        }
      }
    }
    return ok;
  }
}
