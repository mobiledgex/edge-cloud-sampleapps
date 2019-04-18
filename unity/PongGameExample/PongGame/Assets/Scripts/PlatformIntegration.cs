using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

// We need this one for importing our IOS functions
using System.Runtime.InteropServices;

namespace MexPongGame
{
  public interface ICarrierInfo
  {
    string GetCurrentCarrierName();
  }

  public class PlatformIntegration : ICarrierInfo
  {
    [DllImport("__Internal")]
    private static extern string _getCurrentCarrierName();

#if UNITY_ANDROID // PC android target builds to through here as well.
  public string GetCurrentCarrierName()
  {
    string networkOperatorName = "";
    if (Application.platform != RuntimePlatform.Android)
    {
      Debug.Log("Not on android device.");
      return "";
    }

    AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
    AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
    if (activity == null)
    {
      Debug.Log("Can't find an activity!");
      return "";
    }

      AndroidJavaObject context = activity.Call<AndroidJavaObject>("getApplicationContext");

    if (context == null)
    {
      Debug.Log("Can't find an app context!");
      return "";
    }

    // Context.TELEPHONY_SERVICE:
    string CONTEXT_TELEPHONY_SERVICE = context.GetStatic<String>("TELEPHONY_SERVICE");
    AndroidJavaObject telManager = context.Call<AndroidJavaObject>("getSystemService", CONTEXT_TELEPHONY_SERVICE);

    if (telManager == null)
    {
      Debug.Log("Can't get telephony manager!");
      return "";
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
  public string GetCurrentCarrierName()
  {
    Debug.Log("GetCurrentCarrierName is NOT IMPLEMENTED");
    return "";
  }
#endif

  }
}