using UnityEngine;
using UnityEngine.UI;

namespace MobiledgeXComputerVision
{
    public class LocationSettings : MonoBehaviour
    {
        public InputField LatitudeInput;
        public InputField LongitudeInput;
        public Button UpdateLocationButton;
        public Toggle UseDeviceLocationToggle;
        public Dropdown RegionDropDownList;
        public GameObject SetupLocationLabel;
        public GameObject SetupLocationField;
        public NetworkManager networkManager;


        private void OnEnable()
        {
            RegionDropDownList.value = NetworkManager.regionIndex;
            UpdateLocationButton.onClick.AddListener(UpdateLocationSettings);
            UseDeviceLocationToggle.onValueChanged.AddListener(UseDeviceLocation);
            RegionDropDownList.onValueChanged.AddListener(SetRegion);
        }

       void UpdateLocationSettings()
        {
            double latitude, longitude;
            double.TryParse(LatitudeInput.text, out latitude);
            double.TryParse(LongitudeInput.text, out longitude);
            networkManager.UpdateUserLocation(longitude, latitude);
            UseDeviceLocationToggle.isOn = false;
        }

        void UseDeviceLocation(bool useDeviceLocation)
        {
            if (useDeviceLocation)
            {
                LatitudeInput.text = "";
                LongitudeInput.text = "";
                networkManager.UpdateUserLocation();
                SetupLocationField.SetActive(false);
                SetupLocationLabel.SetActive(false);
            }
            else
            {
                SetupLocationField.SetActive(true);
                SetupLocationLabel.SetActive(true);
            }

        }

        void SetRegion(int region)
        {
            NetworkManager.regionIndex = region;
            networkManager.GetEDGE();
        }
    }
}