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
using System.Collections.Generic;
using System.Runtime.Serialization.Json;
using System.IO;
using UnityEngine;
using UnityEngine.UI;

using System.Net.WebSockets;
using System;
using System.Threading.Tasks;


using DistributedMatchEngine;

namespace MexPongGame {

  enum STATUS
  {
    LOBBY = 0,
    JOINED,
    INGAME,
    NEXTROUND,
    LOST,
    WON,
    RESTART
  }

  class GameSession
  {
    public string sessionId;
    public string uuidPlayer;
    public string gameId;

    public int side;
    public string uuidOtherPlayer;
    public string lastUuidPing = null;

    public GameState currentGs;

    public STATUS status;

  }
  public class GameManager : MonoBehaviour
  {
    public static int PlayerScore1 = 0;
    public static int PlayerScore2 = 0;

    public GUISkin layout;

    GameObject theBall;
    GameObject[] players;
    GameObject ghostBall; // Just one.
    GameObject ghostPlayer; // Local player.
    WsClient client;

    GameSession gameSession = new GameSession();

    /**
     * MobiledgeX Integration: thin example encapsulation outside Pong for ease
     * of viewing.
     */
    MobiledgeXIntegration integration = new MobiledgeXIntegration();

    string host = "localhost";
    string serverHost = "192.168.1.10"; // Local server hack. Override and set useAltServer=true for dev demo use.
    int port = 3000;
    string server = "";
    string queryParams = "";
    string edgeCloudletStr = "";

    bool isPaused = false;

    bool useAltServer = false;
    NetTest netTest = new NetTest();

    GameObject uiG;
    public Text uiConsole;
    public InputField roomIdInput;

    // Use this for initialization
    async Task Start()
    {
      // Demo mode DME server to run MobiledgeX APIs, or if SIM card is missing
      // and a local DME cannot be located. Set to false if using a supported
      // Carrier.
      integration.useDemo = false;

      // Use local server, by IP. This must be started before use:
      if (useAltServer)
      {
        host = serverHost;
      }

      server = "ws://" + host + ":" + port;
      theBall = GameObject.FindGameObjectWithTag("Ball");
      players = GameObject.FindGameObjectsWithTag("Player");
      client = new WsClient();
      gameSession.currentGs = new GameState();
      gameSession.status = STATUS.LOBBY;

      // Create a Mex Paddle (for local user) from the Prefab:
      ghostPlayer = (GameObject)Instantiate(Resources.Load("PaddleGhost"));
      ghostBall = (GameObject)Instantiate(Resources.Load("BallGhost"));

      uiG = GameObject.FindGameObjectWithTag("UIConsole");
      uiConsole = uiG.GetComponent<Text>();

      // Attach a listener to the Room ID input field.
      roomIdInput = GameObject.Find("InputFieldRoomId").GetComponent<InputField>();
      roomIdInput.onEndEdit.AddListener(ConnectToServerWithRoomId);

      // Register and find cloudlet:
      uiConsole.text = "Registering to DME: ";
      edgeCloudletStr = await RegisterAndFindCloudlet();
      clog("Found Cloudlet from DME result: [" + edgeCloudletStr + "]");

      // This might be inside the update loop. Re-register client and check periodically.
      bool verifiedLocation = await integration.VerifyLocation();

      // Decide what to do with location status.
      clog("VerifiedLocation: " + verifiedLocation);

      // QosPositions of various gps locations
      QosPositionKpiStream qosReplyStream = await integration.GetQosPositionKpi();
      if (qosReplyStream == null)
      {
        clog("Reply Stream result missing: " + qosReplyStream);
      }
      else
      {
        foreach (var qosPositionKpiReply in qosReplyStream)
        {
          // Serialize the DataContract and print everything:
          DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(QosPositionKpiReply));
          MemoryStream ms = new MemoryStream();
          serializer.WriteObject(ms, qosPositionKpiReply);
          string jsonStr = Util.StreamToString(ms);
          clog("QoS of requested gps location(s): " + jsonStr);
        }
        qosReplyStream.Dispose();
      }
    }

