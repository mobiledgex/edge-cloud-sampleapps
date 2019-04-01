from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt
from django.http import HttpResponse, HttpResponseBadRequest
from django.utils import timezone
import logging
import gzip
import base64
import time
import io
import json
from imageio import imread, imwrite
from facerec.FaceRecognizer import FaceRecognizer
from trainer.apps import myFaceRecognizer
from trainer.models import Subject, Owner

logger = logging.getLogger(__name__)

@csrf_exempt
def index(request):
    return HttpResponse("Hello, world. You're at the trainer index.")

@csrf_exempt
def test_connection(request):
    """ Test the connection to the backend """
    if request.method == 'GET':
        logger.info("/test/ Valid GET Request received")
        return HttpResponse("Valid GET Request to server")
    return HttpResponseBadRequest("Please send response as a GET")

@csrf_exempt
def download(request):
    file = gzip.compress(open(myFaceRecognizer.training_data_filepath, 'rb').read())
    logger.info("Sending %s, gzip compressed to %d bytes" %(myFaceRecognizer.training_data_filename, len(file)))
    response = HttpResponse(file)
    response['Content-Disposition'] = 'attachment; filename="%s"' %myFaceRecognizer.training_data_filename
    response['Content-Type'] = 'application/x-yaml'
    response['Content-Encoding'] = 'gzip'
    response['Last-Modified'] = int(myFaceRecognizer.get_training_data_timestamp())
    return response

@csrf_exempt
def lastupdate(request):
    ret = int(myFaceRecognizer.get_training_data_timestamp())
    return HttpResponse(ret)

@csrf_exempt
def init(request):
    if request.method != 'POST':
        return HttpResponseBadRequest("/init/ must be a POST")
    myFaceRecognizer.init_database()
    ret = {"success": "true"}
    json_ret = json.dumps(ret)
    logger.info("Returning %s" %(json_ret))
    return HttpResponse(json_ret)

@csrf_exempt
def add(request):
    """
    Perform face detection on received image. If face found, save image to
    subject's training data directory.
    """
    logger.debug("Request received: %s" %request)
    if request.method != 'POST':
        error = "Must send frame as a POST"
        logger.error(error)
        return HttpResponseBadRequest(error)
    if request.content_type != 'application/x-www-form-urlencoded':
        error = "Content-Type must be 'application/x-www-form-urlencoded'"
        logger.error(error)
        return HttpResponseBadRequest(error)

    owner_id = request.POST.get("owner_id", "")
    if owner_id == "":
        error = "Missing 'owner_id' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)
    owner_name = request.POST.get("owner_name", "")
    if owner_name == "":
        error = "Missing 'owner_name' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)
    subject = request.POST.get("subject", "")
    if subject == "":
        error = "Missing 'subject' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)
    if request.POST.get("image", "") == "":
        error = "Missing 'image' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)

    if owner_name != subject:
        logger.info("Received guest image for subject '%s' from owner '%s'" %(subject, owner_name))
    else:
        logger.info("Received image for subject '%s'" %subject)

    image = base64.b64decode(request.POST.get("image"))
    myFaceRecognizer.save_debug_image(image)
    logger.debug("Performing detection process")
    start = time.time()
    image2 = imread(io.BytesIO(image))
    rects = myFaceRecognizer.detect_faces(image2)
    elapsed = "%.3f" %((time.time() - start)*1000)

    # Create a JSON response to be returned in a consistent manner
    if len(rects) == 0:
        ret = {"success": "false", "reason": "No face detected", "server_processing_time": elapsed}
    elif len(rects) > 1:
        ret = {"success": "false", "reason": "More than 1 face", "server_processing_time": elapsed}
    else:
        try:
            ret = {"success": "true", "rects": rects.tolist(), "server_processing_time": elapsed}
            # Save image to subject's directory
            myFaceRecognizer.save_subject_image(subject, image)

            # Save to DB
            owner_record, created = Owner.objects.get_or_create(id = owner_id, name = owner_name)
            owner_record.save()
            subject_record, created = Subject.objects.get_or_create(name = subject, owner = owner_record)
            # subject_record.owner = owner_record
            subject_record.save()
        except Exception as e:
            logger.error(e)
            return HttpResponseBadRequest(e)

    json_ret = json.dumps(ret)
    logger.info("Returning %s" %(json_ret))
    return HttpResponse(json_ret)

@csrf_exempt
def train(request):
    if request.method != 'POST':
        return HttpResponseBadRequest("/train/ must be a POST")

    owner_name = request.POST.get("owner_name", "")
    if owner_name == "":
        error = "Missing 'owner_name' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)
    subject = request.POST.get("subject", "")
    if subject == "":
        error = "Missing 'subject' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)

    try:
        subject_record = Subject.objects.get(name = subject)
    except Exception as e:
        logger.error(e)
        return HttpResponseBadRequest(e)

    logger.info("Owner '%s' performing training for subject '%s'" %(owner_name, subject))

    start = time.time()

    myFaceRecognizer.update_training_data()
    subject_record.in_training = True
    subject_record.save()

    myFaceRecognizer.read_trained_data()

    elapsed = "%.3f" %((time.time() - start)*1000)
    ret = {"success": "true", "server_processing_time": elapsed}
    json_ret = json.dumps(ret)
    logger.info("Returning %s" %(json_ret))
    return HttpResponse(json_ret)

