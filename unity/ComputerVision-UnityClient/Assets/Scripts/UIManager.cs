using UnityEngine.UI;
using UnityEngine;
using UnityEngine.Video;

namespace MobiledgeXComputerVision
{
    public class UIManager : MonoBehaviour
    {
        public GameObject Background;
        public GameObject ModesPanel;
        public GameObject DataSourcePanel;
        public GameObject CameraPanel;
        public GameObject VideoPanel;
        public Button backButton;
        public AppManager appManager;
        public NetworkManager networkManager;
        public Image Logo;
        public GameObject StatsButton;
        public GameObject StatsPanel;

        #region MonoBehaviour Callbacks
        private void Start()
        {
            backButton.onClick.AddListener(BackButton);
        }
        #endregion
        
        public void SetMode(int modeSelected)
        {
            switch (modeSelected)
            {
                case 0:
                    AppManager.serviceMode = AppManager.ServiceMode.FaceDetection;
                    break;
                case 1:
                    AppManager.serviceMode = AppManager.ServiceMode.FaceRecognition;
                    break;
                case 2:
                    AppManager.serviceMode = AppManager.ServiceMode.ObjectDetection;
                    break;
            }
            AppManager.level++;
            UpdateUIBasedOnLevel(AppManager.level);
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
                //case 2:
                //    AppManager.source = AppManager.DataSource.nReal;
                //    VideoPanel.SetActive(true);
                //    break;
            }
            AppManager.level++;
            UpdateUIBasedOnLevel(AppManager.level);
        }

        public void UpdateUIBasedOnLevel(int level)
        {
            switch (level)
            {
                case 0: // Select Service
                    CameraPanel.SetActive(false);
                    VideoPanel.SetActive(false);
                    DataSourcePanel.SetActive(false);
                    backButton.gameObject.SetActive(false);
                    ModesPanel.SetActive(true);
                    break;
                case 1: // Select Data Source
                    CameraPanel.SetActive(false);
                    VideoPanel.SetActive(false);
                    ModesPanel.SetActive(false);
                    DataSourcePanel.SetActive(true);
                    backButton.gameObject.SetActive(true);
                    // once the service is selected(url suffix depends on it) get mobiledgex url based on the protocol((rest/ws)> selected in editor)
                    appManager.SetConnection();
                    StatsButton.SetActive(false);
                    StatsPanel.SetActive(false);
                    networkManager.ClearStats();
                    Background.SetActive(true);
                    Logo.color = new Color(Logo.color.r, Logo.color.g, Logo.color.b, 1);
                    break;
                case 2:  // Service View (FaceDetection, Face Recognition ...)
                    ModesPanel.SetActive(false);
                    DataSourcePanel.SetActive(false);
                    backButton.gameObject.SetActive(true);
                    appManager.StartCV();
                    Logo.color = new Color(Logo.color.r, Logo.color.g, Logo.color.b, 0);
                    networkManager.ClearStats();
                    StatsButton.SetActive(true);
                    StatsPanel.SetActive(false);
                    if(AppManager.source == AppManager.DataSource.CAMERA)
                    {
                        Background.SetActive(false);
                    }
                    else
                    {
                        Background.SetActive(true);
                    }
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
                if (AppManager.level == 2)
                {
                    appManager.StopAllCoroutines();
                    appManager.ClearGUI();
                    AppManager.showGUI = false;
                    VideoPanel.GetComponentInChildren<VideoPlayer>().Stop(); // Reset Video
                    AppManager.urlFound = false; 
                    appManager.wsStarted = false;
                    appManager.webRequestsLock = true;
                    if(networkManager.client != null)
                    {
                        networkManager.client.tokenSource.Cancel();
                    }
                }
                    AppManager.level--;
            }
            UpdateUIBasedOnLevel(AppManager.level);
        }
    }
}
