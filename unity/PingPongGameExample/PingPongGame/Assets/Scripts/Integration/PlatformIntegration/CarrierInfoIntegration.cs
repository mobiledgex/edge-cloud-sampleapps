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
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Android;
using DistributedMatchEngine;

// We need this one for importing our IOS functions
using System.Runtime.InteropServices;

namespace MobiledgeXPingPongGame
{
  public class CarrierInfoClass : CarrierInfo
  {

#if UNITY_ANDROID // PC android target builds to through here as well.

    int sdkVersion;

    AndroidJavaObject cellInfoLte;
    AndroidJavaObject cellInfoGsm;
    AndroidJavaObject cellInfoWcdma;
    AndroidJavaObject cellInfoCdma;
    AndroidJavaObject cellInfoTdscdma;
    AndroidJavaObject cellInfoNr;

    string cellInfoLteString;
    string cellInfoGsmString;
    string cellInfoWcdmaString;
    string cellInfoCdmaString;
    string cellInfoTdscdmaString;
    string cellInfoNrString;

    public CarrierInfoClass()
    {
      sdkVersion = getAndroidSDKVers();
      if (sdkVersion < 0)
      {
        Debug.Log("Could not get valid sdkVersion: " + sdkVersion);
        return;
      }

      if (sdkVersion >= 17) {
        cellInfoLte = PlatformIntegrationUtil.getAndroidJavaObject("android.telephony.CellInfoLte");
        cellInfoLteString = cellInfoLte != null ? PlatformIntegrationUtil.getSimpleName(cellInfoLte) : "";

        cellInfoGsm = PlatformIntegrationUtil.getAndroidJavaObject("android.telephony.CellInfoGsm");
        cellInfoGsmString = cellInfoGsm != null ? PlatformIntegrationUtil.getSimpleName(cellInfoGsm) : "";

        cellInfoCdma = PlatformIntegrationUtil.getAndroidJavaObject("android.telephony.CellInfoCdma");
        cellInfoCdmaString = cellInfoCdma != null ? PlatformIntegrationUtil.getSimpleName(cellInfoCdma) : "";
      }

      if (sdkVersion >= 18) {
        cellInfoWcdma = PlatformIntegrationUtil.getAndroidJavaObject("android.telephony.CellInfoWcdma");
        cellInfoWcdmaString = cellInfoWcdma != null ? PlatformIntegrationUtil.getSimpleName(cellInfoWcdma) : "";
      }
      
      if (sdkVersion >= 28)
      {
        cellInfoTdscdma = PlatformIntegrationUtil.getAndroidJavaObject("android.telephony.CellInfoTdscdma");
        cellInfoTdscdmaString = cellInfoTdscdma != null ? PlatformIntegrationUtil.getSimpleName(cellInfoTdscdma) : "";
      }
      if (sdkVersion >= 29)
      {
        cellInfoNr = PlatformIntegrationUtil.getAndroidJavaObject("android.telephony.CellInfoNr");
        cellInfoNrString = cellInfoNr != null ? PlatformIntegrationUtil.getSimpleName(cellInfoNr) : "";
      }
    }

    public int getAndroidSDKVers()
    {
      AndroidJavaClass version = PlatformIntegrationUtil.getAndroidJavaClass("android.os.Build$VERSION");
      if (version == null)
      {
        Debug.Log("Unable to get Build Version");
        return -1;
      }
      return PlatformIntegrationUtil.getStaticInt(version, "SDK_INT");
    }

