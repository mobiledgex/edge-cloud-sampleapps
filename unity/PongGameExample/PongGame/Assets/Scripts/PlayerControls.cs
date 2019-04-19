﻿using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class PlayerControls : MonoBehaviour
{
  // This should be serialized.
  public KeyCode moveUp = KeyCode.W;
  public KeyCode moveDown = KeyCode.S;
  public float speed = 10f;
  public float boundY = 2.25f;
  private Rigidbody2D rb2d;

  // Start is called before the first frame update
  void Start()
  {
    rb2d = GetComponent<Rigidbody2D>();
  }

  // Update is called once per frame
  // Note: rigid body physics is set to kinematics.
  void Update()
  {
    var vel = rb2d.velocity;
    if (Input.GetKey(moveUp))
    {
      vel.y = speed;
    }
    else if (Input.GetKey(moveDown))
    {
      vel.y = -speed;
    }
    rb2d.velocity = vel;

    var pos = transform.position;
    if (pos.y > boundY)
    {
      pos.y = boundY;
    }
    else if (pos.y < -boundY)
    {
      pos.y = -boundY;
    }
    transform.position = pos;
  }
}
