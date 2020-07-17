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

using UnityEngine;
using UnityEngine.UI;
using System;


namespace MobiledgeXPingPongGame {

  [RequireComponent(typeof(MobiledgeX.LocationService))]
  public class GameManager : MonoBehaviour
  {
    public static int PlayerScore1 = 0;
    public static int PlayerScore2 = 0;
    public GUISkin layout;
    public GameObject theBall;
    GameObject[] players;
    public GameObject ghostBall; // Just one.
    public GameObject ghostPlayer; // Local player.
    public NetworkManager networkManager;
    GameObject uiG;
    public Text uiConsole;
    public InputField roomIdInput;

#region MonoBehaviour Callbacks
    private void Awake()
    {
            theBall = GameObject.FindGameObjectWithTag("Ball");
            players = GameObject.FindGameObjectsWithTag("Player");
            // Create a Mex Paddle (for local user) from the Prefab:
            ghostPlayer = (GameObject)Instantiate(Resources.Load("PaddleGhost"));
            ghostBall = (GameObject)Instantiate(Resources.Load("BallGhost"));
            // Attach a listener to the Room ID input field.
            roomIdInput = GameObject.Find("InputFieldRoomId").GetComponent<InputField>();
            roomIdInput.onEndEdit.AddListener(networkManager.ConnectToServerWithRoomId);
            uiG = GameObject.FindGameObjectWithTag("UIConsole");
            uiConsole = uiG.GetComponent<Text>();
        }
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
#endregion

    public void clog(string msg)
    {
      uiConsole.text = msg;
      Debug.Log(msg);
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
        uuid = networkManager.gameSession.uuidPlayer,
        gameId = networkManager.gameSession.gameId,
        side = networkManager.gameSession.side,
        playerScore1 = PlayerScore1,
        playerScore2 = PlayerScore2
      };