    AndroidJavaObject GetTelephonyManager()
    {
      AndroidJavaClass unityPlayer = PlatformIntegrationUtil.getAndroidJavaClass("com.unity3d.player.UnityPlayer");
      if (unityPlayer == null)
      {
        Debug.Log("Unable to get UnityPlayer");
        return null;
      }
      AndroidJavaObject activity = PlatformIntegrationUtil.getStatic(unityPlayer, "currentActivity");
      if (activity == null)
      {
        Debug.Log("Can't find an activity!");
        return null;
      }

      AndroidJavaObject context = PlatformIntegrationUtil.call(activity, "getApplicationContext");
      if (context == null)
      {
        Debug.Log("Can't find an app context!");
        return null;
      }

      // Context.TELEPHONY_SERVICE:
      string CONTEXT_TELEPHONY_SERVICE = context.GetStatic<String>("TELEPHONY_SERVICE");
      if (CONTEXT_TELEPHONY_SERVICE == null)
      {
        Debug.Log("Can't get Context Telephony Service");
        return null;
      }

      AndroidJavaObject telManager = PlatformIntegrationUtil.call(context, "getSystemService", new object[] {CONTEXT_TELEPHONY_SERVICE});
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

      networkOperatorName = PlatformIntegrationUtil.callString(telManager, "getNetworkOperatorName");
      if (networkOperatorName == null)
      {
        Debug.Log("Network Operator Name is not found on the device");
        networkOperatorName = "";
      }

      return networkOperatorName;
    }

    public string GetMccMnc()
    {
      string mccmnc = null;
      if (Application.platform != RuntimePlatform.Android)
      {
        Debug.Log("Not on android device.");
        return null;
      }

      AndroidJavaObject telManager = GetTelephonyManager();
      if (telManager == null)
      {
        Debug.Log("Can't get telephony manager!");
        return null;
      }

      mccmnc = PlatformIntegrationUtil.callString(telManager, "getNetworkOperator");
      if (mccmnc == null)
      {
        return null;
      }

      if (mccmnc.Length < 5)
      {
        return null;
      }
      return mccmnc;
    }

    KeyValuePair<string, uint> GetCidKeyValuePair(AndroidJavaObject cellInfo)
    {
      KeyValuePair<string, uint> pair = new KeyValuePair<string, uint>(null, 0);

      string simpleName = PlatformIntegrationUtil.getSimpleName(cellInfo);
      AndroidJavaObject cellIdentity = PlatformIntegrationUtil.call(cellInfo, "getCellIdentity");
      if (cellIdentity == null)
      {
        Debug.Log("Unable to get cellIdentity");
        return pair;
      }

      if (simpleName.Equals(cellInfoTdscdmaString))
      {
        int cid = PlatformIntegrationUtil.callInt(cellIdentity, "getCid");
        if (cid != -1)
        {
          pair = new KeyValuePair<string, uint>(simpleName, (uint)cid);
        }
      }
      else if (simpleName.Equals(cellInfoNrString))
      {
        int nci = PlatformIntegrationUtil.callInt(cellIdentity, "getNci");
        if (nci != -1)
        {
          pair = new KeyValuePair<string, uint>(simpleName, (uint)nci);
        }
      }
      else if (simpleName.Equals(cellInfoLteString))
      {
        int ci = PlatformIntegrationUtil.callInt(cellIdentity, "getCi");
        if (ci != -1)
        {
          pair = new KeyValuePair<string, uint>(simpleName, (uint)ci);
        }
      }
      else if (simpleName.Equals(cellInfoGsmString))
      {
        int cid = PlatformIntegrationUtil.callInt(cellIdentity, "getCid");
        if (cid != -1)
        {
          pair = new KeyValuePair<string, uint>(simpleName, (uint)cid);
        }
      }
      else if (simpleName.Equals(cellInfoWcdmaString))
      {
        int cid = PlatformIntegrationUtil.callInt(cellIdentity, "getCid");
        if (cid != -1)
        { 
          pair = new KeyValuePair<string, uint>(simpleName, (uint)cid);
        }
      }
      else if (simpleName.Equals(cellInfoCdmaString))
      {
        int baseStationId = PlatformIntegrationUtil.callInt(cellIdentity, "getBaseStationId");
        if (baseStationId != -1)
        { 
          pair = new KeyValuePair<string, uint>(simpleName, (uint)baseStationId);
        }
      }
      else
      {
        Debug.Log("Object is not an instance of a CellInfo class");
      }

      return pair;

    }

