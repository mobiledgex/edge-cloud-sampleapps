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
using System.Net;
using System.Net.Sockets;
using System.Linq;
using MobiledgeX;
using DistributedMatchEngine;
using System.Collections;
using UnityEngine.Networking;
using System.Collections.Concurrent;
using UnityEngine.UI;
using System.Text;
using System.Net.Http;

namespace MobiledgeXComputerVision {

    public class NetworkManager:MonoBehaviour
    {
        private MobiledgeXIntegration integration;
        private float avgFullProcessLatency;
        private float avgServerProcessingTime;
        private float avgNetworkOnlyLatency;
        private ConcurrentQueue<float> LatencyRollingAvgQueue = new ConcurrentQueue<float>();
        private ConcurrentQueue<float> ServerProcessingTimeRollingAvgQueue = new ConcurrentQueue<float>();
        private int networkLatencyCounter = 0;

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
        public MobiledgeXWebSocketClient client;
        public static bool showStats; 
        public enum ConnectionMode
        {
            Rest,
            WebSocket
        }
        public ConnectionMode connectionMode;

        private void Awake()
        {
            InvokeRepeating("GetNetworkOnlyLatency", 5, 10);
        }
        #region MonoBehaviour Callbacks
        IEnumerator Start()
        {
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
        async Task GetEDGE() {
            integration = new MobiledgeXIntegration();
            integration.UseWifiOnly(true);
            try
            {
                bool cloudletFound = await integration.RegisterAndFindCloudlet();
                if (cloudletFound)
                {
                    appManager.EnableInteraction();
                    ConnectedToEdgePanel.SetActive(true);
                }
            }
           
           catch(RegisterClientException) // In case we don't support the detected carrierName  (In the generated dme)
            {
                integration.UseWifiOnly(true);
                await integration.RegisterAndFindCloudlet();
            }
            catch (FindCloudletException) // GPS Location Error
            {
                EdgePanel.SetActive(true); // Disables User Input
                NotConnectedToEdgePanel.SetActive(true);
                ErrorReason.text = "Location Permission Denied";
                ErrorSolution.GetComponentInChildren<Text>().text = "Allow Location Permission in your settings & Restart the App";
                ErrorPanel.SetActive(true);
            }
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

            var temp = Time.realtimeSinceStartup;
            yield return www.SendWebRequest();
            LatencyCalculator(Time.realtimeSinceStartup - temp);
            UpdateStats();

            // isHttpError True on response codes greater than or equal to 400.
            // isNetworkError True on failure to resolve a DNS entry
            if (www.isNetworkError || www.isHttpError)
            {
                Debug.Log("Error sending Image to SERVER");
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
                if (networkLatencyCounter % 10 == 0)
                {
                    appManager.webRequestsLock = false;
                    networkLatencyCounter++;
                    GetNetworkOnlyLatency();
                    if (networkLatencyCounter > 10)
                    {
                        networkLatencyCounter = 0;
                    }
                }
                else
                {
                    appManager.webRequestsLock = true;
                }
                appManager.HandleServerResponse(www.downloadHandler.text);
            }
        }

        void LatencyCalculator(float requestLatency)
        {
            if(LatencyRollingAvgQueue.Count < 10)
            {
                LatencyRollingAvgQueue.Enqueue(requestLatency);
            }
            else
            {
                float t;
                LatencyRollingAvgQueue.TryDequeue(out t);
                LatencyRollingAvgQueue.Enqueue(requestLatency);
            }
            avgFullProcessLatency = LatencyRollingAvgQueue.Average();
        }

        public void ServerProcessingTimeCalculator(float requestLatency)
        {
            if (ServerProcessingTimeRollingAvgQueue.Count < 10)
            {
                ServerProcessingTimeRollingAvgQueue.Enqueue(requestLatency);
            }
            else
            {
                float t;
                ServerProcessingTimeRollingAvgQueue.TryDequeue(out t);
                ServerProcessingTimeRollingAvgQueue.Enqueue(requestLatency);
            }
            avgServerProcessingTime = ServerProcessingTimeRollingAvgQueue.Average();
        }

        public void ClearStats()
        {
            ServerProcessingTimeRollingAvgQueue = new ConcurrentQueue<float>();
            LatencyRollingAvgQueue = new ConcurrentQueue<float>();
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
                avgFullProcessLatencyText.text = (avgFullProcessLatency * 1000).ToString("f0") + " ms";
                avgServerProcessingTimeText.text = (avgServerProcessingTime).ToString("f0") + " ms";

            }
        }

       async void GetNetworkOnlyLatency()
        {
            HttpClient httpClient = new HttpClient();
            HttpRequestMessage request =new HttpRequestMessage(HttpMethod.Head,new Uri(UriBasedOnConnectionMode()));
            System.Diagnostics.Stopwatch timer = new System.Diagnostics.Stopwatch();
            timer.Start();
            HttpResponseMessage response = await httpClient.SendAsync(request);
            timer.Stop();
            TimeSpan timeTaken = timer.Elapsed;
            avgNetworkOnlyLatencyText.text = (timeTaken.Milliseconds).ToString("f0") + " ms";
            appManager.webRequestsLock = true;
        }
        #endregion
    }
}
