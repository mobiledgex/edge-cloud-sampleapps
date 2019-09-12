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

using System.Collections;
using System.Collections.Concurrent;

using System.Net.Sockets;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;

using System;

public class NetTest
{
  public class HostAndPort
  {
    public string host;
    public int port;
    public double lastPingMs = 0f;
  }
  private Stopwatch stopWatch;

  public bool shouldPing = false;
  private Thread pingThread;
  public int PingIntervalMS { get; set; } = 5000;

  public ConcurrentQueue<HostAndPort> sites;

  public NetTest()
  {
    stopWatch = new Stopwatch();
    sites = new ConcurrentQueue<HostAndPort>();
  }

  // Create a client and connect/disconnect on a server port. Not quite ping ICMP.
  public double ConnectAndDisconnect(string host, int port=3000)
  {
    stopWatch.Reset();
    stopWatch.Start();
    TcpClient client = new TcpClient(host, port);
    client.Close();
    TimeSpan ts = stopWatch.Elapsed;
    stopWatch.Stop();
    return ts.TotalMilliseconds;
  }

  public bool doPing(bool enable)
  {
    shouldPing = enable;

    if (shouldPing)
    {
      pingThread = new Thread(RunNetTest);
      pingThread.Start();
    } else
    {
      pingThread = null;
    }
    return shouldPing;
  }

  // Basic utility funtion to connect and disconnect from any TCP port.
  public async void RunNetTest()
  {
    NetTest nt = new NetTest();
    while (shouldPing)
    {
      foreach (HostAndPort site in sites)
      {
        UnityEngine.Debug.Log("Pinging: " + site.host + ", port: " + site.port);
        double elapsed = nt.ConnectAndDisconnect(site.host, site.port);
        site.lastPingMs = elapsed;
        UnityEngine.Debug.Log("Round(-ish) trip to host: " + site.host + ", port: " + site.port + ", elapsed: " + elapsed);
      }
      await Task.Delay(5000);
    }
  }
}
