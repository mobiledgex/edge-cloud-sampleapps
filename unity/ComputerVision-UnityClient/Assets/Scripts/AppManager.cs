using UnityEngine;
using System.Collections;
using UnityEngine.UI;
using UnityEngine.Networking;
using System.Threading.Tasks;
namespace MobiledgeXComputerVision
{
    public class AppManager : MonoBehaviour
    {
        public RawImage rawImage;
        public Texture rectTexture;
        public Texture nodeTexture;
        public static bool showGUI;
        float[][][] poseDetectionNodes;
        int[][] faceDetectionRects;
        int[] faceRecognitionRect;
        static string faceRecognitionSubject;
        static float faceRecognitionConfidenceLevel;
        static @Object[] objectsDetected;
        float imgScalingFactor;
        public static int level = 0;
        bool serviceAlreadyStarted
        {
            get
            {
                switch (serviceMode)
                {

                    case ServiceMode.FaceRecognition:
                        return faceRecognitionRect == null ? false : true;
                    case ServiceMode.PoseDetection:
                        return poseDetectionNodes == null ? false : true;
                    case ServiceMode.ObjectDetection:
                        return objectsDetected == null ? false : true;
                    default:
                    case ServiceMode.FaceDetection:
                        return faceDetectionRects == null ? false : true;
                }

            }
        }
        static string urlSuffix
        {
            get
            {
                switch (serviceMode)
                {
                    case ServiceMode.FaceRecognition:
                        return "/recognizer/predict/";
                    case ServiceMode.PoseDetection:
                        return "/openpose/detect/";
                    case ServiceMode.ObjectDetection:
                        return "/object/detect/";
                    default:
                    case ServiceMode.FaceDetection:
                        return "/detector/detect/";
                }
            }
        }
        static string url;
        public enum ServiceMode
        {
            FaceDetection,
            FaceRecognition,
            PoseDetection,
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



        #region Monobehaviour callbacks

        void OnGUI()
        {
            if (showGUI)
            {
                DrawRectangles();
            }
        }

        #endregion

        public IEnumerator AppFlow()
        {
            showGUI = false;
            yield return new WaitForEndOfFrame();
            Texture2D screenShot = TakeSnapshot();
            showGUI = serviceAlreadyStarted;
            byte[] imgBinary = ShrinkAndEncode(source: screenShot, targetWidth: serviceMode == ServiceMode.FaceRecognition ? 500 : 240);
            SendImageToServer(imgBinary);
            StartCoroutine(AppFlow());
        }

        public static async Task SetURL()
        {
            NetworkManager networkManager = new NetworkManager();
            networkManager.connectionMode = NetworkManager.ConnectionMode.Rest;
            string uri = await networkManager.UriBasedOnConnectionMode();
            url = uri + urlSuffix;
        }

        Texture2D TakeSnapshot()
        {
            int width = Screen.width;
            int height = Screen.height;
            Texture2D texture = new Texture2D(width, height, TextureFormat.RGB24, true);
            texture.ReadPixels(new Rect(0, 0, width, height), 0, 0);
            texture.Apply();
            return texture;
        }

        IEnumerator SendImageToServer(byte[] imgBinary)
        {
            WWWForm form = new WWWForm();
            form.AddBinaryData("image", imgBinary);
            UnityWebRequest www = UnityWebRequest.Post(url, form);
            yield return www.SendWebRequest();
            if (www.isNetworkError || www.isHttpError)
            {
                Debug.Log(www.error);
                if (Application.internetReachability == NetworkReachability.NotReachable)
                {
                    Debug.Log("Error. Your are not connected to the Internet");
                }
                else
                {
                    StartCoroutine(SendImageToServer(imgBinary));
                }
            }
            else
            {
                if (www.responseCode == 200)
                {
                    Debug.Log("ServerResponse :\n" + www.downloadHandler.text);
                    HandleServerRespone(www.downloadHandler.text);
                }
                else
                {
                    if (serviceMode == ServiceMode.PoseDetection && www.responseCode == 501)
                    {
                        Debug.LogError("Pose Detection isn't supported on this server since it this server doesn't have GPU support ");
                    }
                    if (serviceMode == ServiceMode.FaceDetection && www.responseCode == 503)
                    {
                        Debug.Log("Training data update in progress, Sending another request in 2 seconds");
                        yield return new WaitForSeconds(2);
                        StartCoroutine(SendImageToServer(imgBinary));
                    }
                }
            }
        }