    public List<KeyValuePair<String, uint>> GetCellInfoList()
    {
      if (Application.platform != RuntimePlatform.Android)
      {
        Debug.Log("Not on android device.");
        return null;
      }

      AndroidJavaObject telManager = GetTelephonyManager();
      if (telManager == null)
      {
        Debug.Log("Can't get telephony manager!");
        return null;
      }

      if (!Permission.HasUserAuthorizedPermission(Permission.FineLocation))
      {
        Permission.RequestUserPermission(Permission.FineLocation);
      }

      AndroidJavaObject cellInfoList = PlatformIntegrationUtil.call(telManager, "getAllCellInfo");
      if (cellInfoList == null)
      {
        Debug.Log("Can't get list of cellInfo objects.");
        return null;
      }

      int length = PlatformIntegrationUtil.callInt(cellInfoList, "size");
      if (length <= 0)
      {
        Debug.Log("Unable to get valid length for cellInfoList");
        return null;
      }

      List<KeyValuePair<String, uint>> cellIDList = new List<KeyValuePair<string, uint>>();
      // KeyValuePair to compare to in case GetCidKeyValuePair returns nothing
      KeyValuePair<string,uint> empty = new KeyValuePair<string, uint>(null, 0);

      for (int i = 0; i < length; i++)
      {
        AndroidJavaObject cellInfo = PlatformIntegrationUtil.call(cellInfoList, "get", new object[] {i});
        if (cellInfo == null) continue;
        
        bool isRegistered = PlatformIntegrationUtil.callBool(cellInfo, "isRegistered");
        if (isRegistered)
        {
          KeyValuePair<string, uint> pair = GetCidKeyValuePair(cellInfo);
          if (!pair.Equals(empty))
          {
            cellIDList.Add(pair);
          }
        }
      }

      return cellIDList;
    }

    public uint GetCellID()
    {
      uint cellID = 0;

      List<KeyValuePair<String, uint>> cellInfoList = GetCellInfoList();

      if (cellInfoList == null || cellInfoList.Count == 0)
      {
        Debug.Log("no cellID");
        return cellID;
      }

      KeyValuePair<String, uint> pair = cellInfoList[0]; // grab first value

      return pair.Value;
    }

#elif UNITY_IOS

    // Sets iOS platform specific internal callbacks (reference counted objects), etc.
    [DllImport("__Internal")]
    private static extern string _ensureMatchingEnginePlatformIntegration();

    [DllImport("__Internal")]
    private static extern string _getCurrentCarrierName();

    [DllImport("__Internal")]
    private static extern string _getMccMnc();

    [DllImport("__Internal")]
    private static extern int _getCellID();

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

    public uint GetCellID()
    {
      int cellID = 0;
      if (Application.platform == RuntimePlatform.IPhonePlayer)
      {
        cellID = _getCellID();
      }
      return (uint)cellID;
    }

#else

    // Implement CarrierInfo
    public string GetCurrentCarrierName()
    {
      Debug.Log("GetCurrentCarrierName is NOT IMPLEMENTED");
      return null;
    }

    public string GetMccMnc()
    {
      Debug.Log("GetMccMnc is NOT IMPLEMENTED");
      return null;
    }

    public uint GetCellID()
    {
      Debug.Log("GetCellID is NOT IMPLEMENTED");
      return 0;
    }

#endif
  }

  // Used for testing in UnityEditor (any target platform)
  public class TestCarrierInfoClass : CarrierInfo
  {
    // Implement CarrierInfo
    public string GetCurrentCarrierName()
    {
      return "wifi";
    }

    public string GetMccMnc()
    {
      return "26201";
    }

    public uint GetCellID()
    {
      return 0;
    }
  }
}
