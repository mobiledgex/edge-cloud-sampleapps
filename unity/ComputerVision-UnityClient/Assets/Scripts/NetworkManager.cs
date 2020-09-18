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

using UnityEngine;
using System;
using System.Threading.Tasks;
using System.Linq;
using MobiledgeX;
using DistributedMatchEngine;
using System.Collections;
using UnityEngine.Networking;
using System.Collections.Concurrent;
using UnityEngine.UI;
using System.Collections.Generic;

namespace MobiledgeXComputerVision {

    public class NetworkManager:MonoBehaviour
    {
        private MobiledgeXIntegration integration;
        private ConcurrentQueue<float> networkOnlyLatencyRollingAvgQueue = new ConcurrentQueue<float>();
        private ConcurrentQueue<float> fullLatencyRollingAvgQueue = new ConcurrentQueue<float>();

        public ConcurrentQueue<float> ServerProcessingTimeRollingAvgQueue = new ConcurrentQueue<float>();
        public AppManager appManager;
        public GameObject EdgePanel;
        public GameObject ErrorPanel;
        public Text ErrorReason;
        public Button ErrorSolution;
        public GameObject ConnectedToEdgePanel;
        public GameObject NotConnectedToEdgePanel;
        public Text avgServerProcessingTimeText;
        public Text avgFullProcessLatencyText;
        public Text avgNetworkOnlyLatencyText;
        public Text cloudletLocationText;
        public MobiledgeXWebSocketClient client;
        public static bool showStats;
        public List<string> regionsDmeList;
        public enum ConnectionMode
        {
            Rest,
            WebSocket
        }
        public ConnectionMode connectionMode;
        public static int regionIndex = 0;

        #region MonoBehaviour Callbacks
        IEnumerator Start()
        {
            regionsDmeList = new List<string>() { "wifi", "eu-mexdemo", "us-mexdemo", "jp-mexdemo", "kr-mexdemo" };
            integration = new MobiledgeXIntegration();

#if UNITY_EDITOR
            GetEDGE();
            yield break;
#endif
            if (Application.internetReachability == NetworkReachability.NotReachable)
            {
                EdgePanel.SetActive(true); // Disables User Input
                NotConnectedToEdgePanel.SetActive(true);
                ErrorReason.text = "Not Connected to the internet";
                ErrorSolution.GetComponentInChildren<Text>().text = "Restart the App and Connect to the Internet through Carrier Data.";
                ErrorPanel.SetActive(true);
            }
            yield return StartCoroutine(MobiledgeX.LocationService.EnsureLocation());
            GetEDGE();
        }
        private void Update()
        {
            if (client == null)
            {
                return;
            }
            var cqueue = client.receiveQueue;
            string msg;
            while (cqueue.TryPeek(out msg))
            {
                cqueue.TryDequeue(out msg);
                appManager.HandleServerResponse(msg);
            }

        }
        #endregion

        #region MobiledgeXComputerVision Functions
        public async Task GetEDGE() {
            ConnectedToEdgePanel.SetActive(false);
            NotConnectedToEdgePanel.SetActive(false);
            appManager.DisableInteraction();
            integration.UseWifiOnly(true);
            EdgePanel.SetActive(false); // Disables User Input
            NotConnectedToEdgePanel.SetActive(false);
            try
            {
                bool cloudletFound = await integration.RegisterAndFindCloudlet(regionsDmeList[regionIndex]+"."+MatchingEngine.baseDmeHost, MatchingEngine.defaultDmeRestPort);
                if (cloudletFound)
                {
                    appManager.EnableInteraction();
                    ConnectedToEdgePanel.SetActive(true);
                }
                StartCoroutine(GetCloudletCityName(integration.FindCloudletReply.cloudlet_location.longitude, integration.FindCloudletReply.cloudlet_location.latitude));
            }
           
           catch(RegisterClientException) // App Name Doesn't exist on the DME
            {
                EdgePanel.SetActive(true); // Disables User Input
                NotConnectedToEdgePanel.SetActive(true);
                ErrorReason.text = "CV App is not availabe in your region";
                ErrorSolution.GetComponentInChildren<Text>().text = "Change the region in Location Settings.";
                ErrorPanel.SetActive(true);
                appManager.EnableInteraction();
            }
            catch (FindCloudletException) // GPS Location Error
            {
                EdgePanel.SetActive(true); // Disables User Input
                NotConnectedToEdgePanel.SetActive(true);
                ErrorReason.text = "Either no App Instances in your Region or Application Location permissions are denied";
                ErrorSolution.GetComponentInChildren<Text>().text = "Update Location Settings in the Application.";
                ErrorPanel.SetActive(true);
                appManager.EnableInteraction();
            }
        }

        public void UpdateUserLocation(double longitude, double latitude)
        {
            integration.SetFallbackLocation(longitude, latitude);
            integration.useFallbackLocation = true;
            GetEDGE();
        }

        public void UpdateUserLocation()
        {
            integration.useFallbackLocation = false;
            GetEDGE();
        }

        public string UriBasedOnConnectionMode()
        {
            AppPort appPort;
            string url;
            switch (connectionMode)
            {
                case ConnectionMode.WebSocket:
                    appPort = integration.GetAppPort(LProto.L_PROTO_TCP);
                    url = integration.GetUrl("ws");
                    return url;
                case ConnectionMode.Rest:
                    appPort = integration.GetAppPort(LProto.L_PROTO_TCP);
                    url = integration.GetUrl("http");
                    return url;
                default:
                    return "";
            }
        }

