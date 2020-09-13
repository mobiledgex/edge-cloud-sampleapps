# Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
# MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt
from django.views.generic import TemplateView
from django.http import HttpResponse, HttpResponseBadRequest
from tracker.models import CentralizedTraining
from tracker.apps import myFaceDetector, myFaceRecognizer, myOpenPose, myOpWrapper
from tracker.apps import myObjectDetector

import ast
import numpy as np
import json
import base64
from imageio import imread, imwrite
import io
import time
import os
import glob
import json
import logging
import imghdr
from PIL import Image

logger = logging.getLogger(__name__)

@csrf_exempt
def show_index(request):
    """ Show index.html for the CV demo. """
    return render(request,'index.html')

@csrf_exempt
def show_index2(request):
    """ Show index2.html for the CV demo. """
    return render(request,'index2.html')

@csrf_exempt
def test_connection(request):
    """ Test the connection to the backend """
    if request.method == 'GET':
        logger.info(prepend_ip("/test/ Valid GET Request received", request))
        return HttpResponse("Valid GET Request to server")
    if request.method == 'HEAD':
        # This is used by the JavaScript CV Demo for testing network latency.
        logger.debug(prepend_ip("/test/ Valid HEAD Request received", request))
        return HttpResponse("Valid HEAD Request to server")
    return HttpResponseBadRequest("Please send request as a GET or HEAD")

@csrf_exempt
def get_data(request):
    """ Generate and send data to a client connection """
    logger.info(prepend_ip("Request received: %s" %request, request))
    if request.method == 'GET':
        numbytes = request.GET.get("numbytes", "")
        if numbytes == "":
            return HttpResponseBadRequest("Missing 'numbytes' parameter")
        return HttpResponse("Z"*int(numbytes))
    return HttpResponseBadRequest("Please send response as a GET")

@csrf_exempt
def upload_data(request):
    """
    Receive data from a client connection. This is used for speed test purposes,
    so the data isn't written anywhere or used in any way.
    """
    logger.info(prepend_ip("Request received: %s" %request, request))
    if request.method == 'POST':
        start = time.time()
        # Doesn't do anything with posted data
        content_length = int(request.headers['Content-Length']) # Gets the size of data
        logger.info(prepend_ip("content_length=%s" %content_length, request))
        post_data = request.read(content_length) # Gets the data itself
        elapsed = float("%.3f" %((time.time() - start)*1000))
        mbps = "%.3f" %((content_length*8)/(elapsed*1000))
        ret = {"bytes_read": content_length, "elapsed_ms": elapsed, "mbps": mbps}
        json_ret = json.dumps(ret)
        logger.info(prepend_ip(json_ret, request))
        return HttpResponse(json_ret)

    return HttpResponseBadRequest("Please send response as a POST")

def prepend_ip(text, request):
    return "[%s] %s" %(request.META.get('REMOTE_ADDR'), text)

def save_debug_image(image, request, type):
    """ Save current image with timestamp. """
    logger.debug(prepend_ip("Saving image for debugging (first 32 bytes logged): %s" %image[:32], request))
    extension = imghdr.what("XXX", image)
    if extension is None or extension == "jpeg":
        extension = "jpg"
    now = time.time()
    mlsec = repr(now).split('.')[1][:3]
    timestr = time.strftime("%Y%m%d-%H%M%S")+"."+mlsec
    fileName = "/tmp/"+type+"_"+timestr+"."+extension
    with open(fileName, "wb") as fh:
        fh.write(image)
    #Delete all old files except the 20 most recent
    logger.debug(prepend_ip("Deleting all but 20 newest images", request))
    try:
        files = sorted(glob.glob("/tmp/"+type+"_*.*"), key=os.path.getctime, reverse=True)
        for file in files[20:]:
            os.remove(file)
    except FileNotFoundError:
        logger.warn("Cleanup of /tmp/"+type+"* failed. Next request will retry.")

