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
      if (!sites.IsEmpty)
      {
        foreach (HostAndPort site in sites)
        {
          UnityEngine.Debug.Log("Pinging: " + site.host + ", port: " + site.port);
          double elapsed = nt.ConnectAndDisconnect(site.host, site.port);
          site.lastPingMs = elapsed;
          UnityEngine.Debug.Log("Round(-ish) trip to host: " + site.host + ", port: " + site.port + ", elapsed: " + elapsed);
        }
      }
      await Task.Delay(5000);
    }
  }
}
