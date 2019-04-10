using System.Collections;
using System.Collections.Generic;

using System;
using System.IO;
using System.Linq;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Concurrent;

using UnityEngine;

namespace MexPongGame
{
  // C#'s built in WebSockets concurrency model supports the use a single queue for
  // send, and another queue for recieve. WsClient here has 1 independent thread
  // per send or receive direction of communication.
  public class WsClient
  {
    // Life of WsClient:
    private static string proto = "ws";
    private static string host = "localhost";
    private static int port = 3000;
    private static string server = proto + "://" + host + ":" + port;

    public Uri uri = new Uri(server);
    private ClientWebSocket ws = new ClientWebSocket();
    static UTF8Encoding encoder; // For websocket text message encoding.
    const UInt64 MAXREADSIZE = 1 * 1024 * 1024;

    public ConcurrentQueue<String> receiveQueue { get; }
    public BlockingCollection<ArraySegment<byte>> sendQueue { get; }

    Thread receiveThread { get; set; }
    Thread sendThread { get; set; }

    Thread netTestThread;
    const int testIntervalMs = 5000;
    bool shouldRun = true;

    // TODO: CancellationToken for Tasks to handle OnApplicationFocus, OnApplicationPause.
    public WsClient()
    {
      encoder = new UTF8Encoding();
      ws = new ClientWebSocket();

      // This long running WebSocket thread must be kept alive to receive server WebSocket messages.
      ShouldRunThreads(true);

      receiveQueue = new ConcurrentQueue<string>();
      receiveThread = new Thread(RunReceive);
      receiveThread.Start();

      sendQueue = new BlockingCollection<ArraySegment<byte>>();
      sendThread = new Thread(RunSend);
      sendThread.Start();

      // Mini diagnostics.
      netTestThread = new Thread(() => RunNetTest(host, port, testIntervalMs));
      netTestThread.Start();
    }

    public bool ShouldRunThreads(bool run)
    {
      // Run flag conditional in run loops.
      shouldRun = run;

      if (shouldRun)
      {
        // Mini network diagnostics, parameterized to one server.
        netTestThread = new Thread(() => RunNetTest(host, port, testIntervalMs));
        netTestThread.Start();
      }
      return shouldRun;
    }

    public bool isConnecting()
    {
      return ws.State == WebSocketState.Connecting;
    }

    public bool isOpen()
    {
      return ws.State == WebSocketState.Open;
    }

    // Basic utility funtion to connect and disconnect from any TCP port.
    public void RunNetTest(string ahost, int aport, int intervalMs = 5000)
    {
      NetTest nt = new NetTest();
      while (shouldRun)
      {
        double elapsed = nt.ConnectAndDisconnect(ahost, aport);
        Debug.Log("Round(-ish) trip to host: " + ahost + ", port: " + aport + ", elapsed: " + elapsed);
        Thread.Sleep(intervalMs);
      }
    }

    public async Task Connect(Uri uri)
    {
      Debug.Log("Connecting to: " + uri);
      await ws.ConnectAsync(uri, CancellationToken.None);
      while (ws.State == WebSocketState.Connecting)
      {
        Debug.Log("Waiting to connect...");
        Task.Delay(50).Wait();
      }
      Debug.Log("Connect status: " + ws.State);
    }

    public void Send(string message)
    {
      byte[] buffer = encoder.GetBytes(message);
      //Debug.Log("Message to queue for send: " + buffer.Length + ", message: " + message);
      var sendBuf = new ArraySegment<byte>(buffer);

      sendQueue.Add(sendBuf);
    }

    public async void RunSend()
    {
      ArraySegment<byte> msg;
      Debug.Log("RunSend entered.");
      while (true)
      {
        while(!sendQueue.IsCompleted)
        {
          msg = sendQueue.Take();
          //Debug.Log("Dequeued this message to send: " + msg);
          await ws.SendAsync(msg, WebSocketMessageType.Text, true /* is last part of message */, CancellationToken.None);
        }
      }
    }

    // This belongs in a background thread posting queued results for the UI thread to pick up.
    public async Task<string> Receive(UInt64 maxSize = MAXREADSIZE)
    {
      // A read buffer, and a memory stream to stuff unknown number of chunks into:
      byte[] buf = new byte[4 * 1024];
      var ms = new MemoryStream();
      ArraySegment<byte> arrayBuf = new ArraySegment<byte>(buf);
      WebSocketReceiveResult chunkResult = null;

      if (ws.State == WebSocketState.Open)
      {
        do
        {
          chunkResult = await ws.ReceiveAsync(arrayBuf, CancellationToken.None);
          ms.Write(arrayBuf.Array, arrayBuf.Offset, chunkResult.Count);
          //Debug.Log("Size of Chunk message: " + chunkResult.Count);
          if ((UInt64)(chunkResult.Count) > MAXREADSIZE)
          {
            Console.Error.WriteLine("Warning: Message is bigger than expected!");
          }
        } while (!chunkResult.EndOfMessage);
        ms.Seek(0, SeekOrigin.Begin);

        // Looking for UTF-8 JSON type messages.
        if (chunkResult.MessageType == WebSocketMessageType.Text)
        {
          return StreamToString(ms, Encoding.UTF8);
        }

      }

      return "";
    }

    public async void RunReceive()
    {
      Debug.Log("WebSocket Message Receiver looping.");
      string result;
      while (true)
      {
        //Debug.Log("Awaiting Receive...");
        result = await Receive();
        if (result != null && result.Length > 0)
        {
          receiveQueue.Enqueue(result);
        }
      }
    }

    static string StreamToString(MemoryStream ms, Encoding encoding)
    {
      string readString = "";
      if (encoding == Encoding.UTF8)
      {
        using (var reader = new StreamReader(ms, encoding))
        {
          readString = reader.ReadToEnd();
        }
      }

      return readString;
    }
  }
}