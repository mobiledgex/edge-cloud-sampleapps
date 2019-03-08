using UnityEngine;
using UnityEngine.Android;

public class PermissionScript : MonoBehaviour
{
  void Start()
  {
#if UNITY_ANDROID
    if (Permission.HasUserAuthorizedPermission(Permission.FineLocation))
    {
      // The user authorized use of the FineLocation.
    }
    else
    {
      // Ask for permission or proceed without the functionality enabled.
      Permission.RequestUserPermission(Permission.FineLocation);
    }
#endif
  }
}
