/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
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
using System.Collections.Generic;
using System.Diagnostics;
using UnityEngine;

public class FaceDetectionRects : Stopwatch
{
  string TAG = "FaceDetectionRects";
  List<Rect> rects = new List<Rect>();
  private TimeSpan TTL = new System.TimeSpan(0, 0, 0, 4);

  public delegate void DetectionCallback();
  public DetectionCallback consumers;

  Vector3 position;
  Quaternion rotation;

  object syncObject;

  public FaceDetectionRects()
  {
    syncObject = new object();
    consumers = UnWatched;
  }

  private void ClearRects()
  {
    rects.Clear();
  }

  private void UnWatched()
  {
    int? num;
    num = consumers?.GetInvocationList().Length;

    if (num == 1)
    {
      Logger.D(TAG, "Default no-op consumer notified.");
    }
    else
    {
      Logger.D(TAG, "Num consumers: " + num);
    }
  }

  public void Update(List<Rect> newRects, Vector3 position, Quaternion rotation)
  {
    lock (syncObject)
    {
      Reset();
      if (newRects == null)
      {
        this.rects.Clear();
      }
      else
      {
        rects = newRects;
      }
      this.position = new Vector3(position.x, position.y, position.z);
      this.rotation = new Quaternion(rotation.x, rotation.y, rotation.z, rotation.w);
      consumers();
    }

  }

  public bool Expired()
  {
    if (Elapsed > TTL)
    {
      return true;
    }
    return false;
  }

  public bool DoExpire()
  {
    if (Expired())
    {
      ClearRects();
      Reset();
      return true;
    }
    return false;
  }

  /**
   * Auto expires, then return rects;
   */
  public List<Rect> GetRects()
  {
    lock (syncObject)
    {
      DoExpire();
    }
    return rects;
  }

}

public class FaceDetectionUI : MonoBehaviour
{
  const string TAG = "FaceDetectionUI";
  GameObject faceDetectionGo;

  bool RectsUpdated;
  void HandleDetection()
  {
    //Logger.D(TAG, "Notified of Rect updates!");
    RectsUpdated = true;
  }

  // Base prefab for detection decoration.
  public GameObject detectionPrefab;
  private List<GameObject> detectionUIInsts;

  public GameObject parentCam;

  // captured pixels width and height
  int imageW = 1440;
  int imageH = 1080;

  // Field of View in degrees
  int horizontalFOV = 40;
  int verticalFOV = 30;

  // Half of the Field of view in radians
  double horizontalRadians;
  double verticalRadians;

  // Unit vector to place decorator in front of camera
  Vector3 unitCamForward = new Vector3(0, 0, 1);

  // View fustrum variables
  float depth;
  double widthAtDepth;
  double heightAtDepth;

  // Set in Unity Editor or programatially:
  private FaceDetectionRects faceDetectionRects;
  public FaceDetectionRects FaceDetectionRects
  {
    get {
      return faceDetectionRects;
    }
    set
    {
      faceDetectionRects = value;
      faceDetectionRects.consumers += HandleDetection; // Add a listener.
    }
  }

  // Start is called before the first frame update
  void Start()
  {
    Logger.D(TAG, "Start()");
    faceDetectionGo = GameObject.FindGameObjectWithTag("DetectionQuad");

    if (parentCam == null)
    {
      Logger.D(TAG, "Warning: Cannot place decorator for face dectection in world if the camera position is not known!");
    }

    // Set a default detection decoration:
    if (detectionPrefab == null && parentCam != null)
    {
      detectionPrefab = (GameObject)Resources.Load("Prefabs/LineDrawer", typeof(GameObject));
    }

    // Create a few instances to cache:
    detectionUIInsts = new List<GameObject>();
    for (int i = 0; i < 4; i++)
    {
      GameObject obj = Instantiate(detectionPrefab, new Vector3(0, 0, 0), Quaternion.identity);
      detectionUIInsts.Add(obj);
    }

    // Initialize view fustrum variables
    horizontalRadians = (double)horizontalFOV / 2 * (Math.PI/180);
    verticalRadians = (double)verticalFOV / 2 * (Math.PI/180);

    // Calculate width and height of view fustrum based on depth
    depth = unitCamForward.z;
    widthAtDepth = 2 * depth * Math.Tan(horizontalRadians);
    heightAtDepth = 2 * depth * Math.Tan(verticalRadians);
  }