      networkManager.webSocketClient.Send(Messaging<ScoreEvent>.Serialize(scoreEvent));
    }
    public void UpdateBall()
    {
      var bc = theBall.GetComponent<BallControl>();
      Ball ball = Ball.CopyBall(bc);

      networkManager.gameSession.currentGs.balls[0] = ball;

      MoveEvent moveEvent = new MoveEvent
      {
        uuid = bc.uuid,
        playeruuid = networkManager.gameSession.uuidPlayer,
        gameId = networkManager.gameSession.gameId,
        objectType = "Ball",
        position = new Position(ball.position),
        velocity = new Velocity(ball.velocity)
      };

      networkManager.webSocketClient.Send(Messaging<MoveEvent>.Serialize(moveEvent));
    }
    public void UpdatePlayer()
    {
      // Client side dict needed.
      Player[] gsPlayers = new Player[networkManager.gameSession.currentGs.players.Length];
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
      networkManager.gameSession.currentGs.players = gsPlayers;

      MoveEvent moveEvent = new MoveEvent
      {
        uuid = selected.uuid,
        playeruuid = selected.uuid,
        gameId = networkManager.gameSession.gameId,
        objectType = "Player",
        position = new Position(selected.position),
        velocity = new Velocity(selected.velocity)
      };

      networkManager.webSocketClient.Send(Messaging<MoveEvent>.Serialize(moveEvent));
    }
    public void SendContactEvent(PlayerControls c, BallControl b, Collision2D collision)
    {
      // If the contact is actually the other player
      ContactEvent ce = new ContactEvent
      {
        sequence = networkManager.gameSession.currentGs.sequence,
        objectType = "Ball",
        uuid = c.uuid,
        playeruuid = networkManager.gameSession.uuidPlayer, // Sender source of this event.
        gameId = networkManager.gameSession.gameId,
        position = new Position(b.rb2d.position),
        velocity = new Velocity(b.rb2d.velocity)
      };
      networkManager.webSocketClient.Send(Messaging<ContactEvent>.Serialize(ce));
    }
    public void SendRestart()
    {
      GameRestart gr = new GameRestart();
      gr.gameId = networkManager.gameSession.gameId;
      gr.balls = new Ball[1];
      gr.balls[0] = Ball.CopyBall(theBall.GetComponent<BallControl>());
      networkManager.webSocketClient.Send(Messaging<GameRestart>.Serialize(gr));
    }
    public bool UpdateScore(ScoreEvent se)
    {
      // Policy: Server rules:
      PlayerScore1 = se.playerScore1;
      PlayerScore2 = se.playerScore2;

      return true;
    }
    public bool UpdatePosition(MoveEvent moveItem)
    {
      //clog("moveItem: " + moveItem.uuid);
      var gs = networkManager.gameSession.currentGs;

      if (moveItem.sequence < networkManager.gameSession.currentGs.sequence)
      {
        clog("old event.");
        return false; // Old.
      }

      if (moveItem.uuid == gs.balls[0].uuid)
      {
        // If the source is the other player, and that's the last contact,
        // update local ball.
        if (networkManager.gameSession.lastUuidPing == networkManager.gameSession.uuidOtherPlayer &&
            moveItem.playeruuid == networkManager.gameSession.uuidOtherPlayer)
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

      if (moveItem.uuid == networkManager.gameSession.uuidPlayer)
      {
        UpdatePlayerGhost(moveItem);
        // Server echo of current player position and velocity.
        // Add a gameObject if not existing, and show it along with the current player's position.
        // Also, if significantly different, jump player to "server" position, or interpolate postion over time.
      }
      else if (moveItem.uuid == networkManager.gameSession.uuidOtherPlayer) // Other player, blind copy.
      {
        foreach (var player in gs.players)
        {
          if (player.uuid == networkManager.gameSession.uuidOtherPlayer)
          {
            player.position = moveItem.position;
            player.velocity = moveItem.velocity;
            // Apply to GameObject of player:
            var gp = GameObject.FindGameObjectsWithTag("Player");
            foreach (var g in gp)
            {
              var p = g.GetComponent<PlayerControls>();
              if (p.uuid == networkManager.gameSession.uuidOtherPlayer)
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
    public bool UpdateBallGhost(MoveEvent moveItem)
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
    public bool UpdatePlayerGhost(MoveEvent moveItem)
    {
      if (moveItem.objectType == "Player" &&
          moveItem.uuid == networkManager.gameSession.uuidPlayer)
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
    public bool HandleContactEvent(ContactEvent ce)
    {
      // This is an event everyone should (try) to agree on, even if the simulation diverges.
      // 1) It's the latest event.
      // 2) The other player has already observed this on their game simulation.
      BallControl bc = theBall.GetComponent<BallControl>();

      if (bc.uuid != networkManager.gameSession.currentGs.balls[0].uuid)
      {
        clog("Ball UUID is unknown! Contact event unknown.");
        return false;
      }

      if (ce.playeruuid == networkManager.gameSession.uuidOtherPlayer)
      {
        clog("Matching local ball to remote player event: " + ce.playeruuid);
        bc.setPosition(ce.position);
        bc.setVelocity(ce.velocity);
        networkManager.gameSession.lastUuidPing = networkManager.gameSession.uuidOtherPlayer;
      }
      else if(ce.playeruuid == networkManager.gameSession.uuidPlayer)
      {
        clog("Updating ghost ball (once) to server version: " + ce.velocity);
        // Self echo, just update server ghost.
        var gbc = ghostBall.GetComponent<BallControl>();
        gbc.setPosition(ce.position);
        gbc.setVelocity(ce.velocity);
        networkManager.gameSession.lastUuidPing = networkManager.gameSession.uuidPlayer;
      }

      return true;
    }
    public bool ApplyGameState(GameState gameState)
    {
      // Instantiate other player, inspect, and then apply game state.
      return true;
    }
    // Not differential, but this is small. Bespoke. TODO: GRPC
    GameState GatherGameState()
    {
      GameState gameState = new GameState();

      gameState.gameId = networkManager.gameSession.gameId; // Keep gameId.
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
    public void UpdateServer()
    {
      GameState gameState = GatherGameState();
      gameState.type = "gameState";
      gameState.source = "networkManager.webSocketClient";

      string jsonStr = Messaging<GameState>.Serialize(gameState);
      clog("UpdateServer: " + jsonStr);
      MessageWrapper wrapped = MessageWrapper.WrapTextMessage(jsonStr);
      networkManager.webSocketClient.Send(Messaging<MessageWrapper>.Serialize(wrapped));

      networkManager.gameSession.currentGs = gameState;

      return;
    }
    public bool PositionInRange(Position p1, Position p2, float error = 1.5f)
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
    public bool VelocityInRange(Velocity p1, Velocity p2, float error = 20.0f)
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
    public void UpdateLocalGame(GameState serverGameState)
    {

      if (serverGameState.sequence == 0)
      {
        networkManager.gameSession.status = STATUS.INGAME;
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
        if (a.uuid == networkManager.gameSession.uuidOtherPlayer)
        {
          // Find other player in server view:
          for (var i = 0; i < serverGameState.players.Length; i++)
          {
            if (a.uuid == serverPlayers[i].uuid)
            {
              a.setPosition(serverPlayers[i].position);
              if (a.uuid == networkManager.gameSession.uuidPlayer)
              {
                cpIdx = i;
              }
              else if (a.uuid == networkManager.gameSession.uuidOtherPlayer)
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
    public bool JoinGame(GameJoin gj)
    {
      clog("Told to join gameId: " + gj.gameId + ", side: " + gj.side);
      if (gj.gameId == "")
      {
        clog("Bad gameId!");
        return false;
      }

      networkManager.gameSession.gameId = gj.gameId;
      networkManager.gameSession.side = gj.side;
      networkManager.gameSession.uuidOtherPlayer = gj.uuidOtherPlayer;
      networkManager.gameSession.status = STATUS.JOINED;

      networkManager.gameSession.currentGs = new GameState();
      var gs = networkManager.gameSession.currentGs;

      gs.currentPlayer = networkManager.gameSession.uuidPlayer;

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

      clog("Left sel: " + left.transform.position.x + "Right other: " + right.transform.position.x);

      if (networkManager.gameSession.side == 0)
      {
        left.uuid = networkManager.gameSession.uuidPlayer; // Player 1 assigned in match by server.
        left.ownPlayer = true;

        right.uuid = networkManager.gameSession.uuidOtherPlayer;
        right.ownPlayer = false;
        gs.players[0] = Player.CopyPlayer(left);
        gs.players[1] = Player.CopyPlayer(right);
      }
      else if (networkManager.gameSession.side == 1)
      {

        right.uuid = networkManager.gameSession.uuidPlayer; // Player 2 assigned in match by server.
        right.ownPlayer = true;

        left.uuid = networkManager.gameSession.uuidOtherPlayer;
        left.ownPlayer = false;

        gs.players[0] = Player.CopyPlayer(right);
        gs.players[1] = Player.CopyPlayer(left);
      }

      // Assign player state:
      networkManager.gameSession.currentGs = gs;

      clog("Transition to inGame.");
      networkManager.gameSession.status = STATUS.INGAME;
      return true;
    }
    public bool StartNextRound(NextRound nr)
    {
      clog("Start next round for game, gameId: " + nr.gameId);
      if (nr.gameId != networkManager.gameSession.gameId)
      {
        return false;
      }
      networkManager.gameSession.lastUuidPing = null;
      networkManager.gameSession.status = STATUS.INGAME;

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
    public bool RestartGame(GameRestart gr)
    {
      clog("Restarting game, gameId: " + gr.gameId);
      if (gr.gameId != networkManager.gameSession.gameId)
      {
        return false;
      }
      networkManager.gameSession.status = STATUS.RESTART;

      //theBall.SendMessage("ResetBall", null, SendMessageOptions.RequireReceiver);
      PlayerScore1 = 0;
      PlayerScore2 = 0;

      networkManager.gameSession.currentGs.sequence = 0;
      networkManager.gameSession.status = STATUS.INGAME;

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
