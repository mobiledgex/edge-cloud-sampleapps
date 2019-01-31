from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt

from tracker.serializers import DetectedSerializer
from facial_detection.facedetector import FaceDetector
from facial_detection.faceRecognizer import FaceRecognizer
from tracker.apps import myFaceRecognizer, myOpenPose
from django.http import HttpResponse, HttpResponseBadRequest

import ast
import cv2
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
from subprocess import Popen, PIPE

logger = logging.getLogger(__name__)

fd = FaceDetector()

@csrf_exempt
def test_connection(request):
    """ Test the connection to the backend """
    if request.method == 'GET':
        logger.info(prepend_ip("/test/ Valid GET Request received", request))
        return HttpResponse("Valid GET Request to server")
    return HttpResponse("Please send response as a GET")

def prepend_ip(text, request):
    return "[%s] %s" %(request.META.get('REMOTE_ADDR'), text)

def save_debug_image(image, request):
    logger.debug(prepend_ip("Saving image for debugging", request))
    #Save current image with timestamp
    now = time.time()
    mlsec = repr(now).split('.')[1][:3]
    timestr = time.strftime("%Y%m%d-%H%M%S")+"."+mlsec
    fileName = "/tmp/face_"+timestr+".png"
    with open(fileName, "wb") as fh:
        fh.write(image)
    #Delete all old files except the 20 most recent
    logger.debug(prepend_ip("Deleting all but 20 newest images", request))
    files = sorted(glob.glob("/tmp/face_*.png"), key=os.path.getctime, reverse=True)
    #print(files[20:])
    for file in files[20:]:
        #print("removing %s" %file)
        os.remove(file)

def save_rendered_pose_image(image, request):
    logger.debug(prepend_ip("Saving rendered pose image for debugging", request))
    #Save current image with timestamp
    now = time.time()
    mlsec = repr(now).split('.')[1][:3]
    timestr = time.strftime("%Y%m%d-%H%M%S")+"."+mlsec
    fileName = "/tmp/pose_"+timestr+".png"
    imwrite(fileName, image)
    #Delete all old files except the 20 most recent
    logger.debug(prepend_ip("Deleting all but 20 newest pose images", request))
    files = sorted(glob.glob("/tmp/pose_*.png"), key=os.path.getctime, reverse=True)
    #print(files[20:])
    for file in files[20:]:
        #print("removing %s" %file)
        os.remove(file)

@csrf_exempt
def detector_detect(request):
    """
    Runs facial detection on a frame that is sent via a REST
    API call and returns a JSON formatted set of coordinates.
    """
    if request.method == 'POST':
        logger.debug(prepend_ip("Request received: %s" %request, request))
        if request.POST.get("image", "") == "":
            return HttpResponseBadRequest("Missing 'image' parameter")
        image = base64.b64decode(request.POST.get("image"))
        save_debug_image(image, request)

        logger.debug(prepend_ip("Performing detection process", request))
        start = time.time()
        image = imread(io.BytesIO(image))
        rects = fd.detect_faces(image)
        elapsed = "%.3f" %((time.time() - start)*1000)

        # Create a JSON response to be returned in a consistent manner
        if len(rects) == 0:
            ret = {"success": "false"}
        else:
            ret = {"success": "true", "server_processing_time": elapsed, "rects": rects.tolist()}
        logger.info(prepend_ip("%s ms to detect rectangles: %s" %(elapsed, ret), request))
        json_ret = json.dumps(ret)
        return HttpResponse(json_ret)

    return HttpResponse("Must send frame as a POST")

