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

    // empty parameters for JNI calls
    object[] emptyObjectArr = new object[0];

    int sdkVersion;

    AndroidJavaObject cellInfo;
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

      cellInfo = new AndroidJavaObject("android.telephony.CellInfo");

      cellInfoLte = new AndroidJavaObject("android.telephony.CellInfoLte");
      cellInfoLteString = cellInfoLte.Call<AndroidJavaObject>("getClass", emptyObjectArr).Call<string>("getSimpleName", new object[0]);

      cellInfoGsm = new AndroidJavaObject("android.telephony.CellInfoGsm");
      cellInfoGsmString = cellInfoGsm.Call<AndroidJavaObject>("getClass", emptyObjectArr).Call<string>("getSimpleName", new object[0]);

      cellInfoWcdma = new AndroidJavaObject("android.telephony.CellInfoWcdma");
      cellInfoWcdmaString = cellInfoWcdma.Call<AndroidJavaObject>("getClass", emptyObjectArr).Call<string>("getSimpleName", new object[0]);

      cellInfoCdma = new AndroidJavaObject("android.telephony.CellInfoCdma");
      cellInfoCdmaString = cellInfoCdma.Call<AndroidJavaObject>("getClass", emptyObjectArr).Call<string>("getSimpleName", new object[0]);

      if (sdkVersion >= 28)
      {
        cellInfoTdscdma = new AndroidJavaObject("android.telephony.CellInfoTdscdma");
        cellInfoTdscdmaString = cellInfoTdscdma.Call<AndroidJavaObject>("getClass", emptyObjectArr).Call<string>("getSimpleName", new object[0]);
      }
      if (sdkVersion >= 29)
      {
        cellInfoNr = new AndroidJavaObject("android.telephony.CellInfoNr");
        cellInfoNrString = cellInfoNr.Call<AndroidJavaObject>("getClass", emptyObjectArr).Call<string>("getSimpleName", new object[0]);
      }
    }

    public int getAndroidSDKVers()
    {
      AndroidJavaClass version = new AndroidJavaClass("android.os.Build$VERSION");
      return version.GetStatic<int>("SDK_INT");
    }

    AndroidJavaObject GetTelephonyManager()
    {
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

      networkOperatorName = telManager.Call<String>("getNetworkOperatorName", emptyObjectArr);

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

      mccmnc = telManager.Call<String>("getNetworkOperator", emptyObjectArr);

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

      AndroidJavaObject cellInfoClass = cellInfo.Call<AndroidJavaObject>("getClass", emptyObjectArr);
      string simpleName = cellInfoClass.Call<string>("getSimpleName", emptyObjectArr);

      if (simpleName.Equals(cellInfoTdscdmaString))
      {
        AndroidJavaObject cellIdentity = cellInfo.Call<AndroidJavaObject>("getCellIdentity", emptyObjectArr);
        int cid = cellIdentity.Call<int>("getCid", emptyObjectArr);
        pair = new KeyValuePair<string, uint>(simpleName, (uint)cid);
      }
      else if (simpleName.Equals(cellInfoNrString))
      {
        AndroidJavaObject cellIdentity = cellInfo.Call<AndroidJavaObject>("getCellIdentity", emptyObjectArr);
        int nci = cellIdentity.Call<int>("getNci", emptyObjectArr);
        pair = new KeyValuePair<string, uint>(simpleName, (uint)nci);
      }
      else if (simpleName.Equals(cellInfoLteString))
      {
        AndroidJavaObject cellIdentity = cellInfo.Call<AndroidJavaObject>("getCellIdentity", emptyObjectArr);
        int ci = cellIdentity.Call<int>("getCi", emptyObjectArr);
        pair = new KeyValuePair<string, uint>(simpleName, (uint)ci);
      }
      else if (simpleName.Equals(cellInfoGsmString))
      {
        AndroidJavaObject cellIdentity = cellInfo.Call<AndroidJavaObject>("getCellIdentity", emptyObjectArr);
        int cid = cellIdentity.Call<int>("getCid", emptyObjectArr);
        pair = new KeyValuePair<string, uint>(simpleName, (uint)cid);
      }
      else if (simpleName.Equals(cellInfoWcdmaString))
      {
        AndroidJavaObject cellIdentity = cellInfo.Call<AndroidJavaObject>("getCellIdentity", emptyObjectArr);
        int cid = cellIdentity.Call<int>("getCid", emptyObjectArr);
        pair = new KeyValuePair<string, uint>(simpleName, (uint)cid);
      }
      else if (simpleName.Equals(cellInfoCdmaString))
      {
        AndroidJavaObject cellIdentity = cellInfo.Call<AndroidJavaObject>("getCellIdentity", emptyObjectArr);
        int baseStationId = cellIdentity.Call<int>("getBaseStationId", emptyObjectArr);
        pair = new KeyValuePair<string, uint>(simpleName, (uint)baseStationId);
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

      AndroidJavaObject cellInfoList = telManager.Call<AndroidJavaObject>("getAllCellInfo", emptyObjectArr);
      if (cellInfoList == null)
      {
        Debug.Log("Can't get list of cellInfo objects.");
        return null;
      }

      int length = cellInfoList.Call<int>("size", emptyObjectArr);

      List<KeyValuePair<String, uint>> cellIDList = new List<KeyValuePair<string, uint>>();
      // KeyValuePair to compare to in case GetCidKeyValuePair returns nothing
      KeyValuePair<string,uint> empty = new KeyValuePair<string, uint>(null, 0);

      for (int i = 0; i < length; i++)
      {
        AndroidJavaObject cellInfo = cellInfoList.Call<AndroidJavaObject>("get", new object[] {i});
        
        bool isRegistered = cellInfo.Call<bool>("isRegistered", emptyObjectArr);
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
}