def get_image_from_request(request, type):
    """ Based on the content type, get the image data from the request. """
    logger.info(prepend_ip("get_image_from_request method=%s content_type=%s" %(request.method, request.content_type), request))
    logger.debug("first 32 bytes of body: %s" %request.body[:32])
    if request.method != 'POST':
        return HttpResponseBadRequest("Must send frame as a POST")

    if request.content_type == "image/png" or request.content_type == "image/jpeg":
        if request.body == "":
            return HttpResponseBadRequest("No image data")
        image = request.body
    elif request.content_type == "multipart/form-data":
        # Image data is expected as if it came from a form with <input type="file" name="image">
        if not "image" in request.FILES.keys():
            return HttpResponseBadRequest("Image file must be uploaded with key of 'image'")
        uploaded_file = request.FILES["image"]
        logger.info("Uploaded file type=%s size=%d" %(uploaded_file.content_type, uploaded_file.size))
        if uploaded_file.content_type != "image/png" and uploaded_file.content_type != "image/jpeg" and uploaded_file.content_type != "application/octet-stream":
            return HttpResponseBadRequest("Uploaded file Content-Type must be 'image/png', 'image/jpeg'")
        image = uploaded_file.read()
    elif request.content_type == "application/x-www-form-urlencoded":
        if request.POST.get("image", "") == "":
            return HttpResponseBadRequest("Missing 'image' parameter")
        image = base64.b64decode(request.POST.get("image"))
    else:
        return HttpResponseBadRequest("Content-Type must be 'image/png', 'image/jpeg', 'multipart/form-data', or 'application/x-www-urlencoded'")

    if request.headers.get("Mobiledgex-Debug", "") == "true":
        save_debug_image(image, request, type)

    image = imread(io.BytesIO(image)) # convert to numpy array
    return image

@csrf_exempt
def detector_detect(request):
    """
    Runs facial detection on a frame that is sent via a REST
    API call and returns a JSON formatted set of coordinates.
    """
    logger.debug(prepend_ip("Request received: %s" %request, request))

    image = get_image_from_request(request, "face")
    if not isinstance(image, np.ndarray):
        # If it's not an image in numpy array format, it is an
        # HttpResponseBadRequest which we will return to the caller.
        return image

    logger.debug(prepend_ip("Performing detection process", request))
    start = time.time()
    rects = myFaceDetector.detect_faces(image)
    elapsed = "%.3f" %((time.time() - start)*1000)

    # Create a JSON response to be returned in a consistent manner
    if len(rects) == 0:
        ret = {"success": "false", "server_processing_time": elapsed}
    else:
        ret = {"success": "true", "server_processing_time": elapsed, "rects": rects.tolist()}
    logger.info(prepend_ip("%s ms to detect rectangles: %s" %(elapsed, ret), request))
    json_ret = json.dumps(ret)
    return HttpResponse(json_ret)

@csrf_exempt
def recognizer_update(request):
    """
    Checks with FaceTrainingServer and if new training data is available,
    downloads and reads it.
    """
    logger.debug(prepend_ip("Request received: %s" %request, request))
    if request.method != 'POST':
        return HttpResponseBadRequest("Must send request as a POST")

    ret = myFaceRecognizer.download_training_data_if_needed()
    return HttpResponse("%s\n" %ret)

@csrf_exempt
def recognizer_predict(request):
    """
    Runs facial recognition on received image, using pre-trained dataset.
    Returns subject name, and rectangle coordinates of recognized face.
    """
    logger.debug(prepend_ip("Request received: %s" %request, request))

    if myFaceRecognizer.training_update_in_progress:
        error = "Training data update in progress"
        logger.error(prepend_ip("%s" %error, request))
        return HttpResponse(error, status=503)

    image = get_image_from_request(request, "face")
    if not isinstance(image, np.ndarray):
        return image

    logger.debug(prepend_ip("Performing recognition process", request))
    now = time.time()
    predicted_img, subject, confidence, rect = myFaceRecognizer.predict(image)
    elapsed = "%.3f" %((time.time() - now)*1000)

    # Create a JSON response to be returned in a consistent manner
    if subject is None:
        ret = {"success": "false", "server_processing_time": elapsed}
    else:
        # Convert rect from [x, y, width, height] to [x, y, right, bottom]
        rect2 = rect.tolist()
        rect2 = [int(rect[0]), int(rect[1]), int(rect[0]+rect[2]), int(rect[1]+rect[3])]
        ret = {"success": "true", "subject": subject, "confidence":"%.3f" %confidence, "server_processing_time": elapsed, "rect": rect2}
    logger.info(prepend_ip("Returning: %s" %(ret), request))
    json_ret = json.dumps(ret)
    return HttpResponse(json_ret)

@csrf_exempt
def recognizer_train(request):
    """
    Perform recognizer.train() on saved training images.
    """

    if request.method == 'POST':
        logger.info(prepend_ip("Request received: %s" %request, request))
        subject = request.POST.get("subject", "")
        if subject == "":
            return HttpResponseBadRequest("Missing 'subject' parameter")

        start = time.time()
        myFaceRecognizer.update_training_data(subject)
        elapsed = "%.3f" %((time.time() - start)*1000)
        logger.info(prepend_ip("%s ms to update training data" %elapsed, request))

        return HttpResponse("OK\n")

    return HttpResponseBadRequest("Must send request as a POST")

