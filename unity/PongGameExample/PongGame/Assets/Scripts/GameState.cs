using System.Collections;
using System.Collections.Generic;
using UnityEngine;

using System.IO;
using System.Text;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;

// Example server is JSON based serialization
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
    public string utf8data;

    public static MessageWrapper WrapTextMessage(string jsonStr)
    {
      var wrapper = new MessageWrapper();
      wrapper.utf8data = jsonStr;
      return wrapper;
    }

    public static MessageWrapper UnWrapMessage(string wrappedJsonStr)
    {
      var wrapper = Messaging<MessageWrapper>.Deserialize(wrappedJsonStr);
      return wrapper;
    }
  }

  [DataContract]
  public class GameState
  {
    [DataMember]
    public string type = "gameState";

    [DataMember]
    public string source;

    [DataMember]
    public uint seq;

    [DataMember]
    public string gameId;
    [DataMember]
    public string sceneId;
    [DataMember]
    public string playerSelfId; // Local player id.

    [DataMember]
    public string currentPlayer;

    // All players in room, scene
    [DataMember]
    public Player[] players;

    // Ball(s)
    [DataMember]
    public Ball[] balls;

    [DataMember]
    public int score1;
    [DataMember]
    public int score2;
  }

  [DataContract]
  public class Player
  {
    [DataMember]
    public string uuid = "";
    [DataMember]
    public Position position = new Position();
    [DataMember]
    public Velocity velocity = new Velocity();

    public static Player CopyPlayer(PlayerControls pc)
    {
      Transform tf = pc.transform;

      Position pos = new Position
      {
        x = tf.position.x,
        y = tf.position.y,
        z = tf.position.z
      };

      Velocity vel = new Velocity
      {
        x = pc.rb2d.velocity.x,
        y = pc.rb2d.velocity.y,
        z = 0f
      };

      Player player = new Player
      {
        position = pos,
        velocity = vel
      };

      return player;
    }
  }

  [DataContract]
  public class Ball
  {
    [DataMember]
    public Position position = new Position();
    [DataMember]
    public Velocity velocity = new Velocity();

    public static Ball CopyBall(BallControl bc)
    {

      Transform tf = bc.transform;

      Position pos = new Position
      {
        x = tf.position.x,
        y = tf.position.y,
        z = tf.position.z
      };

      Velocity vel = new Velocity
      {
        x = bc.rb2d.velocity.x,
        y = bc.rb2d.velocity.y,
        z = 0f
      };

      Ball nb = new Ball
      {
        position = pos,
        velocity = vel
      };

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
    [DataMember]
    public float z = 0;
  }

  [DataContract]
  public class Velocity
  {
    [DataMember]
    public float x = 0;
    [DataMember]
    public float y = 0;
    [DataMember]
    public float z = 0;
  }
}