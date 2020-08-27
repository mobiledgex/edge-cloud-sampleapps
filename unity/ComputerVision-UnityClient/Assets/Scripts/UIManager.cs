/**
 * Copyright 2020 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        public VideoClip objectsVideo;
        public VideoClip faceVideo;

        #region MonoBehaviour Callbacks
        private void Start()
        {
            backButton.onClick.AddListener(BackButton);
        }
        #endregion

        #region MobiledgeXComputerVision Functions
        public void SetMode(int modeSelected)
        {
            switch (modeSelected)
            {
                case 0:
                    AppManager.serviceMode = AppManager.ServiceMode.FaceDetection;
                    VideoPanel.GetComponentInChildren<VideoPlayer>().clip = faceVideo;
                    break;
                case 1:
                    AppManager.serviceMode = AppManager.ServiceMode.FaceRecognition;
                    VideoPanel.GetComponentInChildren<VideoPlayer>().clip = faceVideo;
                    break;
                case 2:
                    AppManager.serviceMode = AppManager.ServiceMode.ObjectDetection;
                    VideoPanel.GetComponentInChildren<VideoPlayer>().clip = objectsVideo;
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
        #endregion
    }
}
