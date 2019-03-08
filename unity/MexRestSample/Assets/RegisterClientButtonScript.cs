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
          dme.getCarrierName(),
          mexSample.devName,
          mexSample.appName,
          mexSample.appVers,
          mexSample.authToken);

      reply = await dme.RegisterClient(mexSample.host, mexSample.port, registerClientRequest);

      // the dme object stores the session tokens, so the only thing to do here is
      // inspect parts of the JSON registration status and retry:
      if (!reply.Status.Equals(ReplyStatus.RS_SUCCESS.ToString()))
      {
        statusContainer.Post("RegisterClient did not succeed!");
      }
      else
      {
        ok = true;
      }
    }
    catch (Exception e)
    {
      Console.WriteLine(e.StackTrace);
      statusContainer.Post("Exception: " + e.ToString());
      statusContainer.Post(e.StackTrace);
    }
    finally
    {
      if (reply != null) {
        statusContainer.Post(
            "RegisterClient Button results:" +
            " Status: " + reply.Status +
            " SessionCookie: " + reply.SessionCookie +
            " TokenServerURI: " + reply.TokenServerURI
            );
      }
    }
    return ok;
  }
}
