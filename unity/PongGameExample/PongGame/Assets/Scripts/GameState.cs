using System.Collections;
using System.Collections.Generic;
using UnityEngine;

using System;
using System.Runtime.Serialization;

// Example server is JSON based serialization
namespace MexPongGame
{
  [DataContract]
  public class GameState
  {
    [DataMember]
    long seq = 0;

    [DataMember]
    public string room_id;
    [DataMember]
    public string scene_id;
    [DataMember]
    public string player_self_id; // Local player id.

    // All players in room, scene
    [DataMember]
    public Player[] players;

    // Ball(s)
    [DataMember]
    public Ball[] balls;

    [DataMember]
    int score1 = 0;
    [DataMember]
    int score2 = 0;
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

    public Player CopyPlayer(PlayerControls pc)
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
        uuid = pc.uuid,
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
    public string uuid = "";
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
        uuid = bc.uuid,
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