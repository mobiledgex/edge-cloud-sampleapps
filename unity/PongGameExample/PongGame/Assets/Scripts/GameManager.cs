using System.Collections;
using System.Collections.Generic;
using UnityEngine;

using System.Net.WebSockets;
using System;
using System.Threading;
using System.Threading.Tasks;


namespace MexPongGame {
  public class GameManager : MonoBehaviour
  {
    public static int PlayerScore1 = 0;
    public static int PlayerScore2 = 0;

    public GUISkin layout;

    GameObject theBall;
    GameState gs;
    WsClient client;

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

      gs = new GameState();

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
      }

      UpdateServer();

    }


    public void ReceiveMessage(string message)
    {
      Debug.Log("Message: " + message);
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
      gameState.gameId = gs.gameId; // Keep gameId.
      gameState.score1 = PlayerScore1;
      gameState.score2 = PlayerScore2;

      // copy Balls:
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
      await client.Send(jsonStr);

      return;
    }

  }

}