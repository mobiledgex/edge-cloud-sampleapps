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

﻿using System;
using System.IO;
using System.Runtime.Serialization.Json;
using System.Threading.Tasks;

using UnityEngine;
using UnityEngine.UI;

using DistributedMatchEngine;

public class VerifyLocationButtonScript : MonoBehaviour
{
  public Button verifyLocationButton;
  MexSample mexSample;
  StatusContainer statusContainer;

  // Start is called before the first frame update
  void Start()
  {
    Button btn = verifyLocationButton.GetComponent<Button>();
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
    Debug.Log("VerifyLocation button!");
    var ok = await DoFindCloudlet();
  }

  async Task<bool> DoFindCloudlet()
  {
    bool ok = false;
    MatchingEngine dme = mexSample.dme;
    VerifyLocationReply reply = null;

    try
    {
      var deviceSourcedLocation = await LocationService.RetrieveLocation();
      if (deviceSourcedLocation == null)
      {
        Debug.Log("VerifyLocation must have a device sourced location to send to DME.");
        return false;
      }
      Debug.Log("Device sourced location: Lat: " + deviceSourcedLocation.latitude + "  Long: " + deviceSourcedLocation.longitude);

      var verifyLocationRequest = dme.CreateVerifyLocationRequest(
          mexSample.carrierName, // TODO: carrierName is the current carrier string, and must be provided by the app.
          deviceSourcedLocation);

      reply = await dme.VerifyLocation(mexSample.host, mexSample.port, verifyLocationRequest);
      statusContainer.Post("VerifyLocation Reply mobile tower status: " + reply.tower_status.ToString());
      if (reply.tower_status.Equals(FindCloudletReply.FindStatus.FIND_FOUND.ToString())) {
        ok = true;
      }
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
        DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(VerifyLocationReply));
        MemoryStream ms = new MemoryStream();
        serializer.WriteObject(ms, reply);
        string jsonStr = Util.StreamToString(ms);

        statusContainer.Post("VeryfyLocation Reply: " + jsonStr);
      }
    }
    return ok;
  }
}