@csrf_exempt
def recognizer_add(request):
    """
    Perform face detection on received image. If face found, save image to
    subject's training data directory.
    """
    logger.debug(prepend_ip("Request received: %s" %request, request))

    image = get_image_from_request(request, "face")
    if not isinstance(image, np.ndarray):
        return image

    if request.POST.get("owner", "") != "":
        owner = request.POST.get("owner")
        logger.info(prepend_ip("Guest image for %s from owner %s" %(subject, owner), request))

    logger.debug(prepend_ip("Performing detection process", request))
    start = time.time()
    rects = myFaceDetector.detect_faces(image)
    elapsed = "%.3f" %((time.time() - start)*1000)

    # Create a JSON response to be returned in a consistent manner
    if len(rects) == 0:
        ret = {"success": "false", "server_processing_time": elapsed}
    else:
        ret = {"success": "true", "rects": rects.tolist()}
        # Save image to subject's directory
        myFaceRecognizer.save_subject_image(subject, image)

    logger.info(prepend_ip("%s ms to detect rectangles: %s" %(elapsed, ret), request))
    json_ret = json.dumps(ret)
    return HttpResponse(json_ret)

@csrf_exempt
def openpose_detect(request):
    """
    Use OpenPose to extract human skeleton points from a given image.
    """
    if myOpenPose is None:
        error = "OpenPose not supported on this server"
        logger.error(prepend_ip("%s" %error, request))
        ret = {"success": "false", "error": error}
        json_ret = json.dumps(ret)
        return HttpResponse(json_ret, status=501)

    logger.debug(prepend_ip("Request received: %s" %request, request))

    image = get_image_from_request(request, "pose")
    if not isinstance(image, np.ndarray):
        return image

    logger.debug(prepend_ip("Performing pose detection process", request))
    start = time.time()
    datum = myOpenPose.Datum()
    datum.cvInputData = image
    myOpWrapper.emplaceAndPop([datum])
    poses = datum.poseKeypoints
    # Return the human pose poses, i.e., a [#people x #poses x 3]-dimensional numpy object with the poses of all the people on that image
    elapsed = "%.3f" %((time.time() - start)*1000)
    poses2 = np.around(poses, decimals=6)
    # poses2 = np.rint(poses)

    # Create a JSON response to be returned in a consistent manner
    if isinstance(poses2, np.float32) or len(poses2) == 0:
        num_poses = 0
        ret = {"success": "false", "server_processing_time": elapsed}
    else:
        num_poses = len(poses2)
        ret = {"success": "true", "server_processing_time": elapsed, "poses": poses2.tolist()}

    logger.info(prepend_ip("%s ms to detect %d poses: %s" %(elapsed, num_poses, ret), request))
    json_ret = json.dumps(ret)
    return HttpResponse(json_ret)

@csrf_exempt
def server_capabilities(request):
    """ Test the connection to the backend """
    if request.method == 'GET':
        ret = {"success": "true", "gpu_support": myObjectDetector.is_gpu_supported()}
        json_ret = json.dumps(ret)
        return HttpResponse(json_ret)

    return HttpResponseBadRequest("Please send request as a GET")

@csrf_exempt
def server_usage(request):
    from tracker.utils import usage_cpu_and_mem, usage_gpu
    """
    Get CPU and GPU usage summary.
    """
    if request.method == 'GET':
        ret1 = usage_cpu_and_mem()
        ret2 = usage_gpu()
        # Merge the dictionaries together.
        ret = ret1.copy()
        ret.update(ret2)
        json_ret = json.dumps(ret)
        return HttpResponse(json_ret)

    return HttpResponseBadRequest("This request must be a GET")

@csrf_exempt
def object_detect(request):
    """
    """
    logger.debug(prepend_ip("Request received: %s" %request, request))

    image = get_image_from_request(request, "obj")
    if not isinstance(image, np.ndarray):
        return image

    logger.debug(prepend_ip("Performing detection process", request))
    pillow_image = Image.fromarray(image, 'RGB')
    start = time.time()
    objects = myObjectDetector.process_image(pillow_image)
    elapsed = "%.3f" %((time.time() - start)*1000)

    # Create a JSON response to be returned in a consistent manner
    if len(objects) == 0:
        ret = {"success": "false", "server_processing_time": elapsed, "gpu_support": myObjectDetector.is_gpu_supported()}
    else:
        ret = {"success": "true", "server_processing_time": elapsed, "gpu_support": myObjectDetector.is_gpu_supported(), "objects": objects}
    logger.info(prepend_ip("%s ms to detect objects: %s" %(elapsed, ret), request))
    json_ret = json.dumps(ret)
    return HttpResponse(json_ret)
