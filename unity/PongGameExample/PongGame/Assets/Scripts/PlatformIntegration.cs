using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

// We need this one for importing our IOS functions
using System.Runtime.InteropServices;

namespace MexPongGame
{
  public interface CarrierInfo
  {
    string GetCurrentCarrierName();
  }

  public class PlatformIntegration : MonoBehaviour, CarrierInfo
  {
    [DllImport("__Internal")]
    private static extern string _getCurrentCarrierName();

    // Start is called before the first frame update
    void Start()
    {
      string networkOperatorName = GetCurrentCarrierName();
      Debug.Log("networkOperatorName gotten: [" + networkOperatorName + "]");
    }

    // Update is called once per frame
    void Update()
    {

    }

#if UNITY_ANDROID
  public string GetCurrentCarrierName()
  {
    string networkOperatorName = "";
    AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
    AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
    AndroidJavaObject context = activity.Call<AndroidJavaObject>("getApplicationContext");

    if (context == null)
    {
      throw new Exception("Can't find an app context!");
    }

    // Context.TELEPHONY_SERVICE:
    string CONTEXT_TELEPHONY_SERVICE = context.GetStatic<String>("TELEPHONY_SERVICE");
    AndroidJavaObject telManager = context.Call<AndroidJavaObject>("getSystemService", CONTEXT_TELEPHONY_SERVICE);

    if (telManager == null)
    {
      Debug.Log("Can't get telephony manager!");
      return networkOperatorName = "";
    }

    int simState = telManager.Call<int>("getSimState", new object[0]);
    Debug.Log("SimState (type Int): [" + simState + "]");

    networkOperatorName = telManager.Call<String>("getNetworkOperatorName", new object[0]);

    if (networkOperatorName == null)
    {
      Debug.Log("Network Operator Name is not found on the device");
      networkOperatorName = "";
    }

    return networkOperatorName;
  }
#elif UNITY_IOS
    public string GetCurrentCarrierName()
    {
      string networkOperatorName = "";
      if (Application.platform == RuntimePlatform.IPhonePlayer)
      {
        networkOperatorName = _getCurrentCarrierName();
      }
      return networkOperatorName;
    }
#else
  public String GetCurrentCarrierName()
  {
    Debug.Log("GetCurrentCarrierName is NOT IMPLEMENTED");
    return "";
  }
#endif

  }
}