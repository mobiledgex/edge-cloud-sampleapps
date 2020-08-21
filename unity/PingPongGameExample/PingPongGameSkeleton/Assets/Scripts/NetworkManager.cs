﻿using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;
using DistributedMatchEngine;
using DistributedMatchEngine.PerformanceMetrics;
using MobiledgeX;
using System.Diagnostics;


namespace MobiledgeXPingPongGame
{
    public class NetworkManager : MonoBehaviour
    {

#region Local Testing Variables

        bool useAltServer = false; // set to true for local testing
        string host = "localhost";
        string altServerHost = "192.168.1.10"; // Local server hack. Override and set useAltServer=true for dev demo use.
        int port = 3000;
        string server = "";
        
#endregion
       
#region NetTest Variables

        string l7Path; // Layer 7 in the OSI Networking Model (Application Layer) 
        Stopwatch stopWatch = new Stopwatch();
        NetTest netTest = null;

#endregion

        MobiledgeXIntegration integration;
        public MobiledgeXWebSocketClient webSocketClient;
        public GameSession gameSession;
        public GameManager gameManager;
        bool isPaused = false;
        string queryParams = "";
        string edgeCloudletStr = ""; // Connection url to Edge 


#region MonoBehaviour Callbacks
        // Use this for initialization
        IEnumerator Start()
        {
            integration = new MobiledgeXIntegration();
            // Demo mode DME server to run MobiledgeX APIs, or if SIM card is missing
            // and a local DME cannot be located. Set to false if using a supported
            // SIM Card
            yield return new Exception("LocationService NOT IMPLEMENTED");
            MobiledgeXAPICalls();

            // Use local server, by IP. This must be started before use:
            if (useAltServer)
            {
                host = altServerHost;
            }

            server = "ws://" + host + ":" + port;

            webSocketClient = new MobiledgeXWebSocketClient();
            gameSession = new GameSession();
            gameSession.currentGs = new GameState();
            gameSession.status = STATUS.LOBBY;
        }
        void Update()
        {
            // Receive runs in a background filling the receive concurrent queue.
            if (webSocketClient == null)
            {
                return;
            }
            if(netTest != null) { 
                stopWatch.Start();
                // If Ping is running, print:
                if (netTest.runTest)
                {
                    long elapsed = (long)stopWatch.Elapsed.TotalMilliseconds;
                    if (elapsed > netTest.TestTimeoutMS)
                    {
                        stopWatch.Reset();
                        foreach (NetTest.Site s in netTest.sites)
                        {
                            gameManager.clog("Round trip to host: " + s.host + ", port: " + s.port + ", l7Path: " + s.L7Path +
                              ", average: " + s.average + ", stddev: " + s.stddev);
                            for (int i = 0; i < s.samples.Length; i++)
                            {
                                gameManager.clog("Samples: " + s.samples[i]);
                            }
                        }
                    }
                }
            }

            var cqueue = webSocketClient.receiveQueue;
            string msg;
            while (cqueue.TryPeek(out msg))
            {
                cqueue.TryDequeue(out msg);
                //gameManager.clog("Dequeued this message: " + msg);
                HandleMessage(msg);
            }

            if (gameSession.status == STATUS.JOINED)
            {
                // theBall.SendMessage("GoBall", null, SendMessageOptions.RequireReceiver);
            }

            if (gameSession.status == STATUS.INGAME)
            {
                // These puts messages into a send queue, sent via a thread.
                gameManager.UpdateBall();
                gameManager.UpdatePlayer();
            }
        }
        // TODO: Should manage the thread runnables.
        private void OnApplicationFocus(bool focus)
        {
            if (webSocketClient != null)
            {
                try
                {
                    if (integration != null && netTest != null)
                    {
                        netTest.doTest(focus);
                        gameManager.clog("NetTest focused run status: " + netTest.runTest);
                    }
                }
                catch (Exception e)
                {
                    gameManager.clog("Exception hit: " + e.Message);
                }
            }
        }
        // TODO: Should manage the thread runnables.
        void OnApplicationPause(bool pauseStatus)
        {
            isPaused = pauseStatus;
            if (webSocketClient != null)
            {
                if (integration != null && netTest != null)
                {
                    netTest.doTest(!isPaused);
                    gameManager.clog("NetTest pauseStatus: " + netTest.runTest);
                }
            }
        }
        private void OnDestroy()
        {
            webSocketClient.tokenSource.Cancel();
        }
#endregion

