using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace MexPongGame
{
  public class Sidewalls : MonoBehaviour
  {

    private void Start()
    {
    }

    void OnTriggerEnter2D(Collider2D hitInfo)
    {
      if (hitInfo.name == "Ball")
      {
        string wallName = transform.name;
        GameManager.Score(wallName);
        hitInfo.gameObject.SendMessage("RestartGame", 1.0f, SendMessageOptions.RequireReceiver);
      }
    }

  }
}