    // This method is called when the user has finished editing the Room ID InputField.
    async void ConnectToServerWithRoomId(string roomId)
    {
      if(roomId == "")
      {
        clog("You must enter a room ID. Please try again.");
        return;
      }
      Uri edgeCloudletUri = null;
      clog("Connecting to WebSocket Server with roomId="+roomId+"...");
      clog("useAltServer=" + useAltServer + " host="+host+" edgeCloudletStr="+edgeCloudletStr);
      queryParams = "?roomid="+roomId;
      if (useAltServer)
      {
        server = "ws://" + host + ":" + port;
        edgeCloudletUri = new Uri(server + queryParams);
        await client.Connect(edgeCloudletUri);
        netTest.sites.Enqueue(new NetTest.HostAndPort{host=host, port=port});
      }
      else
      {
        if(edgeCloudletStr == "")
        {
          clog("No edgeCloudletUri received yet. Please try again.");
          return;
        }
        edgeCloudletUri = new Uri("ws://" + edgeCloudletStr + queryParams);
        await client.Connect(edgeCloudletUri);
      }
      clog("Connection to " + edgeCloudletUri + " status: " + client.isOpen());
    }

    void Update()
    {
      // Receive runs in a background filling the receive concurrent queue.
      if (client == null)
      {
        return;
      }
      var cqueue = client.receiveQueue;
      string msg;
      while (cqueue.TryPeek(out msg))
      {
        cqueue.TryDequeue(out msg);
        //Debug.Log("Dequeued this message: " + msg);
        HandleMessage(msg);
      }

      if (gameSession.status == STATUS.JOINED)
      {
        //theBall.SendMessage("GoBall", null, SendMessageOptions.RequireReceiver);
      }

      if (gameSession.status == STATUS.INGAME)
      {
        // These puts messages into a send queue, sent via a thread.
        UpdateBall();
        UpdatePlayer();
      }

    }

    void clog(string msg)
    {
      uiConsole.text = msg;
      Debug.Log(msg);
    }

    // TODO: Should manage the thread runnables.
    private void OnApplicationFocus(bool focus)
    {
      if (client != null)
      {
        netTest.doPing(focus);
      }
    }
    // TODO: Should manage the thread runnables.
    void OnApplicationPause(bool pauseStatus)
    {
      isPaused = pauseStatus;
      if (client != null)
      {
        netTest.doPing(!isPaused);
      }

    }

    // Start() is a time to do this, but can change if the device moves to a new location.
    async Task<string> RegisterAndFindCloudlet()
    {
      // For Demo App purposes, it's the TCP app port. Your app may point somewhere else:
      string tcpAppPort = "";
      NetTest.HostAndPort hostAndPort = null;

      string aCarrierName = integration.GetCarrierName();
      clog("aCarrierName: " + aCarrierName);


      clog("Calling DME to register client...");
      bool registered = await integration.Register();

      if (registered)
      {
        FindCloudletReply reply;
        clog("Finding Cloudlet...");
        reply = await integration.FindCloudlet();


        // Handle reply status:
        bool found = false;
        if (reply == null)
        {
          clog("FindCloudlet call failed.");
          return "";
        }

        switch (reply.status)
        {
          case FindCloudletReply.FindStatus.FIND_UNKNOWN:
            clog("FindCloudlet status unknown. No edge cloudlets.");
            break;
          case FindCloudletReply.FindStatus.FIND_NOTFOUND:
            clog("FindCloudlet Found no edge cloudlets in range.");
            break;
          case FindCloudletReply.FindStatus.FIND_FOUND:
            found = true;
            break;

        }

        if (found)
        {
          // Edge cloudlets found!
          clog("Edge cloudlets found!");

          // Where is this app specific edge enabled cloud server:
          clog("GPS location: longitude: " + reply.cloudlet_location.longitude + ", latitude: " + reply.cloudlet_location.latitude);

          // Where is the URI for this app specific edge enabled cloud server:
          clog("fqdn: " + reply.fqdn);
          // AppPorts?
          Debug.Log("On ports: ");

          foreach (AppPort ap in reply.ports)
          {
            clog("Port: proto: " + ap.proto + ", prefix: " +
                ap.fqdn_prefix + ", path_prefix: " + ap.path_prefix + ", port: " +
                ap.public_port);

            // We're looking for one of the TCP app ports:
            if (ap.proto == LProto.L_PROTO_TCP)
            {
              tcpAppPort = reply.fqdn + ":" + ap.public_port;
              // FQDN prefix to append to base FQDN in FindCloudlet response. May be empty.
              if (ap.fqdn_prefix != "")
              {
                tcpAppPort = ap.fqdn_prefix + tcpAppPort;
              }

              // Add to test targets.
              hostAndPort = new NetTest.HostAndPort {  host = ap.fqdn_prefix + reply.fqdn, port = ap.public_port };
              netTest.sites.Enqueue(hostAndPort);
            }

          }

        }
        clog("FindCloudlet found: [" + tcpAppPort + "]");
      }

      return tcpAppPort;
    }

