using System.Collections;
using System.Collections.Generic;
using UnityEngine;

using System.IO;
using System.Text;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;

// Example server is JSON based serialization. Binary GRPC later.
namespace MexPongGame
{
  public static class Messaging<T>
  {
    /*
     * Stream must be repositionable.
     */
    private static string StreamToString(Stream s)
    {
      s.Position = 0;
      StreamReader reader = new StreamReader(s);
      string jsonStr = reader.ReadToEnd();
      return jsonStr;
    }

    public static string Serialize(T t)
    {
      DataContractJsonSerializer serializer = new DataContractJsonSerializer(typeof(T));
      MemoryStream ms = new MemoryStream();

      serializer.WriteObject(ms, t);
      string jsonStr = StreamToString(ms);

      return jsonStr;
    }

    public static T Deserialize(string jsonString)
    {
      Debug.Log("Deserilaizing this: <" + jsonString + ">");
      MemoryStream ms = new MemoryStream(Encoding.UTF8.GetBytes(jsonString ?? ""));
      return Deserialize(ms);
    }

    public static T Deserialize(Stream stream)
    {
      DataContractJsonSerializer deserializer = new DataContractJsonSerializer(typeof(T));
      T t = (T)deserializer.ReadObject(stream);
      return t;
    }

  }

  /* There is no intermediate JSON object. 
   * The MessageWrapper intermediate class specifies the type of object in the message,
   * so that it can be deserialized to the correct typed object.
   */
  [DataContract]
  public class MessageWrapper
  {
    [DataMember]
    public string type = "utf8";

    [DataMember]
    public string utf8Data;

    public static MessageWrapper WrapTextMessage(string jsonStr)
    {
      var wrapper = new MessageWrapper();
      wrapper.utf8Data = jsonStr;
      return wrapper;
    }

    public static MessageWrapper UnWrapMessage(string wrappedJsonStr)
    {
      var wrapper = Messaging<MessageWrapper>.Deserialize(wrappedJsonStr);
      return wrapper;
    }
  }

  [DataContract]
  public class GameRegister
  {
    [DataMember]
    public string type = "register";

    [DataMember]
    public string sessionId;

    [DataMember]
    public string uuidPlayer;
  }

  [DataContract]
  public class GameJoin
  {
    [DataMember]
    public string type = "gameJoin";

    [DataMember]
    public string gameId;

    [DataMember]
    public int side;

    [DataMember]
    public string uuidOtherPlayer;

    [DataMember]
    public string ballId;
  }

  [DataContract]
  public class GameRestart
  {
    [DataMember]
    public string type = "gameRestart";

    [DataMember]
    public string gameId;
  }

  // On start of game, ever player is assigned player1 (0) or player2 (1) and a user id.
  [DataContract]
  public class ScoreEvent
  {
    [DataMember]
    public string type = "scoreEvent";

    [DataMember]
    public string gameId;

    [DataMember]
    public string uuid;

    [DataMember]
    public int side;

    [DataMember]
    public int playerScore1;
    [DataMember]
    public int playerScore2;
  }

  // Generic position and velocity, with uuid.
  [DataContract]
  public class MoveEvent
  {
    [DataMember]
    public string type = "moveEvent";

    [DataMember]
    public string objectType;

    [DataMember]
    public string uuid;

    [DataMember]
    public string gameId;

    [DataMember]
    public Position position;

    [DataMember]
    public Velocity velocity;
  }


  [DataContract]
  public class GameState
  {
    [DataMember]
    public string type = "gameState";

    [DataMember]
    public string source;

    [DataMember]
    public uint sequence;

    [DataMember]
    public string gameId;

    [DataMember]
    public string currentPlayer;

    // All players in room, scene
    [DataMember]
    public Player[] players;

    // Ball(s)
    [DataMember]
    public Ball[] balls;

    [DataMember]
    public int score1; // Side 0

    [DataMember]
    public int score2; // Side 1

    public GameState() { }

    // Copies everything, using the current ball and player
    public GameState(GameState gs, BallControl ballc, PlayerControls currentPlayer)
    {
      this.source = "client";
      this.sequence = gs.sequence;
      this.gameId = gs.gameId;

      this.currentPlayer = currentPlayer.uuid;

      int plen = this.players.Length;
      this.players = new Player[plen];

      Ball b;
      int blen = this.balls.Length;
      this.balls = new Ball[blen];
      for (var idx = 0; idx < plen; idx++)
      {
        b = gs.balls[idx];
        if (b.uuid != currentPlayer.uuid)
        {
          this.balls[idx] = new Ball(b);
        }
        else
        {
          this.balls[idx] = Ball.CopyBall(ballc);
        }
      }

      Player p;
      for (var idx = 0; idx < plen; idx++)
      {
        p = gs.players[idx];
        if (p.uuid != currentPlayer.uuid)
        {
          this.players[idx] = new Player(p);
        }
        else
        {
          this.players[idx] = Player.CopyPlayer(currentPlayer);
        }
      }

    }
  }

  [DataContract]
  public class Player
  {
    [DataMember]
    public string uuid = "";
    [DataMember]
    public Position position = new Position(Vector2.zero);
    [DataMember]
    public Velocity velocity = new Velocity(Vector2.zero);

    public Player(Player p)
    {
      this.uuid = p.uuid;
      this.position = new Position(p.position);
      this.velocity = new Velocity(p.velocity);
    }

    public Player(string uuid, Position pos, Velocity vel)
    {
      this.uuid = uuid;
      this.position = new Position(pos);
      this.velocity = new Velocity(vel);
    }

    public static Player CopyPlayer(PlayerControls pc)
    {
      Transform tf = pc.transform;

      Position pos = new Position(tf.position);
      Velocity vel = new Velocity(pc.rb2d.velocity);

      Player player = new Player(pc.uuid, pos, vel);


      return player;
    }
  }

  [DataContract]
  public class Ball
  {
    [DataMember]
    public string uuid;

    [DataMember]
    public Position position;
    [DataMember]
    public Velocity velocity;

    public Ball(Ball p)
    {
      this.uuid = p.uuid;
      this.position = new Position(p.position);
      this.velocity = new Velocity(p.velocity);
    }

    public Ball(string uuid, Position pos, Velocity vel)
    {
      this.uuid = uuid;
      this.position = new Position(pos);
      this.velocity = new Velocity(vel);
    }

    public static Ball CopyBall(BallControl bc)
    {

      Transform tf = bc.transform;

      Position pos = new Position(tf.position);
      Velocity vel = new Velocity(bc.rb2d.velocity);

      Ball nb = new Ball(bc.uuid, pos, vel);

      return nb;
    }
  }

  [DataContract]
  public class Position
  {
    [DataMember]
    public float x = 0;
    [DataMember]
    public float y = 0;

    public Position(Vector2 p)
    {
      x = p.x;
      y = p.y;
    }

    public Position(Position p)
    {
      x = p.x;
      y = p.y;
    }

  }

  [DataContract]
  public class Velocity
  {
    [DataMember]
    public float x = 0;
    [DataMember]
    public float y = 0;

    public Velocity(Vector2 v)
    {
      x = v.x;
      y = v.y;
    }

    public Velocity(Velocity v)
    {
      x = v.x;
      y = v.y;
    }
  }
}