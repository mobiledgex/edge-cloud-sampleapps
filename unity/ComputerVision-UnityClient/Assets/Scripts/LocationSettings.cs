using UnityEngine;
using UnityEngine.UI;

namespace MobiledgeXComputerVision
{
    public class LocationSettings : MonoBehaviour
    {
        public InputField LatitudeInput;
        public InputField LongitudeInput;
        public Button UpdateLocationButton;
        public Button UseDeviceLocationButton;
        public Dropdown RegionDropDownList;
        public NetworkManager networkManager;


        private void OnEnable()
        {
            RegionDropDownList.value = NetworkManager.regionIndex;
            UpdateLocationButton.onClick.AddListener(UpdateLocationSettings);
            UseDeviceLocationButton.onClick.AddListener(UseDeviceLocation);
            RegionDropDownList.onValueChanged.AddListener(SetRegion);
        }

       void UpdateLocationSettings()
        {
            double latitude, longitude;
            double.TryParse(LatitudeInput.text, out latitude);
            double.TryParse(LongitudeInput.text, out longitude);
            networkManager.UpdateUserLocation(longitude, latitude);
        }

        void UseDeviceLocation()
        {
            LatitudeInput.text = "";
            LongitudeInput.text = "";
            networkManager.UpdateUserLocation();
        }

        void SetRegion(int region)
        {
            NetworkManager.regionIndex = region;
            networkManager.GetEDGE();
        }
    }
}