from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt

from tracker.serializers import DetectedSerializer
from facial_detection.facedetector import FaceDetector
from facial_detection.faceRecognizer import FaceRecognizer
from tracker.apps import myFaceRecognizer
from django.http import HttpResponse, HttpResponseBadRequest

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
import socket

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
        save_debug_image(image, request)

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

@csrf_exempt
def frame_detect_json(request):
    """
    Runs facial detection on a frame that is sent via a REST
    API call and returns a JSON formatted set of coordinates.
    """
    if request.method == 'POST':
        logger.debug(prepend_ip("Request received: %s" %request, request))
        image = base64.b64decode(request.body)
        save_debug_image(image, request)
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

@csrf_exempt
def frame_detect_multi_json(request):
    """
    Runs facial detection on a frame that is sent via a REST
    API call and returns a JSON formatted set of coordinates.
    """
    if request.method == 'POST':
        logger.debug(prepend_ip("Request received: %s" %request, request))
        image = base64.b64decode(request.body)
        save_debug_image(image, request)

        logger.debug(prepend_ip("Performing detection process", request))
        now = time.time()
        image = imread(io.BytesIO(image))
        rects = fd.detect_faces(image)
        elapsed = "%.3f" %((time.time() - now)*1000)

        # Create a JSON response to be returned in a consistent manner
        if len(rects) == 0:
            ret = [[0, 0, 0, 0]]
        else:
            ret = rects.tolist()
        logger.info(prepend_ip("%s ms to detect rectangles: %s" %(elapsed, ret), request))
        json_ret = json.dumps(ret)

        return HttpResponse(json_ret)

    return HttpResponse("Must send frame as a POST")

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
            ret = {"success": "true", "rects": rects.tolist()}
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
        """
        HOST, PORT = "localhost", 8001
        data = "This is what I would have typed.\n"

        # Create a socket (SOCK_STREAM means a TCP socket)
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        try:
            # Connect to server and send data
            sock.connect((HOST, PORT))
            sock.sendall(data.encode('utf-8'))

            # Receive data from the server and shut down
            received = sock.recv(1024)
        finally:
            sock.close()

        print("Sent:     {}".format(data))
        print("Received: {}".format(received))

        """
        """
        #"session" solution doesn't work because FaceRecognizer isn't JSON serializable
        myFaceRecognizer = request.session.get('face_recognizer')
        if myFaceRecognizer == None:
            myFaceRecognizer = FaceRecognizer()
            logger.info("Created myFaceRecognizer")
            myFaceRecognizer.read_trained_data()
            request.session['face_recognizer'] = myFaceRecognizer
        else:
            logger.info("Using existing myFaceRecognizer")
        # """
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
            ret = {"success": "true", "subject": subject, "confidence":"%.3f" %confidence, "rect": rect2}
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