@csrf_exempt
def recognizer_predict(request):
    """
    Runs facial recognition on received image, using pre-trained dataset.
    Returns subject name, and rectangle coordinates of recognized face.
    """
    if request.method == 'POST':
        logger.debug(prepend_ip("Request received: %s" %request, request))
        if request.POST.get("image", "") == "":
            return HttpResponseBadRequest("Missing 'image' parameter")
        image = base64.b64decode(request.POST.get("image"))
        save_debug_image(image, request)
        logger.debug(prepend_ip("Performing recognition process", request))
        now = time.time()
        image = imread(io.BytesIO(image))
        predicted_img, subject, confidence, rect = myFaceRecognizer.predict(image)
        elapsed = "%.3f" %((time.time() - now)*1000)

        # Create a JSON response to be returned in a consistent manner
        if subject is None:
            ret = {"success": "false"}
        else:
            # Convert rect from [x, y, width, height] to [x, y, right, bottom]
            rect2 = rect.tolist()
            rect2 = [int(rect[0]), int(rect[1]), int(rect[0]+rect[2]), int(rect[1]+rect[3])]
            if confidence >= 105:
                subject = "Unknown"
            ret = {"success": "true", "subject": subject, "confidence":"%.3f" %confidence, "server_processing_time": elapsed, "rect": rect2}
        logger.info(prepend_ip("%s ms to recognize: %s" %(elapsed, ret), request))
        json_ret = json.dumps(ret)
        return HttpResponse(json_ret)

    return HttpResponse("Must send frame as a POST")

@csrf_exempt
def recognizer_train(request):
    """
    Perform recognizer.train() on saved training images.
    """

    if request.method == 'POST':
        logger.info(prepend_ip("Request received: %s" %request, request))
        start = time.time()
        myFaceRecognizer.update_training_data()
        elapsed = "%.3f" %((time.time() - start)*1000)
        logger.info(prepend_ip("%s ms to update training data" %elapsed, request))

        return HttpResponse("OK\n")

    return HttpResponse("Must send request as a POST")

@csrf_exempt
def recognizer_add(request):
    """
    Perform face detection on received image. If face found, save image to
    subject's training data directory.
    """
    if request.method == 'POST':
        logger.debug(prepend_ip("Request received: %s" %request, request))
        if request.POST.get("subject", "") == "":
            return HttpResponseBadRequest("Missing 'subject' parameter")
        if request.POST.get("image", "") == "":
            return HttpResponseBadRequest("Missing 'image' parameter")

        subject = request.POST.get("subject")
        image = base64.b64decode(request.POST.get("image"))
        save_debug_image(image, request)

        if request.POST.get("owner", "") != "":
            owner = request.POST.get("owner")
            logger.info(prepend_ip("Guest image for %s from owner %s" %(subject, owner), request))

        logger.debug(prepend_ip("Performing detection process", request))
        start = time.time()
        image2 = imread(io.BytesIO(image))
        rects = fd.detect_faces(image2)
        elapsed = "%.3f" %((time.time() - start)*1000)

        # Create a JSON response to be returned in a consistent manner
        if len(rects) == 0:
            ret = {"success": "false"}
        else:
            ret = {"success": "true", "rects": rects.tolist()}
            # Save image to subject's directory
            myFaceRecognizer.save_subject_image(subject, image)

        logger.info(prepend_ip("%s ms to detect rectangles: %s" %(elapsed, ret), request))
        json_ret = json.dumps(ret)
        return HttpResponse(json_ret)

    return HttpResponse("Must send frame as a POST")

@csrf_exempt
def openpose_detect(request):
    """
    Use OpenPose to extract human skeleton points from a given image.
    """
    if myOpenPose is None:
        error = "OpenPose not supported on this server"
        logger.error(prepend_ip("%s" %error, request))
        return HttpResponse(error, status=501)

    if request.method == 'POST':
        logger.debug(prepend_ip("Request received: %s" %request, request))
        if request.POST.get("image", "") == "":
            return HttpResponseBadRequest("Missing 'image' parameter")
        image = base64.b64decode(request.POST.get("image"))
        logger.debug(prepend_ip("Size of received image: %d" %(len(request.POST.get("image"))), request))
        save_debug_image(image, request)

        image = imread(io.BytesIO(image))

        logger.debug(prepend_ip("Performing detection process", request))
        start = time.time()
        # Output poses and the image with the human skeleton blended on it
        poses, output_image = myOpenPose.forward(image, True) #TODO: Don't generate output_image
        # Return the human pose poses, i.e., a [#people x #poses x 3]-dimensional numpy object with the poses of all the people on that image
        # print(poses.tolist())
        elapsed = "%.3f" %((time.time() - start)*1000)
        poses2 = np.around(poses, decimals=6)
        # poses2 = np.rint(poses)

        # Create a JSON response to be returned in a consistent manner
        if len(poses) == 0:
            ret = {"success": "false"}
        else:
            ret = {"success": "true", "server_processing_time": elapsed, "poses": poses2.tolist()}
            save_rendered_pose_image(output_image, request)

        logger.info(prepend_ip("%s ms to detect %d poses: %s" %(elapsed, len(poses), ret), request))
        json_ret = json.dumps(ret)
        return HttpResponse(json_ret)

    return HttpResponse("Must send frame as a POST", status=400)

