using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using UnityEngine.Networking;
using System.Threading.Tasks;
using System;

namespace MobiledgeXComputerVision
{
    public class AppManager : MonoBehaviour
    {
        public RawImage rawImage;
        public Texture rectTexture;
        public static bool showGUI;
        int[][] faceDetectionRects;
        int[] faceRecognitionRect;
        static string faceRecognitionSubject;
        static float faceRecognitionConfidenceLevel;
        static @Object[] objectsDetected;
        float imgScalingFactor;
        public static int level = 0;
        public NetworkManager networkManager;
        public Image NerdStatsPanel;
        bool serviceAlreadyStarted
        {
            get
            {
                switch (serviceMode)
                {
                    case ServiceMode.FaceRecognition:
                        return faceRecognitionRect == null ? false : true;
                    case ServiceMode.ObjectDetection:
                        return objectsDetected == null ? false : true;
                    case ServiceMode.FaceDetection:
                        return faceDetectionRects == null ? false : true;
                    default:
                        return false;
                }
            }
        }

        public static string urlSuffix
        {
            get
            {
                switch (serviceMode)
                {
                    case ServiceMode.FaceRecognition:
                        return "/recognizer/predict/";
                    case ServiceMode.ObjectDetection:
                        return "/object/detect/";
                    case ServiceMode.FaceDetection:
                        return "/detector/detect/";
                    default:
                        return "";
                }
            }
        }

        static string url; // connection url = uri>> SetConnection () >> (networkManager.UriBasedOnConnectionMode()) + urlSuffix
        public enum ServiceMode
        {
            FaceDetection,
            FaceRecognition,
            ObjectDetection
        }

        public enum DataSource
        {
            VIDEO,
            CAMERA,
            nReal
        }

        public static ServiceMode serviceMode;
        public static DataSource source;
        public static bool urlFound; // trigger indicating RegisterAndFindCloudlet and  GetUrl() occured
        public bool webRequestsLock = true; // to control the flow of sending webrequest
        public bool wsStarted; // trigger indicating wether websocket connection intialized or not

        #region Monobehaviour callbacks

        void OnGUI()
        {
            if (showGUI)
            {
                DrawRectangles();
            }
        }

        #endregion

        public void StartCV()
        {
            StartCoroutine(ImageSenderFlow());
        }

        public void SetConnection()
        {
            string uri =  networkManager.UriBasedOnConnectionMode();
            switch (networkManager.connectionMode)
            {
                case NetworkManager.ConnectionMode.WebSocket:
                    url = uri +"/ws" +urlSuffix;
                    if (serviceMode == ServiceMode.ObjectDetection) // PoseDetection Server have GPU
                    {
                        url = "ws://posedetection.defaultedge.mobiledgex.net:8008/ws/object/detect/";
                    }
                    networkManager.StartWs(url);
                    urlFound = true;
                    break;
                case NetworkManager.ConnectionMode.Rest:
                    url = uri + urlSuffix;
                    //url = "http://posedetection.defaultedge.mobiledgex.net:8008" + urlSuffix;
                    if (serviceMode == ServiceMode.ObjectDetection) // PoseDetection Server have GPU
                    {
                        url = "http://posedetection.defaultedge.mobiledgex.net:8008/object/detect/";
                    }
                    urlFound = true;
                    break;
                default:
                    throw new Exception("Connection mode is not sat in UnityEditor");
            }
        }

