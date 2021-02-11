using UnityEngine;
using UnityEngine.UI;

namespace MobiledgeXComputerVision
{
    public class CameraManager : MonoBehaviour
    {
        public Button switchCameraButton;
        private string cameraName = "Back Camera";
        private int screenWidth = Screen.width;
        private int screenHeight = Screen.height;
        private Vector3 frontCameraRotation = new Vector3(0, 0, 180);
        private Vector3 backCameraRotation = new Vector3(0, 180, 0);
        private WebCamTexture webcamTexture;
        private RawImage image;
        private const int FBS = 50;

        void Start()
        {
            image = GetComponent<RawImage>();
            image.rectTransform.localRotation = Quaternion.Euler(backCameraRotation);
            if (Application.platform == RuntimePlatform.IPhonePlayer)
            {
                webcamTexture = new WebCamTexture(cameraName, screenWidth, screenHeight, FBS);
            }
            else
            {
                webcamTexture = new WebCamTexture(screenWidth, screenHeight, FBS);
            }
            image.texture = webcamTexture;
            webcamTexture.Play();
            if(Application.platform  == RuntimePlatform.IPhonePlayer)
            {
                switchCameraButton.onClick.AddListener(() =>
                {
                    webcamTexture.Stop();
                    if (cameraName == "Back Camera")
                    {
                        cameraName = "Front Camera";
                        image.rectTransform.localRotation = Quaternion.Euler(frontCameraRotation);
                    }
                    else
                    {
                        cameraName = "Back Camera";
                        image.rectTransform.localRotation = Quaternion.Euler(backCameraRotation);
                    }
                    webcamTexture = new WebCamTexture(cameraName, screenWidth, screenHeight, FBS);
                    image.texture = webcamTexture;
                    webcamTexture.Play();
                });
            }
            else
            {
                switchCameraButton.gameObject.SetActive(false);
            }
        }
    }
}
