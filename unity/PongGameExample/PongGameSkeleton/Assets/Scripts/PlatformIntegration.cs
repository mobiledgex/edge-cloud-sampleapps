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
    
    return networkOperatorName;
  }
#elif UNITY_IOS
    public string GetCurrentCarrierName()
    {
      string networkOperatorName = "";
      
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