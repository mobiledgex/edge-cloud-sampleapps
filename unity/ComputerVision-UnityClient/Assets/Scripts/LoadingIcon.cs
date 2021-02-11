using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class LoadingIcon : MonoBehaviour
{
    // Update is called once per frame
    public int speed;
    void Update()
    {
        transform.Rotate(0, 0, speed * Time.deltaTime);
    }
}
