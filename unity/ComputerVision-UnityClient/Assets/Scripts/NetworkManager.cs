using UnityEngine;
using System;
using System.Threading.Tasks;
using System.Net;
using System.Net.Sockets;
using MobiledgeX;
namespace MobiledgeXComputerVision {

    public class NetworkManager:MonoBehaviour
    {
        public AppManager appManager;
        MobiledgeXWebSocketClient client;
        MobiledgeXIntegration integration;
        public enum ConnectionMode
        {
            Rest,
            WebSocket
        }

        public ConnectionMode connectionMode;

        public async Task<string> UriBasedOnConnectionMode()
        {
            DistributedMatchEngine.AppPort appPort;
            string url;
            switch (connectionMode)
            {
                case ConnectionMode.WebSocket:
                    integration = new MobiledgeXIntegration();
                    await integration.RegisterAndFindCloudlet();
                    appPort = integration.GetAppPort(DistributedMatchEngine.LProto.L_PROTO_TCP);
                    url = integration.GetUrl("ws");
                    return url;
                case ConnectionMode.Rest:
                    integration = new MobiledgeXIntegration();
                    await integration.RegisterAndFindCloudlet();
                    appPort = integration.GetAppPort(DistributedMatchEngine.LProto.L_PROTO_TCP);
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
                print("Dequeued this message: " + msg);
                appManager.HandleServerRespone(msg);
            }
        }
    }
}
