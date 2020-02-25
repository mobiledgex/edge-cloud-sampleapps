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

// We need this one for importing our IOS functions
using System.Runtime.InteropServices;

namespace MobiledgeXPingPongGame
{
  public class PlatformIntegrationUtil
  {

#if UNITY_ANDROID

    // empty parameters for JNI calls
    private static object[] emptyObjectArr = new object[0];

    public static AndroidJavaClass getAndroidJavaClass(string pkg)
    {
      try
      {
        return new AndroidJavaClass(pkg);
      }
      catch (Exception e)
      {
        Debug.Log("Could not get AndroidJavaClass " + pkg + ". Exception is: " + e.Message);
        return null;
      }
    }

    public static AndroidJavaObject getAndroidJavaObject(string pkg)
    {
      try
      {
        return new AndroidJavaObject(pkg);
      }
      catch (Exception e)
      {
        Debug.Log("Could not get AndroidJavaObject " + pkg + ". Exception is: " + e.Message);
        return null;
      }
    }

    public static string getSimpleName(AndroidJavaObject obj)
    {
      try
      {
        return obj.Call<AndroidJavaObject>("getClass", emptyObjectArr).Call<string>("getSimpleName", emptyObjectArr);
      }
      catch (Exception e)
      {
        Debug.Log("Could not getSimpleName. Exception is " + e.Message);
        return "";
      }
    }
   
    // Generic functions that get static variables, call static functions, and call object functions

    public static AndroidJavaObject getStatic(AndroidJavaClass c, string member)
    {
      try
      {
        return c.GetStatic<AndroidJavaObject>(member);
      }
      catch (Exception e)
      {
        Debug.Log("Could not getStatic object. Exception: " + e.Message);
        return null;
      }
    }

    public static string getStaticString(AndroidJavaClass c, string member)
    {
      try
      {
        return c.GetStatic<string>(member);
      }
      catch (Exception e)
      {
        Debug.Log("Could not getStatic string. Exception: " + e.Message);
        return "";
      }
    }

    public static int getStaticInt(AndroidJavaClass c, string member)
    {
      try
      {
        return c.GetStatic<int>(member);
      }
      catch (Exception e)
      {
        Debug.Log("Could not getStatic int. Exception: " + e.Message);
        return -1;
      }
    }

    public static AndroidJavaObject callStatic(AndroidJavaClass c, string method, object[] param = null)
    {
      if (param == null)
      {
        param = emptyObjectArr;
      }

      try
      {
        return c.CallStatic<AndroidJavaObject>(method, param);
      }
      catch (Exception e)
      {
        Debug.Log("Could not callStatic object. Exception: " + e.Message);
        return null;
      }
    }

    public static string callStaticString(AndroidJavaClass c, string method, object[] param = null)
    {
      if (param == null)
      {
        param = emptyObjectArr;
      }

      try
      {
        return c.CallStatic<string>(method, param);
      }
      catch (Exception e)
      {
        Debug.Log("Could not callStatic string. Exception: " + e.Message);
        return "";
      }
    }

    public static AndroidJavaObject call(AndroidJavaObject obj, string method, object[] param = null)
    {
      if (param == null)
      {
        param = emptyObjectArr;
      }

      try
      {
        return obj.Call<AndroidJavaObject>(method, param);
      }
      catch (Exception e)
      {
        Debug.Log("Could not call java method " + method + ". Exception: " + e.Message);
        return null;
      }
    }

    public static string callString(AndroidJavaObject obj, string method, object[] param = null)
    {
      if (param == null)
      {
        param = emptyObjectArr;
      }

      try
      {
        return obj.Call<string>(method, param);
      }
      catch (Exception e)
      {
        Debug.Log("Could not call java string method " + method + ". Exception: " + e.Message);
        return "";
      }
    }

    public static int callInt(AndroidJavaObject obj, string method, object[] param = null)
    {
      if (param == null)
      {
        param = emptyObjectArr;
      }

      try
      {
        return obj.Call<int>(method, param);
      }
      catch (Exception e)
      {
        Debug.Log("Could not call java int method " + method + ". Exception: " + e.Message);
        return -1;
      }
    }

    public static bool callBool(AndroidJavaObject obj, string method, object[] param = null)
    {
      if (param == null)
      {
        param = emptyObjectArr;
      }

      try
      {
        return obj.Call<bool>(method, param);
      }
      catch (Exception e)
      {
        Debug.Log("Could not call java bool method " + method + ". Exception: " + e.Message);
        return false;
      }
    }

#endif
  }
}