        public async void StartWs(string url)
        {
            Uri uri = new Uri(url);
            client = new MobiledgeXWebSocketClient();
            if (client.isOpen())
            {
                
                client.Dispose();
                client = new MobiledgeXWebSocketClient();
            }
            await client.Connect(uri);
            appManager.wsStarted = true;
        }

        public void SendtoServer(byte[] imgBinary)
        {
            client.Send(imgBinary);
        }

        public IEnumerator SendImageToServer(byte[] imgBinary, string url)
        {
            appManager.webRequestsLock = false;
            WWWForm form = new WWWForm();
            form.AddBinaryData("image", imgBinary);
            UnityWebRequest www = UnityWebRequest.Post(url, form);
            www.timeout = 5; // Timeout in seconds

            System.Diagnostics.Stopwatch timer = new System.Diagnostics.Stopwatch();
            timer.Start();
            yield return www.SendWebRequest();
            timer.Stop();
            TimeSpan ts = timer.Elapsed;
            UpdateAverageLatency(fullLatencyRollingAvgQueue, (float)ts.TotalMilliseconds);
            UpdateStats();

            // isHttpError True on response codes greater than or equal to 400.
            // isNetworkError True on failure to resolve a DNS entry
            if (www.isNetworkError || www.isHttpError)
            {
                Debug.Log("Error sending Image to Server");
                Debug.Log(www.error);
                if (www.responseCode == 503)
                {
                    Debug.Log("Training data update in progress, Sending another request in 2 seconds.");
                    yield return new WaitForSeconds(2);
                    StartCoroutine(SendImageToServer(imgBinary, url));
                    yield break;
                }
                if (Application.internetReachability == NetworkReachability.NotReachable)
                {
                    Debug.LogError("Error. Your are not connected to the Internet.");
                }
                else
                {
                    yield return new WaitForEndOfFrame();
                    StartCoroutine(SendImageToServer(imgBinary, url));
                    yield break;
                }
            }
            else
            {
                StartCoroutine(GetNetworkOnlyLatency(url));
                appManager.HandleServerResponse(www.downloadHandler.text);
            }
        }

        public void ClearStats()
        {
            ServerProcessingTimeRollingAvgQueue = new ConcurrentQueue<float>();
            fullLatencyRollingAvgQueue = new ConcurrentQueue<float>();
            networkOnlyLatencyRollingAvgQueue = new ConcurrentQueue<float>();
            showStats = false;
        }

        public void ShowStats()
        {
                if (showStats)
                {
                    showStats = false;
                }
                else
                {
                    showStats = true;
                }
        }

        void UpdateStats()
        {
            if (showStats)
            {
                avgFullProcessLatencyText.text = getAverageLatency(fullLatencyRollingAvgQueue).ToString("f0") + " ms";
                avgServerProcessingTimeText.text = getAverageLatency(ServerProcessingTimeRollingAvgQueue).ToString("f0") + " ms";
                avgNetworkOnlyLatencyText.text = getAverageLatency(networkOnlyLatencyRollingAvgQueue).ToString("f0") + " ms";

            }
        }

        public void UpdateAverageLatency(ConcurrentQueue<float> queue, float requestLatency)
        {
            if (queue.Count < 100)
            {
                queue.Enqueue(requestLatency);
            }
            else
            {
                float t;
                queue.TryDequeue(out t);
                queue.Enqueue(requestLatency);
            }
        }

        private float getAverageLatency(ConcurrentQueue<float> queue)
        {
            if (queue.Count < 1) //InvalidOperationException 
            {
                return 0;
            }
            else
            {
                return queue.Average();
            }
        }

        public IEnumerator GetNetworkOnlyLatency( string url)
        {
            UnityWebRequest networkTest = UnityWebRequest.Head(url);
            System.Diagnostics.Stopwatch networkOnlyLatencyTimer = new System.Diagnostics.Stopwatch();
            networkOnlyLatencyTimer.Start();
            yield return networkTest.SendWebRequest();
            networkOnlyLatencyTimer.Stop();
            TimeSpan networkOnlyLatencyTimeSpan = networkOnlyLatencyTimer.Elapsed;
            UpdateAverageLatency(networkOnlyLatencyRollingAvgQueue, (float)networkOnlyLatencyTimeSpan.TotalMilliseconds);
            appManager.webRequestsLock = true;
        }


        public IEnumerator GetCloudletCityName(double longitude, double latitude)
        {
            UnityWebRequest cityNameRequest = UnityWebRequest.Get("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=" + latitude + "&longitude=" + longitude + "&localityLanguage=en");
            cityNameRequest.timeout = 5; // Timeout in seconds
            yield return cityNameRequest.SendWebRequest();
            if (!cityNameRequest.isNetworkError)
            {
                cloudletLocationText.text = (JsonUtility.FromJson<GetCloudletCityNameResponse>(cityNameRequest.downloadHandler.text).principalSubdivision);
            }
            else
            {
                cloudletLocationText.text = "Unknown";
            }
        }
               
        public class GetCloudletCityNameResponse // full response structure at https://www.bigdatacloud.com/geocoding-apis/free-reverse-geocode-to-city-api
        {
            public string principalSubdivision;
        }
        #endregion
    }
}
