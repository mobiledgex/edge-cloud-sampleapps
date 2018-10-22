from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt

from tracker.serializers import DetectedSerializer
from facial_detection.facedetector import FaceDetector
from django.http import HttpResponse

import ast
import cv2
import numpy as np
import json
import base64
from imageio import imread
import io
import time
import os
import glob
import json
import logging

logger = logging.getLogger(__name__)

fd = FaceDetector()

@csrf_exempt
def test_connection(request):
    """ Test the connection to the backend """
    if request.method == 'GET':
        return HttpResponse("Valid GET Request to server")
    return HttpResponse("Please send response as a GET")

@csrf_exempt
def frame_detect(request):
    """
    Runs facial detection on a frame that is sent via a REST
    API call
    """
    if request.method == 'POST':
        image = base64.b64decode(request.body)

        #Save current image with timestamp
        timestr = time.strftime("%Y%m%d-%H%M%S")
        fileName = "/tmp/face_"+timestr+".png"
        with open(fileName, "wb") as fh:
            fh.write(image)
        #Delete all old files except the 20 most recent
        files = sorted(glob.glob("/tmp/face_*.png"), key=os.path.getctime, reverse=True)
        #print(files[20:])
        for file in files[20:]:
            #print("removing %s" %file)
            os.remove(file)

        image = imread(io.BytesIO(image))
        rects = fd.detect_faces(image)

        logger.info(prepend_ip("rects %s" %rects, request))

        # Create a byte object to be returned in a consistent manner
        # TODO: This method only handles numbers up to 4095, so if the screen
        # is bigger than that this will need to be changed
        bs = b''
        if len(rects) == 0:
            rects = [[0, 0, 0, 0]]
        for r in rects[0]:
            bs += int(r).to_bytes(3, byteorder='big', signed=True)

        return HttpResponse(bs)

    return HttpResponse("Must send frame as a POST")

def prepend_ip(text, request):
    return "[%s] %s" %(request.META.get('REMOTE_ADDR'), text)

@csrf_exempt
def frame_detect_json(request):
    """
    Runs facial detection on a frame that is sent via a REST
    API call and returns a JSON formatted set of coordinates.
    """
    if request.method == 'POST':
        logger.debug(prepend_ip("Request received", request))
        image = base64.b64decode(request.body)

        logger.debug(prepend_ip("Saving image for debugging", request))
        #Save current image with timestamp
        timestr = time.strftime("%Y%m%d-%H%M%S")
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

        logger.debug(prepend_ip("Performing detection process", request))
        now = time.time()
        image = imread(io.BytesIO(image))
        rects = fd.detect_faces(image)
        elapsed = "%.3f" %((time.time() - now)*1000)
        logger.info(prepend_ip("%s ms to detect rectangles: %s" %(elapsed, rects), request))

        # Create a JSON response to be returned in a consistent manner
        # TODO: Return an array of all rects instead of only the first. Multi-face!
        if len(rects) == 0:
            rects = [[0, 0, 0, 0]]
        ret = {'left': int(rects[0][0]), 'top': int(rects[0][1]), 'right': int(rects[0][2]), 'bottom': int(rects[0][3])}
        json_ret = json.dumps(ret)

        return HttpResponse(json_ret)

    return HttpResponse("Must send frame as a POST")
