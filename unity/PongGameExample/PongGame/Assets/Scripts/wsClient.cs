using System.Collections;
using System.Collections.Generic;
using UnityEngine;

using System;
using System.IO;
using System.Linq;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

public class wsClient : MonoBehaviour
{
    // Life of wsClient:
    ClientWebSocket ws = new ClientWebSocket();
    static UTF8Encoding encoder; // For websocket text message encoding.
    const UInt64 MAXREADSIZE = 1*1024*1024;

    // Data
    uint count = 0;

    // Start is called before the first frame update
    async void Start()
    {
        if (ws != null)
        {
            // Connect and wait until WebSocketState.CONNECTING becomes OPEN
            Debug.Log("Doing Connect");
            Connect("ws://localhost:8080");
           
            Debug.Log("Connect called.");
        }
        encoder = new UTF8Encoding();

        doStuff();

    }

    // Update is called once per frame, and no loop.
    async void Update()
    {
        doStuff();
    }

    async Task doStuff()
    {
        Debug.Log("doStuff");
        string message = "{\"message\": " + count + "}";
        await Send(ws, message);

        Debug.Log("Send done");

        var receivedMessage = await Recieve(ws);

        Debug.Log("Server says to Client: " + receivedMessage);
    }

    async Task Connect(string uri)
    {
        Task connectTask = ws.ConnectAsync(new Uri(uri), CancellationToken.None);
        while (ws.State == WebSocketState.Connecting)
        {
            connectTask.Wait(20); // in milliseconds
        }
    }

    async static Task Send(ClientWebSocket webSocket, string message)
    {
        byte[] buffer = encoder.GetBytes(message);
        Debug.Log("Message to send: " + buffer.Length);
        await webSocket.SendAsync(new ArraySegment<byte>(buffer), WebSocketMessageType.Text, true /* is last part of message */, CancellationToken.None);
        Debug.Log("Send done");
    }

    async static Task<string> Recieve(ClientWebSocket ws, UInt64 maxSize = MAXREADSIZE)
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
