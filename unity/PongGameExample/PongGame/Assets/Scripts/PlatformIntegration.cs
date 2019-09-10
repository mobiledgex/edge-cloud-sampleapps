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
    string GetMccMnc();
    string GenerateDmeHostName();
  }

  public class PlatformIntegration : ICarrierInfo
  {
    [DllImport("__Internal")]
    private static extern string _getCurrentCarrierName();

    [DllImport("__Internal")]
    private static extern string _getMccMnc();


    // Sets platform specific internal callbacks, etc.
    [DllImport("__Internal")]
    private static extern string _initMatchingEnginePlatformIntegration();

    public PlatformIntegration()
    {
#if UNITY_IOS
      _initMatchingEnginePlatformIntegration();
#endif
    }

    // All platforms utility function.
    public string GenerateDmeHostName()
    {
      string baseDmeHost = DistributedMatchEngine.MatchingEngine.baseDmeHost;
      string mccmnc = GetMccMnc();
      if (mccmnc == null || mccmnc.Length < 5)
      {
        return null;
      }
      string mcc = mccmnc.Substring(0,3);
      if (mcc == null)
      {
        return null;
      }
      string mnc = mccmnc.Substring(3);
      if (mnc == null)
      {
        return null;
      }
      return mcc + "-" + mnc + "." + baseDmeHost;
    }

#if UNITY_ANDROID // PC android target builds to through here as well.
    AndroidJavaObject GetTelephonyManager() {
      AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
      AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
      if (activity == null)
      {
       Debug.Log("Can't find an activity!");
       return null;
      }

     AndroidJavaObject context = activity.Call<AndroidJavaObject>("getApplicationContext");

      if (context == null)
      {
        Debug.Log("Can't find an app context!");
        return null;
      }

      // Context.TELEPHONY_SERVICE:
      string CONTEXT_TELEPHONY_SERVICE = context.GetStatic<String>("TELEPHONY_SERVICE");
      AndroidJavaObject telManager = context.Call<AndroidJavaObject>("getSystemService", CONTEXT_TELEPHONY_SERVICE);

      return telManager;
    }

    public string GetCurrentCarrierName()
    {
      string networkOperatorName = "";
      Debug.Log("Device platform: " + Application.platform);
      if (Application.platform != RuntimePlatform.Android)
      {
        Debug.Log("Not on android device.");
        return "";
      }
      AndroidJavaObject telManager = GetTelephonyManager();

      if (telManager == null)
      {
        Debug.Log("Can't get telephony manager!");
        return "";
      }

      networkOperatorName = telManager.Call<String>("getNetworkOperatorName", new object[0]);

      if (networkOperatorName == null)
      {
        Debug.Log("Network Operator Name is not found on the device");
        networkOperatorName = "";
      }

      return networkOperatorName;
    }

    public string GetMccMnc()
    {
      string mccmnc = "";
      if (Application.platform != RuntimePlatform.Android)
      {
        Debug.Log("Not on android device.");
        return "";
      }

      AndroidJavaObject telManager = GetTelephonyManager();

      if (telManager == null)
      {
        Debug.Log("Can't get telephony manager!");
        return "";
      }

      mccmnc = telManager.Call<String>("getNetworkOperator", new object[0]);

      if (mccmnc == null) {
        return null;
      }

      if (mccmnc.Length < 5 || mccmnc.Length > 6) {
        return null;
      }
      return mccmnc;
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

    public string GetMccMnc()
    {
      string mccmnc = null;
      if (Application.platform == RuntimePlatform.IPhonePlayer)
      {
        mccmnc = _getMccMnc();
      }
      return mccmnc;
    }
#else
    public string GetCurrentCarrierName()
    {
      Debug.Log("GetCurrentCarrierName is NOT IMPLEMENTED");
      return "";
    }

    public string GetMccMnc()
    {
      Debug.Log("GetMccMnc is NOT IMPLEMENTED");
      return null;
    }
#endif
  }
}
