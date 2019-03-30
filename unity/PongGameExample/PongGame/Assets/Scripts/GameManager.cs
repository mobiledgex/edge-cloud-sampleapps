using System.Collections;
using System.Collections.Generic;
using UnityEngine;

using System.Net.WebSockets;
using System;
using System.Threading;
using System.Threading.Tasks;


namespace MexPongGame {

  enum STATUS
  {
    LOBBY = 0,
    JOINED,
    INGAME,
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

    public GameState currentGs;

    public STATUS status;

  }
  public class GameManager : MonoBehaviour
  {
    public static int PlayerScore1 = 0;
    public static int PlayerScore2 = 0;

    Dictionary<string, int> scoreDictionary = new Dictionary<string, int>();
    Dictionary<string, WeakReference<PlayerControls>> _playercache;
    Dictionary<string, WeakReference<BallControl>> _ballcache;

    public GUISkin layout;

    GameObject theBall;
    WsClient client;

    GameSession gameSession = new GameSession();




    string server = "ws://localhost:3000";

    private Uri uri = new Uri("ws://localhost:3000");
    private ClientWebSocket ws = new ClientWebSocket();

    Task openTask;
    Task<string> receiveTask;

    // Use this for initialization
    async void Start()
    {

      theBall = GameObject.FindGameObjectWithTag("Ball");
      client = new WsClient();
      gameSession.status = STATUS.LOBBY;

      await client.Connect(uri);
    }
    

    void Update()
    {
      // Receive runs in a background filling the receive concurrent queue.
      var cqueue = client.receiveQueue;
      string msg;
      while(cqueue.TryPeek(out msg))
      {
        cqueue.TryDequeue(out msg);
        Debug.Log("Dequeued this message: " + msg);
        HandleMessage(msg);
      }

    }

    public static void Score(string wallID)
    {
      if (wallID == "RightWall")
      {
        PlayerScore1++;
      }
      else
      {
        PlayerScore2++;
      }
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
      GameObject[] pcs = GameObject.FindGameObjectsWithTag("Player");
      if (pcs.Length > 0)
      {
        gameState.players = new Player[pcs.Length];
        for (uint i = 0; i < bls.Length; i++)
        {
          PlayerControls pc = pcs[i].GetComponent<PlayerControls>();
          gameState.players[i] = Player.CopyPlayer(pc);
        }
      }

      return gameState;
    }

