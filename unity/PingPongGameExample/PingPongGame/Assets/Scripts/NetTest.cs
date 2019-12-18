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

using System.Collections.Concurrent;

using System.Net.Sockets;
using System.Net.Http;
using System.Net.NetworkInformation;
using System.Diagnostics;
using System.Threading;

using System;

public class NetTest
{
  public enum TestType
  {
    PING = 0,
    CONNECT = 1,
  };

  public class Site
  {
    public string host;
    public int port;
    public string L7Path; // This may be load balanced.
    public double lastPingMs;

    public TestType testType;

    int idx;
    int size;
    public double[] samples;

    public double average;
    public double stddev;

    public Site(TestType testType = TestType.CONNECT, int numSamples = 5)
    {
      this.testType = testType;
      samples = new double[numSamples];
    }

    public void addSample(double time)
    {
      samples[idx] = time;
      idx++;
      if (size < samples.Length) size++;
      idx = idx % samples.Length;
    }

    public void recalculateStats()
    {
      double acc = 0d;
      double vsum = 0d;
      double d;
      for (int i = 0; i < size; i++)
      {
        acc += samples[i];
      }
      average = acc / size;
      for (int i = 0; i < size; i++)
      {
        d = samples[i];
        vsum += (d - average) * (d - average);
      }
      if (size > 1) {
        // Bias Corrected Sample Variance
        vsum /= (size - 1);
      }
      stddev = Math.Sqrt(vsum);
    }
  }
  private Stopwatch stopWatch;

  public bool runTest;

  private Thread pingThread;
  public int PingIntervalMS { get; set; } = 5000;
  public int TestTimeoutMS = 5000;

  // For testing L7 Sites, possibly load balanced.
  HttpClient httpClient;

  public ConcurrentQueue<Site> sites { get; }

  public NetTest()
  {
    stopWatch = new Stopwatch();
    sites = new ConcurrentQueue<Site>();

    // TODO: GetConnection to connect from a particular network interface endpoint
    httpClient = new HttpClient();
    httpClient.Timeout = new TimeSpan(0, 0, TestTimeoutMS / 1000); // seconds
  }

  // Create a client and connect/disconnect on a server port. Not quite ping ICMP.
  public double ConnectAndDisconnect(string host, int port = 3000)
  {
    stopWatch.Reset();

    stopWatch.Start();
    TcpClient client = new TcpClient(host, port);
    client.Close();
    TimeSpan ts = stopWatch.Elapsed;
    stopWatch.Stop();
    return ts.TotalMilliseconds;
  }

  // Create a client and connect/disconnect on a partcular site.
  public double ConnectAndDisconnect(Site site)
  {
    stopWatch.Reset();

    // The nature of this app specific GET API call is to expect some kind of
    // stateless empty body return also 200 OK.
    stopWatch.Start();
    var result = httpClient.GetAsync(site.L7Path).GetAwaiter().GetResult();
    TimeSpan ts = stopWatch.Elapsed;
    stopWatch.Stop();

    if (result.StatusCode == System.Net.HttpStatusCode.OK) {
      return ts.TotalMilliseconds;
    }

    // Error, GET on L7 Path didn't return success.
    return -1d;
  }

  // Basic ICMP ping.
  public double Ping(Site site)
  {
    Ping ping = new Ping();
    PingReply reply = ping.Send(site.host, TestTimeoutMS);
    long elapsedMs = reply.RoundtripTime;

    return elapsedMs;
  }

  public bool doTest(bool enable)
  {
    if (runTest == true && enable == true)
    {
      return runTest;
    }

    runTest = enable;
    if (runTest)
    {
      pingThread = new Thread(RunNetTest);
      pingThread.Start();
    }
    else
    {
      pingThread.Join(PingIntervalMS);
      pingThread = null;
    }
    return runTest;
  }

  // Basic utility funtion to connect and disconnect from any TCP port.
  public void RunNetTest()
  {
    while (runTest)
    {
      double elapsed = -1d;
      foreach (Site site in sites)
      {
        switch (site.testType)
        {
          case TestType.CONNECT:
            if (site.L7Path == null) // Simple host and port.
            {
              elapsed = ConnectAndDisconnect(site.host, site.port);
            }
            else // Use L7 Path.
            {
              elapsed = ConnectAndDisconnect(site);
            }
            break;
          case TestType.PING:
            {
              elapsed = Ping(site);
            }
            break;
        }
        site.lastPingMs = elapsed;
        if (elapsed >= 0)
        {
          site.addSample(elapsed);
          site.recalculateStats();
        }

        UnityEngine.Debug.Log("Round trip to host: " + site.host + ", port: " + site.port + ", elapsed: " + elapsed + ", average: " + site.average + ", stddev: " + site.stddev);
      }
      Thread.Sleep(PingIntervalMS);
    }
  }
}