        /// <summary>`
        /// ImageSenderFlow Flow : Hide the GUI > CaptureScreenShot
        ///  > ShowGUI > Shrink Image >
        ///  Based on Connection Mode > WebSocket Case > Add image binary to the socket queue > OnReceive > Handle Server Response
        ///                           >  Rest Case > Send Image to Server > Handle Server Response > Repeat
        /// </summary>
        IEnumerator ImageSenderFlow()
        {
            showGUI = false;
            yield return new WaitForEndOfFrame();
            int width = Screen.width;
            int height = Screen.height;
            Texture2D texture = new Texture2D(width, height, TextureFormat.RGB24, true);
            texture.ReadPixels(new Rect(0, 0, width, height), 0, 0);
            showGUI = serviceAlreadyStarted;
            texture.Apply();
            byte[] imgBinary = ShrinkAndEncode(source: texture, targetWidth: serviceMode == ServiceMode.FaceRecognition ? 500 : 240);
            Destroy(texture);
            while (!urlFound)
            {
                yield return null;
            }
            switch (networkManager.connectionMode)
            {
                case NetworkManager.ConnectionMode.Rest:
                    while (!webRequestsLock)
                    {
                        yield return null;
                    }
                    StartCoroutine(networkManager.SendImageToServer(imgBinary, url));
                    break;
                case NetworkManager.ConnectionMode.WebSocket:
                    while (!wsStarted)
                    {
                        //print("ws not started yet");
                        yield return null;
                    }
                    networkManager.SendtoServer(imgBinary);
                    break;
            }
        }


        private void Update()
        {
            if (!showGUI)
            {
                NerdStatsPanel.enabled = false;
            }
            else
            {
                NerdStatsPanel.enabled = true;
            }
        }
        public void HandleServerRespone(string response)
        {
            switch (serviceMode)
            {
                case ServiceMode.FaceDetection:
                    FaceDetectionResponse faceDetectionResponse = Messaging<FaceDetectionResponse>.Deserialize(response);
                    networkManager.ServerProcessingTimeCalculator(faceDetectionResponse.server_processing_time);
                    showGUI = faceDetectionResponse.success;
                    if (faceDetectionResponse.success)
                    {
                        faceDetectionRects = faceDetectionResponse.rects;
                    }
                    else
                    {
                        faceDetectionRects = null;
                    }
                    break;

                case ServiceMode.FaceRecognition:
                    FaceRecognitionResponse faceRecognitionResponse = Messaging<FaceRecognitionResponse>.Deserialize(response);
                    networkManager.ServerProcessingTimeCalculator(faceRecognitionResponse.server_processing_time);
                    if (faceRecognitionResponse.success)
                    {
                        faceRecognitionRect = faceRecognitionResponse.rect;
                        faceRecognitionSubject =  faceRecognitionResponse.confidence < 120 ?faceRecognitionResponse.subject: "Unknown";
                        faceRecognitionConfidenceLevel = faceRecognitionResponse.confidence;
                        showGUI = faceRecognitionResponse.success;
                    }
                    else
                    {
                        faceRecognitionRect = null;
                    }
                    break;
                case ServiceMode.ObjectDetection:
                    ObjectDetectionResponse objectDetectionResponse = Messaging<ObjectDetectionResponse>.Deserialize(response);
                    networkManager.ServerProcessingTimeCalculator(objectDetectionResponse.server_processing_time);
                    showGUI = objectDetectionResponse.success;
                    if (objectDetectionResponse.success)
                    {
                        objectsDetected = objectDetectionResponse.objects;
                    }
                    else
                    {
                        objectsDetected = null;
                    }
                    break;
            }
            StartCoroutine(ImageSenderFlow());
        }