    public void Score(string wallID)
    {

      if (wallID == "RightWall")
      {
        PlayerScore1++;
      }
      else // Score for player 2.
      {
        PlayerScore2++;
      }

      // Let the star topology server know about this event!
      ScoreEvent scoreEvent = new ScoreEvent
      {
        uuid = gameSession.uuidPlayer,
        gameId = gameSession.gameId,
        side = gameSession.side,
        playerScore1 = PlayerScore1,
        playerScore2 = PlayerScore2
      };

      client.Send(Messaging<ScoreEvent>.Serialize(scoreEvent));
    }

    public void UpdateBall()
    {
      var bc = theBall.GetComponent<BallControl>();
      Ball ball = Ball.CopyBall(bc);

      gameSession.currentGs.balls[0] = ball;

      MoveEvent moveEvent = new MoveEvent
      {
        uuid = bc.uuid,
        playeruuid = gameSession.uuidPlayer,
        gameId = gameSession.gameId,
        objectType = "Ball",
        position = new Position(ball.position),
        velocity = new Velocity(ball.velocity)
      };

      client.Send(Messaging<MoveEvent>.Serialize(moveEvent));
    }

    public void UpdatePlayer()
    {
      // Client side dict needed.
      Player[] gsPlayers = new Player[gameSession.currentGs.players.Length];
      Player selected = null;
      // Only ever need to tell the server of own location (for now)

      int idx = 0;
      foreach (GameObject gpc in players)
      {
        PlayerControls pc = gpc.GetComponent<PlayerControls>();
        if (pc.ownPlayer)
        {
          selected = Player.CopyPlayer(pc);
          gsPlayers[idx++] = selected;
        }
        else
        { // Stright copy and update.
          gsPlayers[idx++] = Player.CopyPlayer(pc);
        }
      }
      gameSession.currentGs.players = gsPlayers;

      MoveEvent moveEvent = new MoveEvent
      {
        uuid = selected.uuid,
        playeruuid = selected.uuid,
        gameId = gameSession.gameId,
        objectType = "Player",
        position = new Position(selected.position),
        velocity = new Velocity(selected.velocity)
      };

      client.Send(Messaging<MoveEvent>.Serialize(moveEvent));
    }

    public void SendContactEvent(PlayerControls c, BallControl b, Collision2D collision)
    {
      // If the contact is actually the other player
      ContactEvent ce = new ContactEvent
      {
        sequence = gameSession.currentGs.sequence,
        objectType = "Ball",
        uuid = c.uuid,
        playeruuid = gameSession.uuidPlayer, // Sender source of this event.
        gameId = gameSession.gameId,
        position = new Position(b.rb2d.position),
        velocity = new Velocity(b.rb2d.velocity)
      };
      client.Send(Messaging<ContactEvent>.Serialize(ce));
    }

    void SendRestart()
    {
      GameRestart gr = new GameRestart();
      gr.gameId = gameSession.gameId;
      gr.balls = new Ball[1];
      gr.balls[0] = Ball.CopyBall(theBall.GetComponent<BallControl>());
      client.Send(Messaging<GameRestart>.Serialize(gr));
    }