@csrf_exempt
def train_SINGLE_BROKEN(request):
    # TODO: update_training_data_single is broken. Either fix the "single()"
    # stuff, or remove it entirely.
    if request.method != 'POST':
        return HttpResponseBadRequest("/train/ must be a POST")

    subject = request.POST.get("subject", "")
    if subject == "":
        error = "Missing 'subject' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)

    try:
        subject_record = Subject.objects.get(name = subject)
    except Exception as e:
        logger.error(e)
        return HttpResponseBadRequest(e)

    start = time.time()

    existing_subject = False
    if subject_record.in_training:
        existing_subject = True
        logger.info("Subject %s already in training. Do full re-train." %subject)
        myFaceRecognizer.update_training_data()
    else:
        logger.info("%s is a new subject. Do update only." %subject)
        myFaceRecognizer.update_training_data_single(subject)
        subject_record.in_training = True
        subject_record.save()

    myFaceRecognizer.read_trained_data()

    elapsed = "%.3f" %((time.time() - start)*1000)
    ret = {"success": "true", "existing_subject": existing_subject, "server_processing_time": elapsed}
    json_ret = json.dumps(ret)
    logger.info("Returning %s" %(json_ret))
    return HttpResponse(json_ret)

@csrf_exempt
def remove(request):
    owner_id = request.POST.get("owner_id", "")
    if owner_id == "":
        error = "Missing 'owner_id' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)
    owner_name = request.POST.get("owner_name", "")
    if owner_name == "":
        error = "Missing 'owner_name' parameter"
        logger.error(error)
        return HttpResponseBadRequest(error)
    subject_name = request.POST.get("subject", "")

    try:
        # Remove images from disk and entries from DB.
        # If the subject name is included, only remove that subject.
        # Otherwise, remove all subjects that have the specified owner.
        owner_record = Owner.objects.get(id = owner_id)
        if subject_name == "":
            logger.info("No 'subject' parameter. Will delete all subjects owned by %s" %owner_id)
            subject_set = Subject.objects.filter(owner = owner_record)
            for subject_record in subject_set:
                if subject_record.name != owner_name:
                    logger.info("Removing owner %s's subject: %s" %(owner_name, subject_record.name))
                    myFaceRecognizer.remove_subject(subject_record.name)
                else:
                    logger.info("Skipping owner %s's self" %(owner_name))
            myFaceRecognizer.remove_subject(owner_record.name)
            owner_record.delete()
        else:
            logger.info("subject %s supplied. Only deleting subject." %subject_name)
            subject_record = Subject.objects.get(name = subject_name)
            subject_record.delete()
            myFaceRecognizer.remove_subject(subject_name)

        myFaceRecognizer.update_training_data()
        myFaceRecognizer.read_trained_data()

    except Exception as e:
        logger.error(e)
        return HttpResponseBadRequest(e)

    return HttpResponse("OK")

@csrf_exempt
def predict(request):
    """
    Runs facial recognition on received image, using pre-trained dataset.
    Returns subject name, and rectangle coordinates of recognized face.
    """
    logger.debug("Request received: %s" %request)
    if request.method != 'POST':
        return HttpResponseBadRequest("Must send frame as a POST")
    if request.content_type != 'application/x-www-form-urlencoded':
        return HttpResponseBadRequest("Content-Type must be 'application/x-www-form-urlencoded'")

    if request.POST.get("image", "") == "":
        return HttpResponseBadRequest("Missing 'image' parameter")
    image = base64.b64decode(request.POST.get("image"))
    # save_debug_image(image, request)
    logger.debug("Performing recognition process")
    now = time.time()
    image = imread(io.BytesIO(image))
    subject, confidence, rect = myFaceRecognizer.predict(image)
    elapsed = "%.3f" %((time.time() - now)*1000)

    # Create a JSON response to be returned in a consistent manner
    if subject is None:
        ret = {"success": "false", "server_processing_time": elapsed}
    else:
        # Convert rect from [x, y, width, height] to [x, y, right, bottom]
        rect2 = rect.tolist()
        rect2 = [int(rect[0]), int(rect[1]), int(rect[0]+rect[2]), int(rect[1]+rect[3])]
        if confidence >= 105:
            subject = "Unknown"
        ret = {"success": "true", "subject": subject, "confidence":"%.3f" %confidence, "server_processing_time": elapsed, "rect": rect2}
    logger.info("Returning: %s" %(ret))
    json_ret = json.dumps(ret)
    return HttpResponse(json_ret)
