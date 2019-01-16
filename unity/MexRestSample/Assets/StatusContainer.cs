using System.Collections.Generic;
using UnityEngine;

using UnityEngine.UI;

/* A simple data container for scrollRect content. */
public class StatusContainer : MonoBehaviour {

    public string status; // Need a list, or accessor.
    public Text scrollViewText;
    long count = 0;

    List<string> posts = new List<string>(26);

	// Use this for initialization
	void Start () {
       scrollViewText = GameObject.Find("/UICanvas/OutputScrollView/Viewport/Content/GRPCOutputText").GetComponent<Text>();
    }
	
	// Update is called once per frame
	void Update () {
        status = "[Sample Output]: " + count + "\n";
        foreach (string post in posts) {
            status += post + "\n";
        }
        count++;
        scrollViewText.text = status;
    }

    public void Post(string postText) {
        if (postText == null) {
            return;
        }
        // Trim first in line if needed.
        if (posts.Count == posts.Capacity) {
            posts.RemoveAt(0);
        }
        posts.Add(postText);
    }
}
