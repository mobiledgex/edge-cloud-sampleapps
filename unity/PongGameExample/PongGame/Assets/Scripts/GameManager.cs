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




    string server = "ws://localhost:3000";

    private Uri uri = new Uri("ws://localhost:3000");
    private ClientWebSocket ws = new ClientWebSocket();

    Task openTask;
    Task<string> receiveTask;

    // Use this for initialization
    async void Start()
    {

      theBall = GameObject.FindGameObjectWithTag("Ball");
      players = GameObject.FindGameObjectsWithTag("Player");
      client = new WsClient();
      gameSession.status = STATUS.LOBBY;

      await client.Connect(uri);
    }
    

    async void Update()
    {
      // Receive runs in a background filling the receive concurrent queue.
      var cqueue = client.receiveQueue;
      string msg;
      while(cqueue.TryPeek(out msg))
      {
        cqueue.TryDequeue(out msg);
        Debug.Log("Dequeued this message: " + msg);
        await HandleMessage(msg);
      }

      // Sever needs to know where the ball is:
      //await updateBall();

      // Update local player paddle (local authority)
      await updatePlayer();

      // Update Server paddle( reslove differences against server)

    }

    public async Task Score(string wallID)
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

      await client.Send(Messaging<ScoreEvent>.Serialize(scoreEvent));
    }

    public async Task updateBall()
    {
      var bc = theBall.GetComponent<BallControl>();
      Ball ball = Ball.CopyBall(bc);

      gameSession.currentGs.balls[0] = ball;

      MoveEvent moveEvent = new MoveEvent
      {
        uuid = bc.uuid,
        gameId = gameSession.gameId,
        objectType = "Ball",
        position = ball.position,
        velocity = ball.velocity
      };

      await client.Send(Messaging<MoveEvent>.Serialize(moveEvent));
    }

    public async Task updatePlayer()
    {
      // Client side dict needed.
      Player[] gsPlayers = new Player[gameSession.currentGs.players.Length];
      Player selected = null;
      // Only ever need to tell the server of own location (for now)

      int idx = 0;
      foreach (GameObject gpc in players)
      {
        PlayerControls pc = gpc.GetComponent<PlayerControls>();
        if (pc.uuid == gameSession.uuidPlayer)
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
        gameId = gameSession.gameId,
        objectType = "Player",
        position = selected.position,
        velocity = selected.velocity
      };

      await client.Send(Messaging<MoveEvent>.Serialize(moveEvent));
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


    // Match whatever WebSocket text is sending
    // Consistency: General rule here is that the game state if not timestamped, events may not represent the same time window.
    async Task HandleMessage(string message)
    {
      var msg = MessageWrapper.UnWrapMessage(message);
      // Not quite symetric, but the server is text only.
      switch (msg.type)
      {
        case "qotd":
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
          //UpdateLocalGame(serverGs);
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

    bool UpdateScore(ScoreEvent se)
    {
      // Policy: Server rules:
      PlayerScore1 = se.playerScore1;
      PlayerScore2 = se.playerScore2;

      return true;
    }

    bool UpdatePosition(MoveEvent moveItem)
    {
      Debug.Log("moveItem: " + moveItem.uuid);
      var gs = gameSession.currentGs;

      // blind update: single ball.
      if (moveItem.uuid == gs.balls[0].uuid)
      {
        gs.balls[0].position = moveItem.position;
        gs.balls[0].velocity = moveItem.velocity;
        // Apply server state to Component
      }

      if (moveItem.uuid == gameSession.uuidPlayer)
      {
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
          Vector2 newPos;
          newPos.x = serverBall.position.x;
          newPos.y = serverBall.position.y;
          bc.rb2d.position = newPos;
        }
      } else {
        // Perhaps a new game. No server info.

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
      gameSession.currentGs = GatherGameState();

      gameSession.gameId = gj.gameId;
      gameSession.side = gj.side;
      gameSession.uuidOtherPlayer = gj.uuidOtherPlayer;
      gameSession.status = STATUS.JOINED;

      var gs = gameSession.currentGs;
      // Update Ball:
      if (gs.balls.Length == 0)
      {
        gs.balls = new Ball[2];
      }
      gs.balls[0].uuid = gj.ballId;
      gs.balls[0].velocity = new Velocity(Vector2.zero);
      gs.balls[0].position = new Position(Vector2.zero);
      // Actual object:
      BallControl bc = theBall.GetComponent<BallControl>();
      bc.uuid = gj.ballId;

      // Match Assignments:
      // Given a side, 0 one is player one (left). 1 other player (right)
      GameObject[] pcs = GameObject.FindGameObjectsWithTag("Player");
      PlayerControls selected = null;
      PlayerControls other = null;

      if (pcs.Length != 2)
      {
        return false; // Can't join this game.
      }

      foreach (GameObject g in pcs)
      {
        if (selected == null)
        {
          selected = g.GetComponent<PlayerControls>();
          continue;
        }

        other = g.GetComponent<PlayerControls>();
        if (other.rb2d.position.x < selected.rb2d.position.x)
        {
          selected = other;
        }
      }

      if (gameSession.side == 0)
      {
        selected.uuid = gameSession.uuidPlayer; // Player 1 assigned in match by server.
        other.uuid = gameSession.uuidOtherPlayer;
      }
      else if (gameSession.side == 1)
      {
        other.uuid = gameSession.uuidPlayer; // Player 2 assigned in match by server.
        selected.uuid = gameSession.uuidOtherPlayer;
      }

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



  }

}