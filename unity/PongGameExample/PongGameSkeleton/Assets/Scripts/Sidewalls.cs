using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace MexPongGame
{
  public class Sidewalls : MonoBehaviour
  {
    GameObject gameManagerObj;


    private void Start()
    {
      gameManagerObj = GameObject.FindGameObjectWithTag("GameManager");

    }

    void OnTriggerEnter2D(Collider2D hitInfo)
    {
      if (hitInfo.name == "Ball")
      {
        GameManager gameManager = gameManagerObj.GetComponent<GameManager>();

        string wallName = transform.name;

        // async check scoring on "goal" wall hit, and send to server.
        gameManager.Score(wallName); 

        hitInfo.gameObject.SendMessage("RestartGame", 1.0f, SendMessageOptions.RequireReceiver);
      }
    }

  }
}
