using UnityEngine.UI;
using UnityEngine;
using System.Collections;
namespace MobiledgeXComputerVision {

    public class UIManager : MonoBehaviour
    {
        public GameObject ModesPanel;
        public GameObject DataSourcePanel;
        public GameObject CameraPanel;
        public GameObject VideoPanel;
        public Text infoText;
        public Button backButton;
        public AppManager appManager;
        public double rollingAvg;
        
        private void Start()
        {
            backButton.onClick.AddListener(BackButton);
        }

        public void SetMode(int modeSelected)
        {
            switch (modeSelected)
            {
                case 0:
                    AppManager.serviceMode = AppManager.ServiceMode.FaceDetection;
                    infoText.text = " FaceDetection Enabled";
                    break;
                case 1:
                    AppManager.serviceMode = AppManager.ServiceMode.FaceRecognition;
                    infoText.text = " FaceRecognition Enabled";
                    break;
                case 2:
                    AppManager.serviceMode = AppManager.ServiceMode.PoseDetection;
                    infoText.text = " PoseDetection Enabled";
                    break;
                case 3:
                    AppManager.serviceMode = AppManager.ServiceMode.ObjectDetection;
                    infoText.text = " ObjectDetection Enabled";
                    break;
            }
            AppManager.level++;
            SetLevel(AppManager.level);
        }

        public void SetDataSource(int dataSourceSelected)
        {
            switch (dataSourceSelected)
            {
               case 0:
                   AppManager.source = AppManager.DataSource.CAMERA;
                   CameraPanel.SetActive(true);
                   break;
               case 1:
                   AppManager.source = AppManager.DataSource.VIDEO;
                   VideoPanel.SetActive(true);
                   break;
                case 2:
                    AppManager.source = AppManager.DataSource.VIDEO;
                    VideoPanel.SetActive(true); //fixme change to nReal Later
                    break;
            }
            AppManager.level++;
            SetLevel(AppManager.level);
        }

        IEnumerator ShowInfoText()
        {
            infoText.gameObject.SetActive(true);
            yield return new WaitForSeconds(2);
            infoText.gameObject.SetActive(false);
        }

        public async void SetLevel(int level)
        {
            switch (level)
            {
                case 1:
                    CameraPanel.SetActive(false);
                    VideoPanel.SetActive(false);
                    ModesPanel.SetActive(false);
                    DataSourcePanel.SetActive(true);
                    backButton.gameObject.SetActive(true);
                    await AppManager.SetURL();
                    break;
                case 2:
                    ModesPanel.SetActive(false);
                    DataSourcePanel.SetActive(false);
                    backButton.gameObject.SetActive(true);
                    StartCoroutine(appManager.AppFlow());
                    StartCoroutine(ShowInfoText());
                    break;
                default:
                case 0:
                    CameraPanel.SetActive(false);
                    VideoPanel.SetActive(false);
                    DataSourcePanel.SetActive(false);
                    backButton.gameObject.SetActive(false);
                    ModesPanel.SetActive(true);
                    break;
            }
        }

        public void BackButton()
        {
            if (AppManager.level < 1)
            {
                AppManager.level = 0;
            }
            else
            {
                if(AppManager.level == 2)
                {
                    appManager.StopAllCoroutines();
                    AppManager.showGUI = false;
                }
                AppManager.level--;
            }
            SetLevel(AppManager.level);
        }
    }

}
