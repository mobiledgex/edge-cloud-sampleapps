using System.Collections;
using System.Collections.Generic;
using UnityEngine;
//using MobiledgeX;
using System;
using System.Threading.Tasks;

namespace MobiledgeXComputerVision {

    public class NetworkManager:MonoBehaviour
    {
        //MobiledgeXIntegration integration;
        public AppManager appManager;
        public enum ConnectionMode
        {
            Rest,
            WebSocket
        }
        public static ConnectionMode connectionMode;

        public async Task<string> UriBasedOnConnectionMode()
        {
            //integration = new MobiledgeXIntegration();
            //await integration.RegisterAndFindCloudlet();

            switch (connectionMode)
            {
                case ConnectionMode.WebSocket:
                    throw new Exception("Not Implemented Yet");
                default:
                case ConnectionMode.Rest:
                    //integration.GetAppPort(DistributedMatchEngine.LProto.L_PROTO_HTTP);
                    //return integration.GetUrl("http");
                    throw new Exception("Add Mobiledgex package : https://github.com/mobiledgex/edge-cloud-sdk-unity ");
            }
        }

    }
}