        void DrawRectangles()
        {
            float height = 0;
            float width = 0;
            GUIStyle TextStyle = new GUIStyle();
            switch (serviceMode)
            {
                case ServiceMode.FaceDetection:
                    for (int i = 0; i < faceDetectionRects.Length; i++)
                    {
                        height = imgScalingFactor * (faceDetectionRects[i][3] - faceDetectionRects[i][1]);
                        width = imgScalingFactor * (faceDetectionRects[i][2] - faceDetectionRects[i][0]);
                        GUI.DrawTexture(new Rect(faceDetectionRects[i][0] * imgScalingFactor, faceDetectionRects[i][1] * imgScalingFactor, width, height), rectTexture, ScaleMode.ScaleToFit, true, width / height);
                    }
                    break;                   
                case ServiceMode.FaceRecognition:
                    height = imgScalingFactor * (faceRecognitionRect[3] - faceRecognitionRect[1]);
                    width = imgScalingFactor * (faceRecognitionRect[2] - faceRecognitionRect[0]);
                    GUI.DrawTexture(new Rect((faceRecognitionRect[0] * imgScalingFactor), faceRecognitionRect[1] * imgScalingFactor, width, height), rectTexture, ScaleMode.StretchToFill, true, width / height);
                    TextStyle.normal.textColor = getConfidenceLevelColorFR(faceRecognitionConfidenceLevel);
                    TextStyle.fontSize = 50;
                    TextStyle.fontStyle = FontStyle.Bold;
                    GUI.Label(new Rect((faceRecognitionRect[0] * imgScalingFactor), faceRecognitionRect[1] * imgScalingFactor, width, 100), new GUIContent(faceRecognitionSubject), TextStyle);
                    break;
                case ServiceMode.ObjectDetection:
                    foreach (@Object obj in objectsDetected)
                    {
                        height = imgScalingFactor * (obj.rect[3] - obj.rect[1]);
                        width = imgScalingFactor * (obj.rect[2] - obj.rect[0]);
                        GUI.DrawTexture(new Rect((obj.rect[0] * imgScalingFactor), (obj.rect[1] * imgScalingFactor), width, height), rectTexture, ScaleMode.StretchToFill, true, width / height);
                        TextStyle.normal.textColor = getConfidenceLevelColorOD(obj.confidence);
                        TextStyle.fontSize = 50;
                        TextStyle.fontStyle = FontStyle.Bold;
                        GUI.Label(new Rect(obj.rect[0] * imgScalingFactor, (obj.rect[1] * imgScalingFactor) -100, width, 100), new GUIContent(obj.@class), TextStyle);
                    }
                    break;
            }
        }

        public void ClearGUI()
        {
            faceDetectionRects = null;
            faceRecognitionRect = null;
            objectsDetected = null;
        }

        /// <summary>
        /// Shrinks the screen shot taken to the supplied TargetWidth, then encode the new texture a JPG format and returns the binary array
        /// Shrinking happens by organizing the pixels of the source img into the new scaled texture using the normalized UV map
        /// </summary>
        /// <param name="source">Screen Shot Texture</param>
        /// <param name="targetWidth"></param>
        /// <returns> shrank image binary </returns>
        byte[] ShrinkAndEncode(Texture2D source, int targetWidth)
        {
            imgScalingFactor = source.width / targetWidth;
            int targetHeight = Mathf.FloorToInt(source.height / imgScalingFactor);
            Texture2D scaledTex = new Texture2D(targetWidth, targetHeight, source.format, true);
            Color[] pixelsColorArray = scaledTex.GetPixels(0);
            float xRatio = ((float)1 / source.width) * ((float)source.width / targetWidth);
            float yRatio = ((float)1 / source.height) * ((float)source.height / targetHeight);
            for (int px = 0; px < pixelsColorArray.Length; px++)
            {
                pixelsColorArray[px] = source.GetPixelBilinear(xRatio * ((float)px % targetWidth), yRatio * ((float)Mathf.Floor(px / targetWidth)));
            }
            scaledTex.SetPixels(pixelsColorArray, 0);
            scaledTex.Apply();
            byte[] bytes = scaledTex.EncodeToJPG();
            Destroy(scaledTex);
            return bytes;
        }

        /// <summary>
        /// Returns the color of the Detected Object Name Text
        /// </summary>
        /// <param name="confidenceLevel"></param>
        /// <returns><returns RGB Color/returns>
        Color getConfidenceLevelColorOD(float confidenceLevel)
        {
            if (confidenceLevel < 0.3)
            {
                return Color.red;
            }
            if (confidenceLevel < 0.5)
            {
                return new Color(1, 0.5f, 0);
            }
            if (confidenceLevel < .8)
            {
                return new Color(0.5f, 1f, 0);
            }
            else
            {
                return Color.green;
            }
        }

        /// <summary>
        /// Returns the color of FaceRecogintion Subject Text 
        /// </summary>
        /// <param name="confidenceLevel"></param>
        /// <returns>returns RGB Color</returns>
        Color getConfidenceLevelColorFR(float confidenceLevel)
        {
            if (confidenceLevel < 10)
            {
                return Color.green;
            }
            if (confidenceLevel < 50)
            {
                return new Color(0.5f, 1, 0);
            }
            if (confidenceLevel < 100)
            {
                return new Color(1, 0.5f, 0);
            }
            else
            {
                return Color.red;
            }
        }
    }
}