        public async void MobiledgeXAPICalls()
        {
            
            gameManager.clog("RegisterClient NOT IMPLEMENTED");
            return;

            gameManager.clog("FindCloudlet NOT IMPLEMENTED");
            return;

            gameManager.clog("GetAppPort NOT IMPLEMENTED");
            return;

            gameManager.clog("GetUrl NOT IMPLEMENTED");
            return;

            // NetTest
            netTest = new NetTest(integration.matchingEngine);
            foreach (AppPort ap in integration.FindCloudletReply.ports)
            {
                gameManager.clog("Port: proto: " + ap.proto + ", prefix: " + ap.fqdn_prefix + ", path_prefix: " + ap.path_prefix + ", port: " + ap.public_port);

                NetTest.Site site;
                // We're looking for one of the TCP app ports:
                if (ap.proto == LProto.L_PROTO_TCP)
                {
                    // Add to test targets.
                    if (ap.path_prefix == "")
                    {
                        site = new NetTest.Site
                        {
                            host = integration.GetHost(ap),
                            port = integration.GetPort(ap)
                        };
                        site.testType = NetTest.TestType.CONNECT;
                    }
                    else
                    {
                        site = new NetTest.Site
                        {
                            L7Path = integration.GetUrl("", ap)
                        };
                        site.testType = NetTest.TestType.CONNECT;
                    }
                    if (useAltServer)
                    {
                        site.host = host;
                    }
                    l7Path = site.L7Path;
                    netTest.sites.Enqueue(site);
                }
            }
            netTest.doTest(true);
        }

        // This method is called when the user has finished editing the Room ID InputField.
        public async void ConnectToServerWithRoomId(string roomId)
        {
            Uri edgeCloudletUri;

            if (roomId == "")
            {
                gameManager.clog("You must enter a room ID. Please try again.");
                return;
            }

            gameManager.clog("Connecting to WebSocket Server with roomId=" + roomId + "...");
            gameManager.clog("useAltServer=" + useAltServer + " host=" + host + " edgeCloudletStr=" + edgeCloudletStr);
            queryParams = "?roomid=" + roomId;

            if (webSocketClient.isOpen())
            {
                webSocketClient.Dispose();
                webSocketClient = new MobiledgeXWebSocketClient();
            }

            if (useAltServer)
            {
                server = "ws://" + host + ":" + port;
                edgeCloudletUri = new Uri(server + queryParams);
                await webSocketClient.Connect(edgeCloudletUri);
            }
            else
            {
                try
                {
                    edgeCloudletUri = new Uri(edgeCloudletStr + queryParams);
                    await webSocketClient.Connect(edgeCloudletUri);
                }
                catch (Exception e)
                {
                    gameManager.clog("Unable to get websocket connection. Exception: " + e.Message + ". Switching to AltServer.");
                    useAltServer = true;
                    ConnectToServerWithRoomId(roomId);
                    return;
                }
            }
            gameManager.clog("Connection to status: " + webSocketClient.isOpen());
        }

        // Match whatever WebSocket text is sending
        // Consistency: General rule here is that the game state if not timestamped, events may not represent the same time window.
        void HandleMessage(string message)
        {
            var msg = MessageWrapper.UnWrapMessage(message);
            // Not quite symetric, but the server is text only.
            switch (msg.type)
            {
                case "qotd":
                    break;
                case "notification":
                    Notification notification = Messaging<Notification>.Deserialize(message);
                    gameManager.clog(notification.notificationText);
                    break;
                case "register":
                    GameRegister register = Messaging<GameRegister>.Deserialize(message);
                    gameSession.sessionId = register.sessionId;
                    gameSession.uuidPlayer = register.uuidPlayer;
                    break;
                case "gameJoin":
                    GameJoin gj = Messaging<GameJoin>.Deserialize(message);
                    gameManager.JoinGame(gj);
                    break;
                case "scoreEvent":
                    ScoreEvent se = Messaging<ScoreEvent>.Deserialize(message);
                    gameManager.UpdateScore(se);
                    break;
                case "moveEvent":
                    MoveEvent me = Messaging<MoveEvent>.Deserialize(message);
                    gameManager.UpdatePosition(me);
                    break;
                case "gameState":
                    GameState serverGs = Messaging<GameState>.Deserialize(message);
                    gameSession.currentGs.sequence = serverGs.sequence;
                    //UpdateLocalGame(serverGs);
                    break;
                case "contactEvent":
                    ContactEvent ce = Messaging<ContactEvent>.Deserialize(message);
                    gameManager.HandleContactEvent(ce);
                    break;
                case "resign":
                    GameResign resign = Messaging<GameResign>.Deserialize(message);
                    gameManager.theBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
                    gameManager.ghostBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
                    break;
                case "nextRound":
                    NextRound nr = Messaging<NextRound>.Deserialize(message);
                    gameManager.StartNextRound(nr);
                    break;
                case "gameRestart":
                    GameRestart gr = Messaging<GameRestart>.Deserialize(message);
                    gameManager.RestartGame(gr);
                    break;

                default:
                    gameManager.clog("Unknown message arrived: " + msg.type + ", message: " + message);
                    break;
            }

        }
    }

    public enum STATUS
    {
        LOBBY = 0,
        JOINED,
        INGAME,
        NEXTROUND,
        LOST,
        WON,
        RESTART
    }
    public class GameSession
    {
        public string sessionId;
        public string uuidPlayer;
        public string gameId;

        public int side;
        public string uuidOtherPlayer;
        public string lastUuidPing = null;

        public NetworkManager networkManager;
        public GameState currentGs;

        public STATUS status;

    }
}
