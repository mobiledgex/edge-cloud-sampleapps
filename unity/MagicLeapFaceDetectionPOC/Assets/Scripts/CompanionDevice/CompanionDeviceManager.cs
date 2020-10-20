/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
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
using System.Collections;
using System.Diagnostics;
using System.Threading.Tasks;
using UnityEngine;

namespace CompanionApp
{
  // MonoBehavior wrapper for CompanionDevice state class, using Update as the
  // event handler for updating location, the DME, and when to run
  // Find Cloudlet.

  public class UserInfo
  {
    public string user_name;
    public string user_email;
    public string user_id;
  }

  public enum CompanionDeviceState
  {
    None,
    NeedCompanionAppHost,
    HaveCompanionAppHost,
    SentDeviceListRequest,
    HaveDeviceList,
    SentDeviceRegister,
    HaveDeviceRegistered,
    HaveDmeHost,
    HaveLocation,
    HaveAppInstHost
  };

  // GameObject that manages CompanionDevice.
  public class CompanionDeviceManager : MonoBehaviour
  {
    string TAG = "CompanionDeviceManager";
    // FIXME: Location update interval. Without a push interface over
    // an open port, we'll just poll every so often.
    Stopwatch stopWatch;
    // This needs to be under App Control.
    TimeSpan interval = new TimeSpan(0, 0, 0, 50);

    public CompanionDevice companionDevice;
    public CompanionDeviceState State { get; private set; }

    // This requires a UI:

    // Task Watcher, spinning on Update() calls.
    Task<DmeHostPrefixReply> UpdateDmeHostTask;
    Task<LocationReply> UpdateLocationTask;

    Task<string> registerAndFindCloudletTask;

    bool requestUpdate;
    public bool UpdateRunning { get; private set; }
    public DeviceRegisterRequest CurrentDeviceRegisterRequest { get; set; }

    private void ensureCompanionDevice()
    {
      if (companionDevice == null)
      {
        companionDevice = new CompanionDevice();
      }
    }

    public void Start()
    {
      Logger.D(TAG, "CompanionDeviceManager Start()");
      stopWatch = new Stopwatch();
      stopWatch.Start();

      ensureCompanionDevice();
      State = CompanionDeviceState.NeedCompanionAppHost;
    }

    public void Update()
    {
      ensureCompanionDevice();
      if (companionDevice == null)
      {
        Logger.E(TAG, "CompanionDevice instance is null. No location possible.");
        return;
      }

      if (UpdateRunning)
      {
        requestUpdate = false;
        return;
      }

      if (requestUpdate || stopWatch.Elapsed >= interval)
      {
        Logger.D(TAG, "Interval elapsed UpdateCompanionDevice()");
        StartCoroutine(UpdateCompanionDevice());
        stopWatch.Restart();
      }
      requestUpdate = false;

    }

    // Wrappers:
    public void SetCompanionAppHost(string host)
    {
      companionDevice.companionAppHost = host;
      State = (host == null || host == "") ? CompanionDeviceState.NeedCompanionAppHost : CompanionDeviceState.HaveCompanionAppHost;
    }

    /**
     * The CompanionApp Host
     */
    public string GetCompanionAppHost()
    {
      return companionDevice.companionAppHost;
    }

    /**
     * Returns the edge the device is interested in, as per appInst profile fields in MobiledgeXIntegration.
     */
    public string GetAppInstHost()
    {
      return companionDevice.CurrentAppInstHost;
    }

    async public Task<DeviceListReply> DeviceList(UserInfo info, string host, int port)
    {
      if (companionDevice == null || companionDevice.companionAppHost == null)
      {
        return null;
      }

      Task<DeviceListReply> replyTask = companionDevice.DeviceList(
        userInfo: info,
        host: host,
        port: port);
      State = CompanionDeviceState.SentDeviceListRequest;

      var reply = await replyTask;
      if (reply != null && reply.success)
      {
        State = CompanionDeviceState.HaveDeviceList;
      }

      return reply;
    }

    async public Task<DeviceRegisterReply> DeviceRegister(UserInfo userInfo, Device chosenDevice, string host, int port)
    {
      if (companionDevice == null || companionDevice.companionAppHost == null)
      {
        return null;
      }

      Task<DeviceRegisterReply> replyTask = companionDevice.DeviceRegister(
        info: userInfo,
        device: chosenDevice,
        host: host,
        port: port);
      State = CompanionDeviceState.SentDeviceRegister;
      var reply = await replyTask;

      if (reply != null && reply.success)
      {
        State = CompanionDeviceState.HaveDeviceRegistered;
      }

      return reply;
    }

    // TODO: Given there's no UI, the updater will just have to use the first one.
    IEnumerator SelectAndRegisterDevice()
    {
      //UpdateDeviceList DevicecompanionDevice.DeviceList();
      return null;
    }

    // Just request an update. Update() will handle from it's own thread context.
    public void RequestUpdateCompanionDevice()
    {
      if (!UpdateRunning)
      {
        requestUpdate = true;
      }
    }

    /**
     * These tasks will update the Companion device on a Unity Update() Schedule.
     */
    IEnumerator UpdateCompanionDevice()
    {
      Logger.D(TAG, "UpdateCompanionDevice() Entry.");
      try
      {
        UpdateRunning = true;
        stopWatch.Reset();

        // One at a time:

        UpdateDmeHostTask = companionDevice.GetDmeHost();
        // Update DME host:
        while (!UpdateDmeHostTask.IsCompleted)
        {
          yield return null;
        }
        if (!UpdateDmeHostTask.IsFaulted && UpdateDmeHostTask.Result != null)
        {
          State = CompanionDeviceState.HaveDmeHost;
        }
        UpdateDmeHostTask = null;

        // Update CompanionApp based location:
        UpdateLocationTask = companionDevice.GetLocation();
        while (!UpdateLocationTask.IsCompleted)
        {
          yield return null;
        }
        if (!UpdateLocationTask.IsFaulted && UpdateLocationTask.Result != null)
        {
          State = CompanionDeviceState.HaveLocation;
        }
        UpdateLocationTask = null;


        // Now that we have something, start a findCloudlet task on the new info
        // we just got:
        registerAndFindCloudletTask = companionDevice.RegisterAndFindCloudlet();
        while (!registerAndFindCloudletTask.IsCompleted)
        {
          //Logger.D(TAG, "UpdateCompanionDevice() RegisterAndFindCloudlet waiting.");
          yield return null;
        }
        if (!registerAndFindCloudletTask.IsFaulted && registerAndFindCloudletTask.Result != null)
        {
          State = CompanionDeviceState.HaveAppInstHost;
        }
        registerAndFindCloudletTask = null;
      }
      finally
      {
        // CompanionDevice is now up to date here.
        UpdateRunning = false;
      }
    }

    public bool isReady()
    {
      return State == CompanionDeviceState.HaveAppInstHost;
    }
  }
}

