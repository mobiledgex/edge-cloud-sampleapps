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
  public class WsClient
  {
    // Life of wsClient:
    private Uri uri = new Uri("ws://localhost:3000");
    private ClientWebSocket ws = new ClientWebSocket();
    static UTF8Encoding encoder; // For websocket text message encoding.
    const UInt64 MAXREADSIZE = 1 * 1024 * 1024;

    public ConcurrentQueue<String> receiveQueue { get; }
    public ConcurrentQueue<ArraySegment<byte>> sendQueue { get; }

    GameObject gameManagerObject;

    Thread receiveThread;
    Thread sendThread;


    public WsClient()
    {
      encoder = new UTF8Encoding();
      ws = new ClientWebSocket();
      gameManagerObject = GameObject.FindGameObjectWithTag("GameManager");

      // This long running WebSocket thread must be kept alive to receive server WebSocket messages.
      receiveQueue = new ConcurrentQueue<string>();
      receiveThread = new Thread(RunReceive);
      receiveThread.Start();

      sendQueue = new ConcurrentQueue<ArraySegment<byte>>();
      sendThread = new Thread(RunSend);
      sendThread.Start();
    }

    public bool isConnecting()
    {
      return ws.State == WebSocketState.Connecting;
    }

    public bool isOpen()
    {
      return ws.State == WebSocketState.Open;
    }

    public async Task Connect(Uri uri)
    {
      await ws.ConnectAsync(uri, CancellationToken.None);
      while (ws.State == WebSocketState.Connecting)
      {
        Task.Delay(50).Wait();
      }
    }

    public void Send(string message)
    {
      byte[] buffer = encoder.GetBytes(message);
      //Debug.Log("Message to queue for send: " + buffer.Length + ", message: " + message);
      var sendBuf = new ArraySegment<byte>(buffer);

      //Debug.Log("Send Message queue size: " + sendQueue.Count);
      sendQueue.Enqueue(sendBuf);
    }

    public async void RunSend()
    {
      ArraySegment<byte> msg;
      Debug.Log("RunSend entered.");
      while (true)
      {
        while (sendQueue.TryPeek(out msg))
        {
          sendQueue.TryDequeue(out msg);
          //Debug.Log("Dequeued this message to send: " + msg);
          await ws.SendAsync(msg, WebSocketMessageType.Text, true /* is last part of message */, CancellationToken.None);
        }
      }
    }

    public async void RunReceive()
    {
      Debug.Log("WebSocket Message Receiver looping.");
      string result;
      while (true) // TODO: ws.State seems to not like concurrent thread access.
      {
        result = await Receive();
        if (result != null && result.Length > 0)
        {
          receiveQueue.Enqueue(result);
          //Debug.Log("Added Message: " + result);
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