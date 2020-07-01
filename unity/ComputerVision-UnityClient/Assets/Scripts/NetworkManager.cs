using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;
using System.Threading.Tasks;

namespace MobiledgeXComputerVision {

    public class NetworkManager:MonoBehaviour
    {
        public AppManager appManager;
        public enum ConnectionMode
        {
            Rest,
            WebSocket
        }

        public static ConnectionMode connectionMode;

        public async Task<string> UriBasedOnConnectionMode()
        {
            switch (connectionMode)
            {
                case ConnectionMode.WebSocket:
                    throw new Exception("Not Implemented Yet");
                default: 
                case ConnectionMode.Rest:
                    throw new Exception("Add Mobiledgex package : https://github.com/mobiledgex/edge-cloud-sdk-unity ");
            }
        }
    }
}
