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
    #TODO: Remove this short circuit.
    # ret = {"success": "true", "server_processing_time": "46.774", "poses": [[[140.217041015625, 33.9814338684082, 0.855783998966217], [148.18716430664062, 47.05922317504883, 0.8073030114173889], [134.5381622314453, 46.49559783935547, 0.7433509826660156], [129.98204040527344, 63.53919219970703, 0.09135700017213821], [0.0, 0.0, 0.0], [162.3600311279297, 47.08113479614258, 0.6943539977073669], [169.74600219726562, 70.35509490966797, 0.11275900155305862], [167.47653198242188, 89.67330169677734, 0.05759799852967262], [151.57713317871094, 95.90613555908203, 0.6147810220718384], [142.4716796875, 96.47975158691406, 0.591094970703125], [148.15391540527344, 129.99000549316406, 0.7776619791984558], [150.4395294189453, 162.9218292236328, 0.7277380228042603], [161.19972229003906, 95.34200286865234, 0.5732889771461487], [166.33120727539062, 130.55551147460938, 0.7571820020675659], [170.3167724609375, 166.33924865722656, 0.7129030227661133], [138.52732849121094, 30.032238006591797, 0.9252830147743225], [143.6241455078125, 30.021474838256836, 0.9108629822731018], [0.0, 0.0, 0.0], [152.71224975585938, 30.0571346282959, 0.8420900106430054], [165.762939453125, 173.151123046875, 0.4102669954299927], [170.31964111328125, 173.1571044921875, 0.42427998781204224], [170.86839294433594, 169.75424194335938, 0.5103390216827393], [138.50721740722656, 170.8777313232422, 0.5361430048942566], [138.51995849609375, 169.73683166503906, 0.4449099898338318], [152.7230987548828, 166.353515625, 0.6322979927062988]], [[61.829734802246094, 29.450944900512695, 0.894993007183075], [68.64557647705078, 43.105831146240234, 0.7989699840545654], [52.19171905517578, 46.490821838378906, 0.7328230142593384], [49.33973693847656, 70.34864044189453, 0.163674995303154], [0.0, 0.0, 0.0], [83.99562072753906, 39.70023727416992, 0.6078220009803772], [101.59304809570312, 35.702667236328125, 0.08027199655771255], [0.0, 0.0, 0.0], [70.90882873535156, 93.07478332519531, 0.6763190031051636], [61.252159118652344, 93.63552856445312, 0.642524003982544], [60.7076301574707, 124.31036376953125, 0.7539929747581482], [61.24055862426758, 157.83265686035156, 0.7556020021438599], [80.57499694824219, 92.50741577148438, 0.6190429925918579], [83.41342163085938, 122.04427337646484, 0.7001540064811707], [84.0037612915039, 153.29306030273438, 0.699379026889801], [59.57147216796875, 26.06795310974121, 0.8086360096931458], [65.22459411621094, 25.488332748413086, 0.872825026512146], [0.0, 0.0, 0.0], [70.95497131347656, 25.48764991760254, 0.8422399759292603], [81.71179962158203, 160.09774780273438, 0.5181710124015808], [85.13970947265625, 160.6505584716797, 0.5170609951019287], [83.40462493896484, 157.2342987060547, 0.47983700037002563], [51.615867614746094, 166.3544158935547, 0.577754020690918], [50.48329544067383, 165.1914825439453, 0.5347509980201721], [62.98095703125, 161.79635620117188, 0.605521023273468]], [[110.67249298095703, 24.910783767700195, 0.8258240222930908], [115.21610260009766, 39.12099075317383, 0.8262280225753784], [99.31431579589844, 38.52540588378906, 0.6636630296707153], [83.41853332519531, 34.55512237548828, 0.15439699590206146], [0.0, 0.0, 0.0], [129.978271484375, 40.25808334350586, 0.6964200139045715], [147.01925659179688, 43.65511703491211, 0.08808299899101257], [0.0, 0.0, 0.0], [110.10071563720703, 89.66990661621094, 0.6460099816322327], [98.74935913085938, 88.53378295898438, 0.6630510091781616], [93.66110229492188, 124.85398864746094, 0.7648860216140747], [94.7891845703125, 156.6794891357422, 0.728859007358551], [120.32310485839844, 91.36952209472656, 0.6626200079917908], [120.34900665283203, 126.0211181640625, 0.7778199911117554], [120.92923736572266, 157.26345825195312, 0.7836120128631592], [108.41651916503906, 20.97446060180664, 0.7783929705619812], [112.9588394165039, 21.518024444580078, 0.8008750081062317], [0.0, 0.0, 0.0], [121.47666931152344, 24.892406463623047, 0.78233802318573], [115.8093490600586, 166.92015075683594, 0.648514986038208], [120.90342712402344, 166.91647338867188, 0.6432250142097473], [122.59953308105469, 160.64761352539062, 0.5930709838867188], [91.36873626708984, 162.35597229003906, 0.47964999079704285], [89.0844497680664, 161.23672485351562, 0.5341489911079407], [98.18624877929688, 160.63973999023438, 0.6323689818382263]], [[179.3692626953125, 20.94101905822754, 0.865077018737793], [187.90843200683594, 39.10340881347656, 0.7600399851799011], [172.58885192871094, 39.143959045410156, 0.6378120183944702], [165.76829528808594, 59.56922912597656, 0.06702399998903275], [0.0, 0.0, 0.0], [202.0955810546875, 38.55139923095703, 0.6781100034713745], [207.23101806640625, 65.81729888916016, 0.07877299934625626], [0.0, 0.0, 0.0], [186.783935546875, 94.78202819824219, 0.49267399311065674], [177.6940460205078, 95.34146118164062, 0.4673070013523102], [190.18902587890625, 135.67245483398438, 0.3605799973011017], [194.7380828857422, 167.48175048828125, 0.4411340057849884], [195.86941528320312, 94.78109741210938, 0.4742330014705658], [196.99986267089844, 135.1138153076172, 0.619642972946167], [198.146728515625, 170.30406188964844, 0.7298579812049866], [176.56336975097656, 17.553466796875, 0.7710520029067993], [183.36219787597656, 17.547155380249023, 0.8256949782371521], [0.0, 0.0, 0.0], [192.44847106933594, 20.948150634765625, 0.8317880034446716], [188.46658325195312, 175.99795532226562, 0.5790280103683472], [192.4636688232422, 177.13600158691406, 0.5946490168571472], [202.10841369628906, 173.14923095703125, 0.604744017124176], [185.64906311035156, 174.874267578125, 0.23726199567317963], [186.2127227783203, 173.72457885742188, 0.2325430065393448], [198.705322265625, 170.88296508789062, 0.13290899991989136]], [[24.925033569335938, 28.896621704101562, 0.6396340131759644], [29.436925888061523, 43.65038299560547, 0.8419420123100281], [13.00137710571289, 46.500160217285156, 0.7483940124511719], [8.458451271057129, 70.3475341796875, 0.803941011428833], [13.55638313293457, 93.08673095703125, 0.7765750288963318], [43.667205810546875, 40.818668365478516, 0.680325984954834], [56.71033477783203, 58.43655014038086, 0.07609400153160095], [0.0, 0.0, 0.0], [30.012380599975586, 95.92049407958984, 0.7006629705429077], [20.375293731689453, 96.48204040527344, 0.6656090021133423], [17.53715705871582, 128.84715270996094, 0.7322999835014343], [16.972373962402344, 158.96372985839844, 0.737375020980835], [39.14030075073242, 95.91390228271484, 0.6547290086746216], [38.56514358520508, 127.15097045898438, 0.7622990012168884], [35.15015411376953, 157.26840209960938, 0.7612159848213196], [21.526596069335938, 25.49319839477539, 0.6686369776725769], [27.75245475769043, 24.94712257385254, 0.6567040085792542], [19.231298446655273, 27.196395874023438, 0.29245999455451965], [33.44400405883789, 25.481515884399414, 0.5879849791526794], [34.01506805419922, 165.20509338378906, 0.6931009888648987], [38.54868698120117, 165.18643188476562, 0.7070419788360596], [34.023948669433594, 160.6499786376953, 0.6224269866943359], [18.10492706298828, 165.7644500732422, 0.6125959753990173], [14.707441329956055, 165.21133422851562, 0.5993720293045044], [18.678239822387695, 161.7952423095703, 0.5808929800987244]], [[214.03392028808594, 20.95455551147461, 0.8270670175552368], [224.8245849609375, 41.398406982421875, 0.702010989189148], [210.06459045410156, 42.527427673339844, 0.5311610102653503], [0.0, 0.0, 0.0], [0.0, 0.0, 0.0], [239.03501892089844, 39.68785095214844, 0.6966059803962708], [252.67315673828125, 70.36243438720703, 0.5955899953842163], [253.22274780273438, 94.20330047607422, 0.5151680111885071], [223.13076782226562, 102.17243194580078, 0.5718050003051758], [212.92138671875, 102.158203125, 0.530085027217865], [220.8491973876953, 143.61329650878906, 0.7091180086135864], [228.22828674316406, 185.0751495361328, 0.6428179740905762], [232.21881103515625, 102.73410034179688, 0.5221750140190125], [229.9349365234375, 144.1981658935547, 0.7265999913215637], [241.30165100097656, 189.05661010742188, 0.6749200224876404], [211.771484375, 18.67261505126953, 0.7580999732017517], [216.8799285888672, 17.533422470092773, 0.8235880136489868], [0.0, 0.0, 0.0], [227.0995330810547, 20.352230072021484, 0.7542660236358643], [228.80389404296875, 198.7228546142578, 0.6576110124588013], [233.35411071777344, 200.40699768066406, 0.6256970167160034], [244.72166442871094, 193.59169006347656, 0.5752480030059814], [215.7387237548828, 189.62677001953125, 0.4130130112171173], [215.759033203125, 187.9241943359375, 0.396928995847702], [229.96151733398438, 189.07241821289062, 0.41249901056289673]]]}
    # json_ret = json.dumps(ret)
    # return HttpResponse(json_ret)

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
