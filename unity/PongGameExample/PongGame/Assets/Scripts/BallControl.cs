using System.Collections;
using System.Collections.Generic;
using UnityEngine;


using System;

namespace MexPongGame
{
  public class BallControl : MonoBehaviour
  {
    public Rigidbody2D rb2d;
    GameObject theBall;

    // Start is called before the first frame update
    void Start()
    {
      theBall = GameObject.FindGameObjectWithTag("Ball");
      rb2d = GetComponent<Rigidbody2D>();
      Invoke("GoBall", 2);

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
      Invoke("GoBall", 1);
    }

    private void OnCollisionEnter2D(Collision2D collision)
    {
      if (collision.collider.CompareTag("Player"))
      {
        Vector2 vel;

        vel.x = rb2d.velocity.x;
        vel.y = (rb2d.velocity.y / 2f) + (collision.collider.attachedRigidbody.velocity.y / 2.5f);
        rb2d.velocity = vel;
      }
    }

  }
}