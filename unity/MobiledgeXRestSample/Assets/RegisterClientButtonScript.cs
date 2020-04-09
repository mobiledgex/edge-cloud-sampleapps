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
using System.Threading.Tasks;

using UnityEngine;
using UnityEngine.UI;

using DistributedMatchEngine;

public class RegisterClientButtonScript : MonoBehaviour
{
  public Button registerClientButton;
  MexSample mexSample;
  StatusContainer statusContainer;

  // Start is called before the first frame update
  void Start()
  {
    Button btn = registerClientButton.GetComponent<Button>();
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
    Debug.Log("RegisterClient button!");
    var ok = await DoRegisterClient();
  }

  async Task<bool> DoRegisterClient()
  {
    bool ok = false;
    MatchingEngine dme = mexSample.dme;
    RegisterClientReply reply = null;

    try
    {
      var registerClientRequest = dme.CreateRegisterClientRequest(
          mexSample.carrierName,
          mexSample.devName,
          mexSample.appName,
          mexSample.appVers,
          mexSample.authToken);

      reply = await dme.RegisterClient(mexSample.host, mexSample.port, registerClientRequest);

      // the dme object stores the session tokens, so the only thing to do here is
      // inspect parts of the JSON registration status and retry:
      if (reply.status != ReplyStatus.RS_SUCCESS)
      {
        statusContainer.Post("RegisterClient did not succeed!");
      }
      else
      {
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
      if (reply != null) {
        statusContainer.Post(
            "RegisterClient Button results:" +
            " Status: " + reply.status +
            " SessionCookie: " + reply.session_cookie +
            " TokenServerURI: " + reply.token_server_uri
            );
      }
    }
    return ok;
  }
}