        void HandleServerRespone(string response)
        {
            switch (serviceMode)
            {

                case ServiceMode.FaceDetection:

                    FaceDetectionResponse faceDetectionResponse = Messaging<FaceDetectionResponse>.Deserialize(response);
                    print("Success : " + faceDetectionResponse.success);
                    print("server_processing_time : " + faceDetectionResponse.server_processing_time);
                    if (faceDetectionResponse.success)
                    {
                        print("Number of faces : " + faceDetectionResponse.rects.Length);
                        print("Number of Rect Dims : " + faceDetectionResponse.rects[0].Length);
                        faceDetectionRects = faceDetectionResponse.rects;
                        showGUI = true;
                    }
                    break;
                case ServiceMode.FaceRecognition:
                    FaceRecognitionResponse faceRecognitionResponse = Messaging<FaceRecognitionResponse>.Deserialize(response);
                    print("Success : " + faceRecognitionResponse.success);
                    print("server_processing_time : " + faceRecognitionResponse.server_processing_time);
                    print("Detected Face Name : " + faceRecognitionResponse.subject);
                    print("Recognition C.L. : " + faceRecognitionResponse.confidence);
                    if (faceRecognitionResponse.success && faceRecognitionResponse.confidence < 105)
                    {
                        faceRecognitionRect = faceRecognitionResponse.rect;
                        faceRecognitionSubject = faceRecognitionResponse.subject;
                        faceRecognitionConfidenceLevel = faceRecognitionResponse.confidence;
                        showGUI = true;
                    }
                    break;
                case ServiceMode.PoseDetection:
                    // not implemented yet
                    break;
                case ServiceMode.ObjectDetection:
                    ObjectDetectionResponse objectDetectionResponse = Messaging<ObjectDetectionResponse>.Deserialize(response);
                    print("Success : " + objectDetectionResponse.success);
                    print("server_processing_time : " + objectDetectionResponse.server_processing_time);
                    print("Gpu Support : " + objectDetectionResponse.gpu_support);
                    if (objectDetectionResponse.success)
                    {
                        objectsDetected = objectDetectionResponse.objects;
                        showGUI = true;
                    }
                    break;
            }

        }

        void DrawRectangles()
        {
            float height = 0;
            float width = 0;
            switch (serviceMode)
            {
                case ServiceMode.FaceDetection:
                    for (int i = 0; i < faceDetectionRects.Length; i++)
                    {
                        height = (faceDetectionRects[i][3] * imgScalingFactor) - (faceDetectionRects[i][1] * imgScalingFactor);
                        width = (faceDetectionRects[i][2] * imgScalingFactor) - (faceDetectionRects[i][0] * imgScalingFactor);
                        GUI.DrawTexture(new Rect(faceDetectionRects[i][0] * imgScalingFactor, faceDetectionRects[i][1] * imgScalingFactor, width, height), rectTexture, ScaleMode.StretchToFill, true, width / height);
                    }
                    break;
                case ServiceMode.FaceRecognition:

                    height = (faceRecognitionRect[3] - faceRecognitionRect[1]) * imgScalingFactor;
                    width = (faceRecognitionRect[2] - faceRecognitionRect[0]) * imgScalingFactor;
                    GUI.DrawTexture(new Rect(faceRecognitionRect[0] * imgScalingFactor, faceRecognitionRect[1] * imgScalingFactor, width, height), rectTexture);
                    GUIStyle TextStyle = new GUIStyle();
                    TextStyle.normal.textColor = getConfidenceLevelColorFD(faceRecognitionConfidenceLevel);
                    print(TextStyle.normal.textColor.ToString());
                    TextStyle.fontSize = 50;
                    TextStyle.fontStyle = FontStyle.Bold;
                    GUI.Label(new Rect(faceRecognitionRect[0] * imgScalingFactor, faceRecognitionRect[1] * imgScalingFactor + 50, width, 100), new GUIContent(faceRecognitionSubject), TextStyle);
                    break;

                case ServiceMode.PoseDetection:
                    // not implemented yet
                    break;
                case ServiceMode.ObjectDetection:
                    foreach (@Object obj in objectsDetected)
                    {
                        height = obj.rect[3] * 2 - obj.rect[1] * imgScalingFactor;
                        width = obj.rect[2] * 2 - obj.rect[0] * imgScalingFactor;
                        GUI.DrawTexture(new Rect(obj.rect[0] * imgScalingFactor, obj.rect[1] * imgScalingFactor, width, height), rectTexture);
                        GUIStyle TxtStyle = new GUIStyle();
                        TxtStyle.normal.textColor = getConfidenceLevelColorOD(obj.confidence);
                        TxtStyle.fontSize = 30;
                        TxtStyle.fontStyle = FontStyle.Bold;
                        GUI.Label(new Rect(obj.rect[0] * imgScalingFactor, (obj.rect[1] * imgScalingFactor) + 50, width, 100), new GUIContent(obj.@class), TxtStyle);
                    }
                    break;
            }

        }

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
            //string filepath = Path.Combine(Application.streamingAssetsPath, "ScaledSCREEN.jpg"); // fixme for testing only delete before publishing
            //File.WriteAllBytes(filepath, bytes);
            //Debug.Log("img Scaled");
            return bytes;
        }

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

        Color getConfidenceLevelColorFD(float confidenceLevel)
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