    // Separate from Update()
    void OnGUI()
    {
      GUI.skin = layout;
      GUI.Label(new Rect(Screen.width / 2 - 150 - 12, 20, 100, 100), "" + PlayerScore1);
      GUI.Label(new Rect(Screen.width / 2 + 150 + 12, 20, 100, 100), "" + PlayerScore2);

      if (GUI.Button(new Rect(Screen.width / 2 - 60, 35, 120, 53), "RESTART"))
      {
        PlayerScore1 = 0;
        PlayerScore2 = 0;
        if (theBall != null)
        {
          theBall.SendMessage("RestartGame", 0.5f, SendMessageOptions.RequireReceiver);
        }
        SendRestart();
      }

      if (PlayerScore1 == 10)
      {
        GUI.Label(new Rect(Screen.width / 2 - 150, 200, 2000, 1000), "PLAYER ONE WINS");
        if (theBall != null)
        {
          theBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
        }
      }
      else if (PlayerScore2 == 10)
      {
        GUI.Label(new Rect(Screen.width / 2 - 150, 200, 2000, 1000), "PLAYER TWO WINS");
        if (theBall != null)
        {
          theBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
        }
      }
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
          clog(notification.notificationText);
          break;
        case "register":
          GameRegister register = Messaging<GameRegister>.Deserialize(message);
          gameSession.sessionId = register.sessionId;
          gameSession.uuidPlayer = register.uuidPlayer;
          break;
        case "gameJoin":
          GameJoin gj = Messaging<GameJoin>.Deserialize(message);
          JoinGame(gj);
          break;
        case "scoreEvent":
          ScoreEvent se = Messaging<ScoreEvent>.Deserialize(message);
          UpdateScore(se);
          break;
        case "moveEvent":
          MoveEvent me = Messaging<MoveEvent>.Deserialize(message);
          UpdatePosition(me);
          break;
        case "gameState":
          GameState serverGs = Messaging<GameState>.Deserialize(message);
          gameSession.currentGs.sequence = serverGs.sequence;
          //UpdateLocalGame(serverGs);
          break;
        case "contactEvent":
          ContactEvent ce = Messaging<ContactEvent>.Deserialize(message);
          HandleContactEvent(ce);
          break;
        case "resign":
          GameResign resign = Messaging<GameResign>.Deserialize(message);
          theBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
          ghostBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
          break;
        case "nextRound":
          NextRound nr = Messaging<NextRound>.Deserialize(message);
          StartNextRound(nr);
          break;
        case "gameRestart":
          GameRestart gr = Messaging<GameRestart>.Deserialize(message);
          RestartGame(gr);
          break;

        default:
          Debug.Log("Unknown message arrived: " + msg.type + ", message: " + message);
          break;
      }

    }

    bool UpdateScore(ScoreEvent se)
    {
      // Policy: Server rules:
      PlayerScore1 = se.playerScore1;
      PlayerScore2 = se.playerScore2;

      return true;
    }

    bool UpdatePosition(MoveEvent moveItem)
    {
      //Debug.Log("moveItem: " + moveItem.uuid);
      var gs = gameSession.currentGs;

      if (moveItem.sequence < gameSession.currentGs.sequence)
      {
        clog("old event.");
        return false; // Old.
      }

      if (moveItem.uuid == gs.balls[0].uuid)
      {
        // If the source is the other player, and that's the last contact,
        // update local ball.
        if (gameSession.lastUuidPing == gameSession.uuidOtherPlayer &&
            moveItem.playeruuid == gameSession.uuidOtherPlayer)
        {
          // Other player...
          var bc = theBall.GetComponent<BallControl>();

          gs.balls[0].position = moveItem.position;
          gs.balls[0].velocity = moveItem.velocity;
          bc.setPosition(gs.balls[0].position);
          bc.setVelocity(gs.balls[0].velocity);

        }
        else // Self echo.
        {
          UpdateBallGhost(moveItem);
        }
      }

      if (moveItem.uuid == gameSession.uuidPlayer)
      {
        UpdatePlayerGhost(moveItem);
        // Server echo of current player position and velocity.
        // Add a gameObject if not existing, and show it along with the current player's position.
        // Also, if significantly different, jump player to "server" position, or interpolate postion over time.
      }
      else if (moveItem.uuid == gameSession.uuidOtherPlayer) // Other player, blind copy.
      {
        foreach (var player in gs.players)
        {
          if (player.uuid == gameSession.uuidOtherPlayer)
          {
            player.position = moveItem.position;
            player.velocity = moveItem.velocity;
            // Apply to GameObject of player:
            var gp = GameObject.FindGameObjectsWithTag("Player");
            foreach (var g in gp)
            {
              var p = g.GetComponent<PlayerControls>();
              if (p.uuid == gameSession.uuidOtherPlayer)
              {
                p.setPosition(moveItem.position);
                p.setVelocity(moveItem.velocity);
              }
            }
          }
        }
      }


      return true;
    }

    bool UpdateBallGhost(MoveEvent moveItem)
    {
      if (moveItem.objectType == "Ball")
      {
        BallControl bc = ghostBall.GetComponent<BallControl>();
        if (bc != null)
        {
          bc.setPosition(moveItem.position);
          bc.setVelocity(moveItem.velocity);
        }
      }
      return true;
    }
    bool UpdatePlayerGhost(MoveEvent moveItem)
    {
      if (moveItem.objectType == "Player" &&
          moveItem.uuid == gameSession.uuidPlayer)
      {
        // Ghost is a variant of the regular player paddle.
        // There's just one pre-assigned ghost.
        PlayerControls pc = ghostPlayer.GetComponent<PlayerControls>();
        if (pc != null)
        {
          pc.setPosition(moveItem.position);
          pc.setVelocity(moveItem.velocity);
        }

      }
      return true;
    }

    bool HandleContactEvent(ContactEvent ce)
    {
      // This is an event everyone should (try) to agree on, even if the simulation diverges.
      // 1) It's the latest event.
      // 2) The other player has already observed this on their game simulation.
      BallControl bc = theBall.GetComponent<BallControl>();

      if (bc.uuid != gameSession.currentGs.balls[0].uuid)
      {
        clog("Ball UUID is unknown! Contact event unknown.");
        return false;
      }

      if (ce.playeruuid == gameSession.uuidOtherPlayer)
      {
        clog("Matching local ball to remote player event: " + ce.playeruuid);
        bc.setPosition(ce.position);
        bc.setVelocity(ce.velocity);
        gameSession.lastUuidPing = gameSession.uuidOtherPlayer;
      }
      else if(ce.playeruuid == gameSession.uuidPlayer)
      {
        clog("Updating ghost ball (once) to server version: " + ce.velocity);
        // Self echo, just update server ghost.
        var gbc = ghostBall.GetComponent<BallControl>();
        gbc.setPosition(ce.position);
        gbc.setVelocity(ce.velocity);
        gameSession.lastUuidPing = gameSession.uuidPlayer;
      }

      return true;
    }

    bool ApplyGameState(GameState gameState)
    {
      // Instantiate other player, inspect, and then apply game state.
      return true;
    }

    // Not differential, but this is small. Bespoke. TODO: GRPC
    GameState GatherGameState()
    {
      GameState gameState = new GameState();

      gameState.gameId = gameSession.gameId; // Keep gameId.
      gameState.score1 = PlayerScore1;
      gameState.score2 = PlayerScore2;

      // copy Ball(s):
      GameObject[] bls = GameObject.FindGameObjectsWithTag("Ball");
      if (bls.Length > 0)
      {
        gameState.balls = new Ball[bls.Length];
        for (uint i = 0; i < bls.Length; i++)
        {
          BallControl bc = bls[i].GetComponent<BallControl>();
          gameState.balls[i] = Ball.CopyBall(bc);
        }
      }

      // copy Player(s)
      if (players.Length > 0)
      {
        gameState.players = new Player[players.Length];
        for (uint i = 0; i < bls.Length; i++)
        {
          PlayerControls pc = players[i].GetComponent<PlayerControls>();
          gameState.players[i] = Player.CopyPlayer(pc);
        }
      }

      return gameState;
    }

    void UpdateServer()
    {
      GameState gameState = GatherGameState();
      gameState.type = "gameState";
      gameState.source = "client";

      string jsonStr = Messaging<GameState>.Serialize(gameState);
      Debug.Log("UpdateServer: " + jsonStr);
      MessageWrapper wrapped = MessageWrapper.WrapTextMessage(jsonStr);
      client.Send(Messaging<MessageWrapper>.Serialize(wrapped));

      gameSession.currentGs = gameState;

      return;
    }

    bool PositionInRange(Position p1, Position p2, float error = 1.5f)
    {
      // Very basic check.
      if ((Math.Abs(p1.x - p2.x) < error) &&
          (Math.Abs(p1.y - p2.y) < error))
      {
        // It's "fair"
        return true;
      }
      return false;
    }

    bool VelocityInRange(Velocity p1, Velocity p2, float error = 20.0f)
    {
      // Very basic check.
      if ((Math.Abs(p1.x - p2.x) < error) &&
          (Math.Abs(p1.y - p2.y) < error))
      {
        // It's "fair"
        return true;
      }
      return false;
    }

    void UpdateLocalGame(GameState serverGameState)
    {

      if (serverGameState.sequence == 0)
      {
        gameSession.status = STATUS.INGAME;
      }

      GameState localGs = GatherGameState();

      // Resolve server versus local score:
      localGs.score1 = serverGameState.score1;
      localGs.score2 = serverGameState.score2;

      // Ball, just assume ball is fair, if close to server.
      BallControl bc = theBall.GetComponent<BallControl>();
      Ball ball = Ball.CopyBall(bc);
      /// Grab the first one...
      Ball serverBall = null;
      if (serverGameState.balls.Length > 0)
      {
        serverBall = serverGameState.balls[0];
        bool ballPositionOK = PositionInRange(ball.position, serverBall.position);
        bool ballVelocityOK = VelocityInRange(ball.velocity, serverBall.velocity);

        if (!ballPositionOK || !ballVelocityOK)
        {
          // Blindly use server's ball position and velocity to resync. Better: Blend and rubber band.
          bc.setPosition(serverBall.position);
          bc.setVelocity(serverBall.velocity);
        }
      } else {
        // Perhaps a new game. No server info.

      }

      // Copy other paddle location(s) from server. Current player knows their own position.
      // TODO: need a map/ordered list.
      int cpIdx = -1; // current player
      int opIdx = -1; // other player
      Player[] serverPlayers = serverGameState.players;

      GameObject[] pcs = GameObject.FindGameObjectsWithTag("Player");
      foreach(GameObject p in pcs)
      {
        PlayerControls a = p.GetComponent<PlayerControls>();
        if (a.uuid == gameSession.uuidOtherPlayer)
        {
          // Find other player in server view:
          for (var i = 0; i < serverGameState.players.Length; i++)
          {
            if (a.uuid == serverPlayers[i].uuid)
            {
              a.setPosition(serverPlayers[i].position);
              if (a.uuid == gameSession.uuidPlayer)
              {
                cpIdx = i;
              }
              else if (a.uuid == gameSession.uuidOtherPlayer)
              {
                opIdx = i;
              }
            }
          }
        }
      }


      // Player ghost. The position of where the server *thinks* the current player is.



      // Copy server score.
      // Merge/tweak ball position and velocity, we only care the ball state is fair.
      //   - Player position is ultimately sort of cosmetic.
      // Save to current GS.

      // Next update, gather and send that to server.

      return;
    }

    bool JoinGame(GameJoin gj)
    {
      clog("Told to join gameId: " + gj.gameId + ", side: " + gj.side);
      if (gj.gameId == "")
      {
        clog("Bad gameId!");
        return false;
      }

      gameSession.gameId = gj.gameId;
      gameSession.side = gj.side;
      gameSession.uuidOtherPlayer = gj.uuidOtherPlayer;
      gameSession.status = STATUS.JOINED;

      gameSession.currentGs = new GameState();
      var gs = gameSession.currentGs;

      gs.currentPlayer = gameSession.uuidPlayer;

      // Update Ball:
      if (gs.balls.Length == 0)
      {
        gs.balls = new Ball[1];
      }
      BallControl bc = theBall.GetComponent<BallControl>();
      bc.uuid = gj.ballId; // Add uuid to ball.
      gs.balls[0] = Ball.CopyBall(bc);

      // Match Assignments:
      // Given a side, 0 one is player one (left). 1 other player (right)
      PlayerControls left = null;
      PlayerControls right = null;

      if (players.Length != 2)
      {
        return false; // Can't join this game.
      }

      foreach (GameObject g in players)
      {
        if (left == null)
        {
          left = g.GetComponent<PlayerControls>();
          continue;
        }

        right = g.GetComponent<PlayerControls>();
        if (right.transform.position.x < left.transform.position.x)
        {
          var tmp = left;
          left = right;
          right = tmp;
        }
      }

      Debug.Log("Left sel: " + left.transform.position.x + "Right other: " + right.transform.position.x);

      if (gameSession.side == 0)
      {
        left.uuid = gameSession.uuidPlayer; // Player 1 assigned in match by server.
        left.ownPlayer = true;

        right.uuid = gameSession.uuidOtherPlayer;
        right.ownPlayer = false;
        gs.players[0] = Player.CopyPlayer(left);
        gs.players[1] = Player.CopyPlayer(right);
      }
      else if (gameSession.side == 1)
      {

        right.uuid = gameSession.uuidPlayer; // Player 2 assigned in match by server.
        right.ownPlayer = true;

        left.uuid = gameSession.uuidOtherPlayer;
        left.ownPlayer = false;

        gs.players[0] = Player.CopyPlayer(right);
        gs.players[1] = Player.CopyPlayer(left);
      }

      // Assign player state:
      gameSession.currentGs = gs;

      clog("Transition to inGame.");
      gameSession.status = STATUS.INGAME;
      return true;
    }

    bool StartNextRound(NextRound nr)
    {
      clog("Start next round for game, gameId: " + nr.gameId);
      if (nr.gameId != gameSession.gameId)
      {
        return false;
      }
      gameSession.lastUuidPing = null;
      gameSession.status = STATUS.INGAME;

      // The ball position and velocity is chosen at random by server, and sent out to players.
      // For now, just trust it.
      BallControl bc = theBall.GetComponent<BallControl>();
      bc.setPosition(new Position(Vector2.zero));
      bc.setVelocity(new Velocity(Vector2.zero));
      // Not correct. FIXME:
      Vector2 startingForce = new Vector2(nr.balls[0].velocity.x, nr.balls[0].velocity.y);
      // Not deterministic?
      //bc.rb2d.AddForce(startingForce);
      bc.setVelocity(nr.balls[0].velocity);

      return false;
    }

    bool RestartGame(GameRestart gr)
    {
      clog("Restarting game, gameId: " + gr.gameId);
      if (gr.gameId != gameSession.gameId)
      {
        return false;
      }
      gameSession.status = STATUS.RESTART;

      //theBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
      PlayerScore1 = 0;
      PlayerScore2 = 0;

      gameSession.currentGs.sequence = 0;
      gameSession.status = STATUS.INGAME;

      // The ball position and velocity is chosen at random by server, and sent out to players.
      // For now, just trust it.
      BallControl bc = theBall.GetComponent<BallControl>();
      bc.setPosition(new Position(Vector2.zero));
      bc.setVelocity(new Velocity(Vector2.zero));
      // Not correct. FIXME:
      Vector2 startingForce = new Vector2(gr.balls[0].velocity.x, gr.balls[0].velocity.y);

      // Not deterministic?
      //bc.rb2d.AddForce(startingForce);

      bc.setVelocity(gr.balls[0].velocity);

      return false;
    }



  }

}
