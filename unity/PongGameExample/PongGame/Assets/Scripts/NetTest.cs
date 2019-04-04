using System.Collections;
using System.Collections.Generic;

using System.Net.Sockets;
using System.Diagnostics;
using System;

public class NetTest
{
  private Stopwatch stopWatch;

  public NetTest()
  {
    stopWatch = new Stopwatch();
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

}