def usage_gpu(request):
    """
    Execute the "nvidia-smi" command and parse the output to get the GPU usage.
    """
    nvidia_smi = "nvidia-smi"
    try:
        p = Popen([nvidia_smi,"-q", "-d", "UTILIZATION"], stdout=PIPE)
    except FileNotFoundError:
        logger.error("%s not found. GPU usage not supported." %nvidia_smi)
        return {} # Empty dict

    stdout, stderror = p.communicate()
    output = stdout.decode('UTF-8')
    # Split on line break
    lines = output.split(os.linesep)
    gpu_utilization_snapshot_section = False
    gpu_utilization_samples_section = False
    memory_utilization_samples_section = False
    gpu_utilization_snapshot = -1
    gpu_utilization_high = -1
    memory_utilization_high = -1

    for line in lines:
        # print(line, gpu_utilization_snapshot_section, gpu_utilization_samples_section, memory_utilization_samples_section)
        if line.strip() == "Utilization":
            gpu_utilization_snapshot_section = True
            gpu_utilization_samples_section = False
            memory_utilization_samples_section = False
        elif line.strip() == "GPU Utilization Samples":
            gpu_utilization_snapshot_section = False
            gpu_utilization_samples_section = True
            memory_utilization_samples_section = False
        elif line.strip() == "Memory Utilization Samples":
            gpu_utilization_snapshot_section = False
            gpu_utilization_samples_section = False
            memory_utilization_samples_section = True
        elif line.strip() == "ENC Utilization Samples":
            # When we hit this line, we're done
            break
        elif line.strip().startswith("Gpu"):
            if gpu_utilization_snapshot_section:
                vals = line.split()
                gpu_utilization_snapshot = vals[2]
        elif line.strip().startswith("Max"):
            if gpu_utilization_samples_section:
                vals = line.split()
                gpu_utilization_high = vals[2]
            elif memory_utilization_samples_section:
                vals = line.split()
                memory_utilization_high = vals[2]

    ret = {"gpu_utilization_snapshot": gpu_utilization_snapshot,
            "gpu_utilization_high": gpu_utilization_high,
            "gpu_memory_utilization_high": memory_utilization_high}
    logger.info(prepend_ip("GPU Stats: %s" %(ret), request))
    return ret

def usage_cpu_and_mem(request):
    """
    Return the cpu and memory usage in percent.
    """
    import psutil
    cpu = psutil.cpu_percent()
    mem = dict(psutil.virtual_memory()._asdict())['percent']
    ret = {"cpu_utilization": cpu,
            "mem_utilization": mem}
    logger.info(prepend_ip("CPU Stats: %s" %(ret), request))
    return ret

@csrf_exempt
def server_usage(request):
    """
    Get CPU and GPU usage summary.
    """
    if request.method == 'GET':
        ret1 = usage_cpu_and_mem(request)
        ret2 = usage_gpu(request)
        # Merge the dictionaries together.
        ret = ret1.copy()
        ret.update(ret2)
        json_ret = json.dumps(ret)
        return HttpResponse(json_ret)

    return HttpResponse("This request must be a GET")
