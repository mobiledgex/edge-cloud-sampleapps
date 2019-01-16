using UnityEngine;

using UnityEngine.UI;

public class ButtonScript : MonoBehaviour {
    public Button startMexSampleButton;
    public MexSample mexSample;

    void Start () {
        Button btn = startMexSampleButton.GetComponent<Button>();
        btn.onClick.AddListener(TaskOnClick);
    }

    void TaskOnClick(){
        Debug.Log ("You have clicked the button!");
        mexSample.RunSampleFlow();
    }

}
