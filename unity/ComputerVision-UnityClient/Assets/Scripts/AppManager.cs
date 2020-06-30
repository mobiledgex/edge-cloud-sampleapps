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
        public Texture nodeTexture;
        public static bool showGUI;
        int[][] faceDetectionRects;
        int[] faceRecognitionRect;
        static string faceRecognitionSubject;
        static float faceRecognitionConfidenceLevel;
        static @Object[] objectsDetected;
        float imgScalingFactor;
        public static int level = 0;
        public NetworkManager networkManager;
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

        static string url;
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
        public static bool urlFound;
        bool webRequestSent=true;

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

        public async Task SetConnection()
        {
            switch (NetworkManager.connectionMode)
            {
                case NetworkManager.ConnectionMode.WebSocket:
                    throw new Exception("Not Implemented Yet");
 
                case NetworkManager.ConnectionMode.Rest:
                    string uri = await networkManager.UriBasedOnConnectionMode();
                    url = uri + urlSuffix;
                    urlFound = true;
                    break;
            }
        }

        /// <summary>
        /// ImageSenderFlow Flow : Hide the GUI > CaptureScreenShot
        ///  > ShowGUI > Shrink Image > Send Image to Server > Handle Server Response > Repeat
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
            Debug.Log("ScreenShot taken");
            byte[] imgBinary = ShrinkAndEncode(source: texture, targetWidth: serviceMode == ServiceMode.FaceRecognition ? 500 : 240);
            Destroy(texture);
            Debug.Log("Image has been shrunk and encoded, Sending Image to Server");
            while (!urlFound && webRequestSent)
            {
                yield return null;
            }
            StartCoroutine(SendImageToServer(imgBinary));
        }
        
        IEnumerator SendImageToServer(byte[] imgBinary)
        {
            webRequestSent = false;
            WWWForm form = new WWWForm();
            form.AddBinaryData("image", imgBinary);
            UnityWebRequest www = UnityWebRequest.Post(url, form);
            www.timeout = 5; // Timeout in seconds
            yield return www.SendWebRequest();

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
                    StartCoroutine(SendImageToServer(imgBinary));
                    yield break;
                }
                if (Application.internetReachability == NetworkReachability.NotReachable)
                {
                    Debug.LogError("Error. Your are not connected to the Internet.");
                }
                else
                {
                    yield return new WaitForEndOfFrame();
                    StartCoroutine(SendImageToServer(imgBinary));
                    yield break;
                }
            }
            else
            {
                webRequestSent = true;
                Debug.Log("Success sending Image to SERVER, Response Received ");
                Debug.Log("ServerResponse :\n" + www.downloadHandler.text);
                HandleServerRespone(www.downloadHandler.text);
            }
        }
   
        public void HandleServerRespone(string response)
        {
            switch (serviceMode)
            {
                case ServiceMode.FaceDetection:
                    FaceDetectionResponse faceDetectionResponse = Messaging<FaceDetectionResponse>.Deserialize(response);
                    Debug.Log("Success : " + faceDetectionResponse.success);
                    Debug.Log("server_processing_time : " + faceDetectionResponse.server_processing_time);
                    showGUI = faceDetectionResponse.success;
                    if (faceDetectionResponse.success)
                    {
                        Debug.Log("Number of faces : " + faceDetectionResponse.rects.Length);
                        Debug.Log("Number of Rect Dims : " + faceDetectionResponse.rects[0].Length);
                        faceDetectionRects = faceDetectionResponse.rects;
                    }
                    else
                    {
                        faceDetectionRects = null;
                    }
                    break;

                case ServiceMode.FaceRecognition:
                    FaceRecognitionResponse faceRecognitionResponse = Messaging<FaceRecognitionResponse>.Deserialize(response);
                    Debug.Log("Success : " + faceRecognitionResponse.success);
                    Debug.Log("server_processing_time : " + faceRecognitionResponse.server_processing_time);
                    if (faceRecognitionResponse.success && faceRecognitionResponse.confidence < 105)
                    {
                        Debug.Log("Detected Face Name : " + faceRecognitionResponse.subject);
                        Debug.Log("Recognition C.L. : " + faceRecognitionResponse.confidence);
                        faceRecognitionRect = faceRecognitionResponse.rect;
                        faceRecognitionSubject = faceRecognitionResponse.subject;
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
                    Debug.Log("Success : " + objectDetectionResponse.success);
                    Debug.Log("server_processing_time : " + objectDetectionResponse.server_processing_time);
                    Debug.Log("Gpu Support : " + objectDetectionResponse.gpu_support);
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
                        GUI.DrawTexture(new Rect(faceDetectionRects[i][0] * imgScalingFactor, faceDetectionRects[i][1] * imgScalingFactor, width, height), rectTexture, ScaleMode.StretchToFill, true, width / height);
                    }
                    break;
                    
                case ServiceMode.FaceRecognition:
                    height = imgScalingFactor* (faceRecognitionRect[3] - faceRecognitionRect[1]);
                    width = imgScalingFactor * (faceRecognitionRect[2] - faceRecognitionRect[0]);
                    GUI.DrawTexture(new Rect(faceRecognitionRect[0] * imgScalingFactor, faceRecognitionRect[1] * imgScalingFactor, width, height), rectTexture, ScaleMode.StretchToFill, true, width/height);
                    TextStyle.normal.textColor = getConfidenceLevelColorFR(faceRecognitionConfidenceLevel);
                    TextStyle.fontSize = 50;
                    TextStyle.fontStyle = FontStyle.Bold;
                    GUI.Label(new Rect(faceRecognitionRect[0] * imgScalingFactor, faceRecognitionRect[1] * imgScalingFactor + 50, width, 100), new GUIContent(faceRecognitionSubject), TextStyle);
                    break;

                case ServiceMode.ObjectDetection:
                    foreach (@Object obj in objectsDetected)
                    {
                        height = imgScalingFactor * (obj.rect[3] - obj.rect[1]);
                        width = imgScalingFactor * (obj.rect[2] - obj.rect[0]);
                        GUI.DrawTexture(new Rect((obj.rect[0] * imgScalingFactor), (obj.rect[1] * imgScalingFactor), width, height), rectTexture, ScaleMode.StretchToFill, true, width / height);
                        TextStyle.normal.textColor = getConfidenceLevelColorOD(obj.confidence);
                        TextStyle.fontSize = 30;
                        TextStyle.fontStyle = FontStyle.Bold;
                        GUI.Label(new Rect(obj.rect[0] * imgScalingFactor, (obj.rect[1] * imgScalingFactor) + 50, width, 100), new GUIContent(obj.@class), TextStyle);
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
            if (faceRecognitionConfidenceLevel < 0.3)
            {
                return Color.red;
            }
            if (faceRecognitionConfidenceLevel < 0.5)
            {
                return new Color(1, 0.5f, 0);
            }
            if (faceRecognitionConfidenceLevel < .8)
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
            if (faceRecognitionConfidenceLevel < 10)
            {
                return Color.green;
            }
            if (faceRecognitionConfidenceLevel < 50)
            {
                return new Color(0.5f, 1, 0);
            }
            if (faceRecognitionConfidenceLevel < 100)
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
