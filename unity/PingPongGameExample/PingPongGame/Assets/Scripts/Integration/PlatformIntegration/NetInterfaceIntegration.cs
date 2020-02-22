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
using UnityEngine;
using DistributedMatchEngine;

// We need this one for importing our IOS functions
using System.Runtime.InteropServices;
using System.Net.Sockets;

namespace MobiledgeXPingPongGame
{
  public class NetInterfaceClass : NetInterface
  {
    NetworkInterfaceName networkInterfaceName;

    public NetInterfaceClass(NetworkInterfaceName networkInterfaceName)
    {
      SetNetworkInterfaceName(networkInterfaceName);
    }

    public NetworkInterfaceName GetNetworkInterfaceName()
    {
      return networkInterfaceName;
    }

    public void SetNetworkInterfaceName(NetworkInterfaceName networkInterfaceName)
    {
      this.networkInterfaceName = networkInterfaceName;
    }

#if UNITY_ANDROID

    AndroidNetworkInterfaceName name = new AndroidNetworkInterfaceName();

    AndroidJavaObject GetNetworkInterface(String netInterfaceType)
    {
      AndroidJavaClass networkInterfaceClass = PlatformIntegrationUtil.getAndroidJavaClass("java.net.NetworkInterface");
      if (networkInterfaceClass == null)
      {
        Debug.Log("Unable to get network interface class");
        return null;
      }

      object[] netParams = new object[1];
      netParams[0] = netInterfaceType;

      AndroidJavaObject networkInterface = PlatformIntegrationUtil.callStatic(networkInterfaceClass, "getByName", netParams);
      return networkInterface;
    }

    IntPtr GetInetAddress(AndroidJavaObject networkInterface)
    {
      AndroidJavaObject inetAddresses = PlatformIntegrationUtil.call(networkInterface, "getInetAddresses");
      if (inetAddresses == null)
      {
        Debug.Log("Could not get inetAddresses");
        return IntPtr.Zero;
      }

      IntPtr inetAddressesRaw = inetAddresses.GetRawObject();  // Get pointer to inetAddresses (Java enum), so that we can iterate through it
      IntPtr inetAddressesClass = AndroidJNI.GetObjectClass(inetAddressesRaw);
      if(inetAddressesClass == IntPtr.Zero)
      {
        Debug.Log("Could not get inetAddressesClass");
        return IntPtr.Zero;
      }

      IntPtr nextMethod = AndroidJNIHelper.GetMethodID(inetAddressesClass, "nextElement");
      if (nextMethod == IntPtr.Zero)
      {
        return IntPtr.Zero;
      }

      IntPtr inetAddress = AndroidJNI.CallObjectMethod(inetAddressesRaw, nextMethod, new jvalue[] { });
      return inetAddress;
    }

    string GetHostAddress(IntPtr inetAddress)
    {
      IntPtr inetAddressClass = AndroidJNI.GetObjectClass(inetAddress);
      IntPtr getHostAddressMethod = AndroidJNIHelper.GetMethodID(inetAddressClass, "getHostAddress");

      String ipAddress = AndroidJNI.CallStringMethod(inetAddress, getHostAddressMethod, new jvalue[] { });
      return ipAddress;
    }

    string GetNetworkInterfaceIP(String netInterfaceType)
    {
      AndroidJavaObject networkInterface = GetNetworkInterface(netInterfaceType);
      if (networkInterface == null)
      {
        return "";
      }

      IntPtr inetAddress = GetInetAddress(networkInterface);
      if (inetAddress == IntPtr.Zero)
      {
        return "";
      }

      String ipAddress = GetHostAddress(inetAddress);
      return ipAddress;
    }

    public string GetIPAddress(String netInterfaceType, AddressFamily adressFamily = AddressFamily.InterNetwork)
    {
      return GetNetworkInterfaceIP(netInterfaceType);
    }

    public bool HasWifi()
    {
      if (GetNetworkInterfaceIP(name.WIFI) != "")
      {
        return true;
      }
      return false;
    }

    public bool HasCellular()
    {
      if (GetNetworkInterfaceIP(name.CELLULAR) != "")
      {
        return true;
      }
      return false;
    }

#elif UNITY_IOS

    [DllImport("__Internal")]
    private static extern string _getIPAddress(string netInterfaceType);

    [DllImport("__Internal")]
    private static extern bool _isWifi();

    [DllImport("__Internal")]
    private static extern bool _isCellular();

    public string GetIPAddress(String netInterfaceType, AddressFamily adressFamily = AddressFamily.InterNetwork)
    {
      string ipAddress = null;
      if (Application.platform == RuntimePlatform.IPhonePlayer)
      {
        ipAddress = _getIPAddress(netInterfaceType);
      }
      return ipAddress;
    }

    public bool HasWifi()
    {
      bool isWifi = false;
      if (Application.platform == RuntimePlatform.IPhonePlayer)
      {
        isWifi = _isWifi();
      }
      return isWifi;
    }

    public bool HasCellular()
    {
      bool isCellular = false;
      if (Application.platform == RuntimePlatform.IPhonePlayer)
      {
        isCellular = _isCellular();
      }
      return isCellular;
    }

#else
    public string GetIPAddress(String netInterfaceType, AddressFamily adressFamily = AddressFamily.InterNetwork)
    {
      Debug.Log("GetIPAddress is NOT IMPLEMENTED");
      return null;
    }

    public bool HasWifi()
    {
      Debug.Log("HasWifi is NOT IMPLEMENTED");
      return false;
    }

    public bool HasCellular()
    {
      Debug.Log("HasCellular is NOT IMPLEMENTED");
      return false;
    }
#endif
  }
}