    async void UpdateServer()
    {
      GameState gameState = GatherGameState();
      gameState.type = "gameState";
      gameState.source = "client";

      string jsonStr = Messaging<GameState>.Serialize(gameState);
      Debug.Log("UpdateServer: " + jsonStr);
      MessageWrapper wrapped = MessageWrapper.WrapTextMessage(jsonStr);
      await client.Send(Messaging<MessageWrapper>.Serialize(wrapped));

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
      float error = 1f;
      BallControl bc = theBall.GetComponent<BallControl>();
      Ball ball = Ball.CopyBall(bc);
      Ball serverBall = serverGameState.balls[0];


      bool ballPositionOK = PositionInRange(ball.position, serverBall.position);
      bool ballVelocityOK = VelocityInRange(ball.velocity, serverBall.velocity);

      if (ballPositionOK && ballVelocityOK)
      {
        // do nothing.
      }
      else // Blindly use server's ball position and velocity to resync. Better: Blend and rubber band.
      {
        Vector2 newPos;
        newPos.x = serverBall.position.x;
        newPos.y = serverBall.position.y;
        bc.rb2d.position = newPos;
      }

      // Copy other paddle location(s) from server. Current player knows their own position.
      // TODO: need a map/ordered list.
      int cpIdx = -1; // current player
      int opIdx = -1; // other player
      Player[] players = serverGameState.players;

      GameObject[] pcs = GameObject.FindGameObjectsWithTag("Player");
      foreach(GameObject p in pcs)
      {
        PlayerControls a = p.GetComponent<PlayerControls>();
        if (a.uuid == gameSession.uuidOtherPlayer)
        {
          // Find other player in server view:
          for (var i = 0; i < serverGameState.players.Length; i++)
          {
            if (a.uuid == players[i].uuid)
            {
              a.rb2d.position = new Vector2(players[i].position.x, players[i].position.y);
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

    /* Have to pick a paddle up (local system) to play.
     * Policy: Returns left most bottom
     * Assming horizontal right now.    
     */
    string GrabPaddleOrSpawn()
    {
      GameObject[] pcs = GameObject.FindGameObjectsWithTag("Player");

      GameObject selectedG = null;
      PlayerControls selected = null;
      foreach(GameObject g in pcs)
      {
        if (selected == null)
        {
          selected = pcs[0].GetComponent<PlayerControls>();
          continue;
        }

        PlayerControls pc = g.GetComponent<PlayerControls>();
        if (pc.rb2d.position.x < selected.rb2d.position.x)
        {
          selected = pc;
          selectedG = g;
        }

      }
      return selected.uuid;
    }

    bool JoinGame(GameJoin gj)
    {
      if (gj.gameId == "")
      {
        return false;
      }
      gameSession.gameId = gj.gameId;
      gameSession.side = gj.side;
      gameSession.uuidOtherPlayer = gj.uuidOtherPlayer;
      gameSession.status = STATUS.JOINED;

      return false;
    }

    bool RestartGame(GameRestart gr)
    {
      if (gr.gameId != gameSession.gameId)
      {
        return false;
      }
      gameSession.status = STATUS.RESTART;

      theBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
      PlayerScore1 = 0;
      PlayerScore2 = 0;

      gameSession.currentGs.sequence = 0;
      gameSession.status = STATUS.JOINED;

      return false;
    }

    // Match whatever WebSocket text is sending:
    void HandleMessage(string message)
    {
      var msg = MessageWrapper.UnWrapMessage(message);
      // Not quite symetric, but the server is text only.
      switch (msg.type)
      {
        case "qotd":
          break;
        case "register":
          // {"type":"register","sessionid":"hneLPx7piEKjb70S5t7pXg==","alias":"94f7aa0c-f5c9-4cb3-8eb1-bb3be15bc265"}
          GameRegister register = Messaging<GameRegister>.Deserialize(message);
          gameSession.sessionId = register.sessionId;
          gameSession.uuidPlayer = register.uuidPlayer;
          break;
        case "gameJoin":
          // {"type":"gameJoin","gameId":"119b91e6-68c3-4e53-81b3-657ffbe458d5"}
          GameJoin gj = Messaging<GameJoin>.Deserialize(message);
          JoinGame(gj);
          break;

        case "gameState":
          // {"type":"gameState","source":"server","gameId":"119b91e6-68c3-4e53-81b3-657ffbe458d5","sequence":0,"currentPlayer":"3979a6f8-7361-415b-89b5-3559f1aca652","players":[{"uuid":"3979a6f8-7361-415b-89b5-3559f1aca652","position":{"x":0,"y":0,"z":0},"velocity":{"x":0,"y":0,"z":0}},{"uuid":"94f7aa0c-f5c9-4cb3-8eb1-bb3be15bc265","position":{"x":0,"y":0,"z":0},"velocity":{"x":0,"y":0,"z":0}}],"balls":[{"position":{"x":0,"y":0,"z":0},"velocity":{"x":0,"y":0,"z":0}}]}
          GameState serverGs = Messaging<GameState>.Deserialize(message);
          UpdateLocalGame(serverGs);
          break;

        case "resign":
          break;
        case "restart":
          // {"type": "restart", "source": "server"}
          GameRestart gr = Messaging<GameRestart>.Deserialize(message);
          RestartGame(gr);
          break;

        default:
          Debug.Log("Unknown message arrived: " + msg.type + ", message: " + message);
          break;
      }

    }

  }

}