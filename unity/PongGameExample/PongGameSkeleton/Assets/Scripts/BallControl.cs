using System.Collections;
using System.Collections.Generic;
using UnityEngine;


using System;

namespace MexPongGame
{
  public class BallControl : MonoBehaviour
  {
    public string uuid;
    public Rigidbody2D rb2d;
    GameObject theBall;
    GameObject gameManager;

    // Start is called before the first frame update
    void Start()
    {
      theBall = GameObject.FindGameObjectWithTag("Ball");
      gameManager = GameObject.FindGameObjectWithTag("GameManager");
      rb2d = GetComponent<Rigidbody2D>();
      //Invoke("GoBall", 2);
    }

    // Update is called once per frame
    void Update()
    {

    }

    void GoBall()
    {
      float rand = UnityEngine.Random.Range(0f, 2f);
      float updown = UnityEngine.Random.Range(-1f, 1f);
      if (rand < 1f)
      {
        rb2d.AddForce(new Vector2(20, 15 * updown));
      }
      else
      {
        rb2d.AddForce(new Vector2(-20, 15 * updown));
      }
    }

    void ResetBall()
    {
      rb2d = GetComponent<Rigidbody2D>();
      transform.position = Vector2.zero;
      rb2d.velocity = Vector2.zero;
    }

    void RestartGame()
    {
      ResetBall();
      //Invoke("GoBall", 1);
    }

    private void OnCollisionEnter2D(Collision2D collision)
    {
      if (collision.collider.CompareTag("BallGhost") || collision.collider.CompareTag("Ball"))
      {
        return;
      }

      if (collision.collider.CompareTag("Player"))
      {
        Vector2 vel;

        vel.x = rb2d.velocity.x * 1.1f;
        vel.y = (rb2d.velocity.y / 2f) + (collision.collider.attachedRigidbody.velocity.y / 1.5f);
        rb2d.velocity = vel;

        var gm = gameManager.GetComponent<GameManager>();
        PlayerControls c = collision.gameObject.GetComponent<PlayerControls>();



        gm.SendContactEvent(c, this, collision);
      }
    }

    public void setPosition(Position position)
    {
      var pos = transform.position;
      pos.x = position.x;
      pos.y = position.y;
      transform.position = pos;
    }

    public void setVelocity(Velocity velocity)
    {
      var vel = rb2d.velocity;
      vel.x = velocity.x;
      vel.y = velocity.y;
      rb2d.velocity = vel;
    }

  }
}