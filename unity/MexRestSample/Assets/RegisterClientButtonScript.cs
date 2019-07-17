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
