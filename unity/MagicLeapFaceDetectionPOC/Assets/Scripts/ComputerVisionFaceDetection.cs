/*
 * Copyright 2012 ZXing.Net authors
 * 
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System;
using System.Threading;

using UnityEngine;

using ZXing;
using ZXing.QrCode;

using TMPro;

using UnityEngine.XR.MagicLeap;
using System.Net.Http;
using System.Threading.Tasks;

using CompanionApp;
using System.IO;
using System.Collections.Generic;
using System.Runtime.Serialization.Json;

enum AppMode
{
  QrCodeScanning,
  CompanionAppInfo,
  FaceDetectionReady,
}

public class ComputerVisionFaceDetection : MonoBehaviour
{
  private const string TAG = "ComputerVisionFaceDetection";
  // Texture for encoding test
  public Texture2D encoded;

  // For lines. Here's one material:
  public Material lineMat;
  public static FaceDetectionRects faceDetectionRects { get; set; }

  // RenderTexture(?) for Camera video stream to update.
  Material monitorMaterial;
  Material barcodeBufferMaterial;
  public GameObject parentCam;

  private Thread qrThread;

  private Color32[] c;
  private int W, H; // Texture width and height (pixels)
  private int ImageWidth, ImageHeight; // Image to send to server width and height (pixels)

  private Rect screenRect;

  private bool isQuit;

  public string CompanionDeviceHost;
  private bool shouldEncodeNow;

  GameObject cameraMonitor;
  GameObject cameraSubtitle;
  GameObject barcodeBuffer;
  TextMeshPro tmpro;

  // Some render targets to extract pixels for Camera.
  RenderTexture tmp;
  Texture2D nt;

  private Color[] green;

  HttpClient httpClient;
  private uint DEFAULT_REST_TIMEOUT_MS = 10000;

  AppMode appMode = AppMode.CompanionAppInfo; // Skip QR Code.
  public GameObject CompanionDeviceManagerGo;
  CompanionDeviceManager CompanionDeviceManager;
  public GameObject facedetectionUIGo;
  FaceDetectionUI faceDetectionUI;

  // Register device parameters as empty strings, to be filled in inside Unity/UI or CompanionDevice utility:
  public string ui_user_name = "";
  public string ui_user_email = "";
  public string ui_user_id = "";
  // This one should be supplied by a /device/list call from an authenticated session.
  public string device_uuid = "";

  Task<List<Rect>> faceDetectionRectsTask;

  void OnEnable()
  {

    Logger.D(TAG, "Called OnEnable()...");

    // Start MagicLeap's Location service:
    if (!MLLocation.IsStarted)
    {
      MLLocation.Start();
    }

    // Needs a render Texture.
    Logger.D(TAG, "Calling Start to camera...");
    MLCamera.Start();
    Logger.D(TAG, "Calling Connect()...");
    MLCamera.Connect();

    Logger.D(TAG, "Calling StartPreview()...");
    MLCamera.StartPreview();

    Logger.D(TAG, "Setting texture sizes app side...");

    // Preview has fixed capture height and width. May need a prepare capture to do parameters not in preview.
    W = MLCamera.PreviewTextureWidth;
    H = MLCamera.PreviewTextureHeight;

    // Image width and height to send to face detection server 
    ImageWidth = 1440;
    ImageHeight = 1080;


    // Allocate space for render buffers:
    tmp = RenderTexture.GetTemporary(
                    W,
                    H,
                    0,
                    RenderTextureFormat.ARGB32,
                    RenderTextureReadWrite.Linear);
    nt = new Texture2D(ImageWidth, ImageHeight, TextureFormat.ARGB32, false);
  }

  void OnDisable()
  {
    if (MLCamera.IsStarted)
    {
      MLCamera.Disconnect();
      MLCamera.Stop();
    }

    if (MLLocation.IsStarted)
    {
      //MLLocation.Stop();
    }
  }

  void OnApplicationPause(bool pauseStatus)
  {
    if (!pauseStatus)
    {

      if (!MLLocation.IsStarted)
      {
        MLLocation.Start();
      }

      MLCamera.Start();
      MLCamera.Connect();
      MLCamera.StartPreview();
    }
    else
    {
      OnDisable();
    }
  }

  void OnDestroy()
  {
    if (qrThread != null)
    {
      qrThread.Abort();
    }
    OnDisable();
  }

  // It's better to stop the thread by itself rather than abort it.
  void OnApplicationQuit()
  {
    isQuit = true;
  }

  // Main thread only.
  private void ensureBarcodeCamera()
  {
    // Try to get a reference to the Barcode Cam Monitor
    if (cameraMonitor == null)
    {
      cameraMonitor = GameObject.FindGameObjectWithTag("BarCodeMonitor");
      // Attach webcam texture output to a render texture of that surface.
      monitorMaterial = cameraMonitor.GetComponent<Renderer>().material;

      cameraSubtitle = GameObject.FindGameObjectWithTag("BarCodeDecodedTextArea");
      tmpro = cameraSubtitle.GetComponent<TextMeshPro>();

      // Barcode sized buffer holder.
      barcodeBuffer = GameObject.FindGameObjectWithTag("BarcodeBuffer");
      barcodeBufferMaterial = barcodeBuffer.GetComponent<Renderer>().material;
    }
  }

  string checkEdgeCloudletUri()
  {
    string uri = null;
    if (CompanionDeviceManager.isReady())
    {
      uri = CompanionDeviceManager.GetAppInstHost();
    }

    return uri;
  }

  void Start()
  {
    faceDetectionRects = new FaceDetectionRects();

    if (CompanionDeviceManager == null)
    {
      Logger.E(TAG, "CompanionDeviceManager doesn't exist!");
    }
    else
    {
      Logger.E(TAG, "CompanionDeviceManager appears to be OK!");
    }
    ensureBarcodeCamera();

    /* Local HTTP client */
    httpClient = new HttpClient();
    httpClient.Timeout = new TimeSpan(0, 0, 0, 0, (int)DEFAULT_REST_TIMEOUT_MS);

    encoded = new Texture2D(256, 256);
    CompanionDeviceHost = "http://www.mobiledgex.com";
    shouldEncodeNow = true;

    screenRect = new Rect(0, 0, Screen.width, Screen.height);


    OnEnable();

    /**
     * Refactor:
     * Spawn a separate Coroutine to go through these steps:
     * 1) Get DME host. wifi.dme.mobiledgex.net
     * 2) Get Location (from ML Location or Unity on the device).
     * 3) then launch FindCloudlet, and yeild for ready.
     * 4) On the yeilding thread, wait for an OK, and run normally. Delete CompanionDevice, and CompaionDeviceManager.cs.
     * 5) Remove large view, keep small one.
     * 
     */

    qrThread = new Thread(CameraFeedHandler);
    qrThread.Start();

    // The CompanionDeviceManager GameObject separately updates itself to get
    // the AppInst for this App/Scene.
  }

  // Simple rate limit warning.
  void Update()
  {
    Logger.D(TAG, "Update()...");
    // Might not have a real camera.
    if (!MLCamera.IsStarted)
    {
      Logger.D(TAG, "No camera is started yet!");
      return;
    }

    if (CompanionDeviceManager != null && CompanionDeviceManager.companionDevice != null)
    {
      Logger.D(TAG, "DME host known: " + CompanionDeviceManager.isReady() + ", host: " + CompanionDeviceManager.companionDevice.AppInstHost);
    }

    try
    {
      if (c == null)
      {
        // Can't read/copy webcam pixels to CPU memory, just point to the camera texture:
        monitorMaterial.mainTexture = MLCamera.PreviewTexture2D;
        Logger.D(TAG, "MainTexture set: " + (monitorMaterial.mainTexture != null));

        // Render to a tmp renderTexture to extract pixels to a readable texture:
        RenderTexture previous = RenderTexture.active;
        RenderTexture.active = tmp;

        // CopyTexture types is system dependent, platform must support it.
        // Copy the pixels on texture to the RenderTexture (not blit, no format conversion?), Src and Dest must be same size texture for copy.
        Graphics.CopyTexture(MLCamera.PreviewTexture2D, tmp);

        // Create rectangle that hopefully is what the user sees through MagicLeap glasses
        //Rect r = new Rect((W - ImageWidth )/2, (H / 2) - ImageHeight / 2, ImageWidth, ImageHeight);
        Rect r = new Rect((W - ImageWidth) * .7f, (H / 2) - ImageHeight / 2, ImageWidth, ImageHeight);

        nt.ReadPixels(r, 0, 0, true);
        nt.Apply();

        // Reset global:
        RenderTexture.active = previous;

        // Set cropped texture to send buffer view:
        barcodeBufferMaterial.mainTexture = nt;

        // FaceDetection must run on Main Thread due to Texture2D lifetime.
        handleFaceDetection(nt);
      }

      SetTeleText(CompanionDeviceHost + " " + count++);


#if false
      // encode the last found
      var textForEncoding = CompanionDeviceHost;
      if (shouldEncodeNow &&
          textForEncoding != null)
      {
        // Bar code:
        var color32 = Encode(textForEncoding, encoded.width, encoded.height);
        encoded.SetPixels32(color32);
        encoded.Apply();
        shouldEncodeNow = false;
      }
#endif

    }
    catch (CompanionDeviceDefinitionMissing cddm)
    {
      Logger.D(TAG, "Whoops: " + cddm.Message);
    }
    catch (Exception e)
    {
      Logger.D(TAG, "Exception Message:" + e.Message);
      Logger.D(TAG, "Exception hit:" + e.StackTrace);
    }
  }

  bool handleFaceDetection(Texture2D texture2DCam)
  {

    // Check to see if companionDeviceManager has an AppInst to send to:
    Logger.D(TAG, "handleFaceDetection()");
    if (CompanionDeviceManager == null)
    {
      Logger.D(TAG, "handleFaceDetection() 1");
      CompanionDeviceManager = CompanionDeviceManagerGo.GetComponent<CompanionDeviceManager>();
      if (CompanionDeviceManager == null)
      {
        Logger.D(TAG, "CompanionDeviceManger not attached!");
      }
    }
    if (faceDetectionUI == null)
    {
      Logger.D(TAG, "handleFaceDetection() Adding listener for face detection rectangles.");
      facedetectionUIGo = GameObject.FindGameObjectWithTag("FaceDetectionUITag");
      faceDetectionUI = facedetectionUIGo.GetComponent<FaceDetectionUI>();
      if (faceDetectionUI != null)
      {
        faceDetectionUI.FaceDetectionRects = faceDetectionRects; // Use the same subscriber.
      }
    }

    if (!CompanionDeviceManager.isReady())
    {
      Logger.D(TAG, "HandleDetection(): CompanionDeviceManager is NOT Ready!");
      return false;
    }

    // Handle FaceDetection Task:
    if (faceDetectionRectsTask == null)
    {
      Logger.D(TAG, "width of texture is " + texture2DCam.width + ", and height is " + texture2DCam.height);
      byte[] bytes = ImageConversion.EncodeToJPG(texture2DCam);
      Logger.D("EncodeToJPG", "Bytes length: " + bytes.Length);
      faceDetectionRectsTask = FaceDetectionGetRectsAsync(bytes);
    }
    else if (faceDetectionRectsTask.IsCompleted)
    {
      Logger.D(TAG, "Detection Task completed.");
      Logger.D(TAG, "ParentCam Set? " + parentCam);
      Logger.D(TAG, "faceDetection Set? " + faceDetectionRects);
      faceDetectionRects.Update(faceDetectionRectsTask.Result, parentCam.transform.position, parentCam.transform.rotation);

      Logger.D(TAG, $"Detection Task rectangle size: {faceDetectionRects.GetRects().Count}");
      // clear for next task:
      faceDetectionRectsTask = null;
    }
    return true;
  }

  static int count = 0;

  // The Feed is long lived in a thread.
  // This just handles background thread tasks that are managing stuff that isn't
  // in the Unity Main thread. The main thread should just check the states
  // and apply it when it sees a change.
  void CameraFeedHandler()
  {
    // create a reader with a custom luminance source
    var barcodeReader = new BarcodeReader { AutoRotate = false };
    Logger.D(TAG, "In DecodeQR");
    while (true)
    {
      //Logger.D(TAG, "DecodeQR loop.");
      if (isQuit)
        break;

      try
      {
        // decode the current frame
        if (c != null)
        {
          Logger.D(TAG, "AppMode is: " + appMode);

          switch (appMode)
          {
            case AppMode.QrCodeScanning:
              Logger.D(TAG, "Send to Decoder...");
              var result = barcodeReader.Decode(c, ImageWidth, ImageHeight);

              // Update states:
              if (result != null)
              {
                CompanionDeviceHost = result.Text;
                CompanionDeviceManager.SetCompanionAppHost(CompanionDeviceHost);
                // Wait for return of the QrCode handler in this thread context. Must not be a UI thread.
                if (HandleQrCodeRegistration(CompanionDeviceHost).Result)
                {
                  appMode = AppMode.CompanionAppInfo;
                  Logger.D(TAG, "The Companion Device is registered!");
                }
                else
                {
                  Logger.D(TAG, "The Companion Device is NOT registered!");
                }
                Logger.D(TAG, result.Text);
              }
              break;
            case AppMode.CompanionAppInfo:
              Logger.D(TAG, "Waiting for CompanionDeviceManger to get Info for FindCloudlet...");
              if (CompanionDeviceManager == null)
              {
                Logger.D(TAG, "CD GO Script: " + (CompanionDeviceManager != null));
                Logger.D(TAG, "Don't forget to set the CompaninoDeviceManager Game Object reference!");
                continue;
              }
              
              if (CompanionDeviceManager.isReady())
              {
                // Should be event handler, not print.
                appMode = AppMode.FaceDetectionReady;
                Logger.D(TAG, "Device is now registered against CompanionApp, and will update location and appInst host server");
              }
              else if (!CompanionDeviceManager.UpdateRunning)
              {
                CompanionDeviceManager.RequestUpdateCompanionDevice();
              }
              break;
            case AppMode.FaceDetectionReady:
              Logger.D(TAG, "Ready. Detection Server Host is: " + CompanionDeviceManager.GetAppInstHost());
              if (CompanionDeviceManager.GetAppInstHost() == null) // Odd, got back to find the CompanionApp host again.
              {
                Logger.D(TAG, "Missing AppInstHost, Moving back to CompanionAppInfo (or QrCodeScanning).");
                appMode = AppMode.CompanionAppInfo;
              }
              break;
            default:
              Logger.D(TAG, "Unknown appMode: " + appMode);
              break;

          }
        }
      }
      catch (EmptyStreamException ese)
      {
        Logger.D(TAG, "Exception Message: " + ese.Message);
        Logger.D(TAG, "Exception hit: " + ese.StackTrace);
      }
      catch (Exception e)
      {
        Logger.D(TAG, "Exception Message: " + e.Message);
        Logger.D(TAG, "Exception hit: " + e.StackTrace);
      }
      finally
      {
        c = null;
      }

      // Sleep a little bit and set the signal to get the next frame.
      Thread.Sleep(200); // So, 5fps. FIXME.
      shouldEncodeNow = true;
    }
  }

  private static Color32[] Encode(string textForEncoding, int width, int height)
  {
    var writer = new BarcodeWriter
    {
      Format = BarcodeFormat.QR_CODE,
      Options = new QrCodeEncodingOptions
      {
        Height = height,
        Width = width
      }
    };
    return writer.Write(textForEncoding);
  }

  private void SetTeleText(string barcodeText)
  {
    tmpro.text = barcodeText;
  }

  // In thread loop, not UI thread. Can pause for async tasks.
  async private Task<Boolean> HandleQrCodeRegistration(string Qrcode)
  {
    Logger.D(TAG, "HandleQrCode(" + Qrcode + ")");
    if (CompanionDeviceManager.GetCompanionAppHost() == null)
    {
      Logger.D(TAG, "No CompanionAppHost is set.");
      return false;
    }

    int minimumLength = "x.x.x.x".Length;
    if (Qrcode != null && Qrcode.Length > minimumLength)
    {
      string quri = "http://" + Qrcode;
      Logger.D(TAG, "quri: " + quri);

      // Register against CompanionApp.
      Logger.D(TAG, "1");
      // TODO: OAuth.
      DeviceListReply deviceListReply;
      DeviceRegisterReply deviceRegisterReply;
      try
      {
        UserInfo info = new UserInfo
        {
          user_name = ui_user_name == null ? "" : ui_user_name,
          user_email = ui_user_email == null ? "" : ui_user_email,
          user_id = ui_user_id == null ? "" : ui_user_id
        };
        // TODO:
        // 0) OAuth UI
        // 0.5) TLS
        // 1) Retrieve a device list and UI
        // 2) Choose and Register a device from managed list and UI.
        string host = Qrcode;
        deviceListReply = await CompanionDeviceManager.DeviceList(info, host, CompanionDevice.DEFAULT_REST_PORT);
        Device chosenDevice = null;
        if (deviceListReply != null)
        {
          Device[] devices = deviceListReply.devices;
          if (devices.Length > 0)
          {
            // UI: have to choose one to use.
            chosenDevice = devices[0];
          }
        }
        else
        {
          Logger.E(TAG, "No devices list to register with!");
          return false;
        }

        deviceRegisterReply = await CompanionDeviceManager.DeviceRegister(info, chosenDevice, host, CompanionDevice.DEFAULT_REST_PORT);
        if (deviceRegisterReply == null)
        {
          Logger.E(TAG, "RegisterDevice Reply NULL");
          return false;
        }
        Logger.D(TAG, "Reply state:" + deviceRegisterReply.success);

        if (!deviceRegisterReply.success)
        {
          Logger.E(TAG, "Did not register! Message: " + deviceRegisterReply.message);
        }
        else
        {
          Logger.D(TAG, "Reply: " + deviceRegisterReply.success + ", Message: " + deviceRegisterReply.message);
        }
      }
      catch (CompanionDeviceDefinitionMissing cddm)
      {
        Logger.E(TAG, cddm.Message);
        return false;
      }
      catch (Exception e)
      {
        Logger.E(TAG, e.Message);
        if (e.InnerException != null)
        {
          Logger.E(TAG, e.InnerException.Message);
        }
        return false;
      }

      return deviceRegisterReply.success;
    }

    return false;
  }

  // Background task, with a send result at the end upon await(). May have no
  // Rectangles.

  public enum CameraMode
  {
    FACE_DETECTION,
    FACE_RECOGNITION,
    FACE_TRAINING,
    FACE_UPDATING_SERVER,
    FACE_UPDATE_SERVER_COMPLETE,
    POSE_DETECTION
  }

  async private Task<List<Rect>> FaceDetectionGetRectsAsync(byte[] bytesJpeg)
  {
    Logger.D(TAG, "FaceDetectionGetRectsAsync()");
    var rectList = new List<Rect>();

    if (!CompanionDeviceManager.isReady())
    {
      Logger.D(TAG, "CompanionDeviceManager not ready yet.");
      return rectList;
    }

    string uri = CompanionDeviceManager.GetAppInstHost();

    if (bytesJpeg == null || bytesJpeg.Length == 0) {
      Logger.D(TAG, "no bytes to send!");
    }

    string mDjangoUrl = "/detector/detect/";
    string url = uri + mDjangoUrl;
    //Logger.D(TAG, "Host to send to: " + url);

    // REST Data
    var content = new ByteArrayContent(bytesJpeg);
    content.Headers.ContentType = System.Net.Http.Headers.MediaTypeHeaderValue.Parse("image/jpeg");

    try
    {
      HttpResponseMessage response = await httpClient.PostAsync(url, content);
      if (response == null)
      {
        Logger.D(TAG, "No response from url! " + url);
        return null;
      }

      Logger.D("FaceDetectionGetRectsAsync", "Response Code: " + response.StatusCode);
      if (!response.IsSuccessStatusCode)
      {
        return rectList;
      }

      //Logger.D("FaceDetectionGetRectsAsync Parse", "Reading response...");
      byte[] bytes = await response.Content.ReadAsByteArrayAsync();
      Logger.D("FaceDetectionGetRectsAsync Parse", "Response Bytes Length: " + bytes.Length);

      DataContractJsonSerializer deserializer = new DataContractJsonSerializer(typeof(FaceDetectionReply));
      MemoryStream ms = new MemoryStream(bytes);
      var reply = (FaceDetectionReply)deserializer.ReadObject(ms);

      if (reply.rects != null)
      {
        Logger.D("FaceDetectionGetRectsAsync", $"FaceDetection Reply: success: {reply.success}, rects length: {reply.rects.Length}, server_processing_time: {reply.server_processing_time}");
        Rect r;
        foreach (int[] minmax in reply.rects)
        {
          if (minmax.Length == 4)
          {
            r = Rect.MinMaxRect(minmax[0], minmax[1], minmax[2], minmax[3]);
            rectList.Add(r);
            Logger.D("FaceDetectionGetRectsAsync", $"Added rect, rectList size {rectList.Count}: {r.x}, {r.y}, {r.width}, {r.height}");
          }
          else
          {
            // Odd:
            Logger.D(TAG, "Odd Rect length: " + minmax.Length);
          }
        }
      }
      else
      {
        Logger.D("FaceDetectionGetRectsAsync", $"FaceDetection Reply: success: {reply.success} rects: None!, server_processing_time: {reply.server_processing_time}");
      }
    }
    catch (Exception e)
    {
      if (e.InnerException != null)
      {
        Logger.D("FaceDetectionGetRectsAsync", "Message: " + e.InnerException.Message + ", Stack: " + e.InnerException.StackTrace);
      }
      Logger.D("FaceDetectionGetRectsAsync", "Message: " + e.Message + ", Stack: " + e.StackTrace);
    }
    return rectList;
  }
}
