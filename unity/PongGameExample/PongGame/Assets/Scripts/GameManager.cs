using System.Collections;
using System.Collections.Generic;
using UnityEngine;

using System;

namespace MexPongGame {
  public class GameManager : MonoBehaviour
  {
    string uuid = new System.Guid(DateTime.Now.Ticks + "").ToString();

    public static int PlayerScore1 = 0;
    public static int PlayerScore2 = 0;

    public GUISkin layout;

    GameObject theBall;
    GameState gs;

    wsClient client = new wsClient();

    string server = "ws://localhost:8080";

    // Use this for initialization
    void Start()
    {
      theBall = GameObject.FindGameObjectWithTag("Ball");
      client.Connect(server);

    }

    private void Update()
    {
      // periodic server updates.


      // Gather Game state. Local player runs locally.
      // 1) Upload entire local state to server, server authoritive still, but server gets to check the score and such.
      // ???
      // 3) Profit!

      gs = new GameState();



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

    bool applyGameState(GameState gs)
    {
      // Instantiate other player, inspect, and then apply game state.
      return true;
    }

    // Not differential, but this is small. Bespoke. TODO: GRPC
    GameState gatherGameState()
    {
      GameState gs = new GameState();

      GameObject[] bls = GameObject.FindGameObjectsWithTag("Ball");
      if (bls.Length > 0)
      {
        gs.balls = new Ball[bls.Length];
      }

      // copyPlayer(s)
      GameObject[] pcs = GameObject.FindGameObjectsWithTag("Player");
      if (pcs.Length > 0)
      {
        Player[] players = new Player[pcs.Length];
      }

      foreach(GameObject go in pcs)
      {
        PlayerControls pc = go.GetComponent<PlayerControls>();
        if (pc != null) {
          Player player = CopyPlayer(pc);
        }
      }



    }

    Ball CopyBall(BallControl bc)
    {

      Transform tf = bc.transform;

      Position position = new Position
      {
        x = tf.position.x,
        y = tf.position.y,
        z = tf.position.z
      };

      Velocity velocity = new Velocity
      {
        x = bc.rb2d.velocity.x,
        y = bc.rb2d.velocity.y,
        z = 0f
      };

      Ball ball = new Ball
      {
        uuid = bc.uuid,
        position = position,
        velocity = velocity,

      };

      return ball;
    }

    Player CopyPlayer(PlayerControls pc)
    {
      Transform tf = pc.transform;

      Position position = new Position
      {
        x = tf.position.x,
        y = tf.position.y,
        z = tf.position.z
      };

      Velocity velocity = new Velocity
      {
        x = pc.rb2d.velocity.x,
        y = pc.rb2d.velocity.y,
        z = 0f
      };

      Player player = new Player
      {
        uuid = pc.uuid,
        position = position,
        velocity = velocity
      }

      return player;
    }

    async Task UpdateServer(GameState gs)
    {
      return;
    }

  }

}