  private int ensureDecoratorInstanceCapacity(int minCapacity)
  {
    int enlarge = minCapacity - detectionUIInsts.Capacity;
    if (enlarge <= 0)
      return 0;

    for (int i = 0; i < enlarge; i++)
    {
      GameObject obj = Instantiate(detectionPrefab, new Vector3(0, 0, 0), Quaternion.identity);
      if (obj == null)
      {
        Logger.D(TAG, "Cannot instantiate prefab decorator!");
        break; // Odd.
      }
      detectionUIInsts.Add(obj);
    }

    return enlarge;
  }

  // Update is called once per frame
  void Update()
  {
    Logger.D(TAG, "Update()...");
    FaceDetectionRects faceDetectionRects = ComputerVisionFaceDetection.faceDetectionRects;
    // If no new rects, just leave the current ones up floating in world space (don't update).
    if (!RectsUpdated)
    {
        Logger.D(TAG, "No new rects notified.");
      return;
    }
    RectsUpdated = false;

    // Singleton field from camera detections
    var faceRects = faceDetectionRects.GetRects();
    Logger.D(TAG, "Pre: Num Rects to draw: " + faceRects.Capacity);
    if (faceRects.Capacity == 0)
    {
      // Disable for Update.
      foreach(GameObject decorator in detectionUIInsts)
      {
        decorator.SetActive(false);
      }
      Logger.D(TAG, "No detection rectangles.");
      return;
    }

    Logger.D(TAG, "In FaceDetectionUI rects size: " + faceRects.Capacity);
    ensureDecoratorInstanceCapacity(faceRects.Capacity);

    if (parentCam == null)
    {
      Logger.D(TAG, "ParentCam or transform is missing!");
      return;
    }

    int i = 0;
    // Sort of hacks dependent on the sub-image cut out and image dimensions.
    float xOffset = 0; // If the camera cut sub-image is heuristically centered, it's 0.
    float yOffset = imageH/5;
    foreach (Rect r in faceRects)
    {
      Logger.D(TAG, $"Rect: {r.x}, {r.y}, {r.width}, {r.height}");
      GameObject decorator = detectionUIInsts[i++];

      if (decorator == null) {
        Logger.D(TAG, "No decorator, odd: " + decorator);
        continue;
      }

      decorator.SetActive(true);

      // Resize rect:
      decorator.transform.localScale = new Vector3(r.width / imageW, r.height / imageH, 1);

      // Set decorator position to camera position
      decorator.transform.position = parentCam.transform.position;

      // Move decorator in front of camera
      decorator.transform.Translate(unitCamForward); 

      // Rotate to match camera rotation:
      Quaternion parentRot = parentCam.transform.rotation;
      Vector3 parentEuler = parentRot.eulerAngles;
      decorator.transform.rotation = Quaternion.Euler(parentEuler.x, parentEuler.y, 0); // no rotation around z-axis

      // Reposition r.x and r.y (origin is at top left of screen) in terms of unity's coordinate system (origin is at "center" of screen)
      // This offset depends entirely on the webcam physical position. The whole thing is sent.
      Vector3 v3pos = new Vector3(
        (float)(-.5 * widthAtDepth + ((r.x + r.width/2 + xOffset) / imageW * widthAtDepth)),
        (float)(.5 * heightAtDepth - ((r.y + r.height/2 + yOffset) / imageH * heightAtDepth)),
        0); // left hand coordinate system
      decorator.transform.Translate(v3pos); // Translation is relative to own space
    }

    // Disable remainder:
    for (; i < faceRects.Capacity; i++)
    {
      detectionUIInsts[i].SetActive(false);
    }
  }

}
