from django.apps import AppConfig
from facial_detection.faceRecognizer import FaceRecognizer
import threading
import logging
import sys
import time

logger = logging.getLogger(__name__)

class TrackerConfig(AppConfig):
    name = 'tracker'

    def ready(self):
        # This global variable will be used by views.py.
        # read_trained_data() only needs to be read once here, instead
        # of every time a request comes in.``
        global myFaceRecognizer
        myFaceRecognizer = FaceRecognizer()
        logger.info("Created myFaceRecognizer")

        global myOpenPose
        global opWrapper
        sys.path.append('/openpose/build/python')
        try:
            from openpose import pyopenpose as myOpenPose
        except:
            logger.error('Error: OpenPose library could not be found. Did you enable `BUILD_PYTHON` in CMake and have this Python script in the right folder?')
            logger.warn('/openpose/ web services calls will not be allowed.')
            myOpenPose = None
            opWrapper = None
            return

        params = dict()
        params["logging_level"] = 3
        params["output_resolution"] = "-1x-1"
        params["net_resolution"] = "-1x368"
        params["model_pose"] = "BODY_25"
        params["alpha_pose"] = 0.6
        params["scale_gap"] = 0.3
        params["scale_number"] = 1
        params["render_threshold"] = 0.05
        # If GPU version is built, and multiple GPUs are available, set the ID here
        params["num_gpu_start"] = 0
        params["disable_blending"] = False
        # Ensure you point to the correct path where models are located
        params["model_folder"] = "/openpose/models/"

        # Starting OpenPose
        opWrapper = myOpenPose.WrapperPython()
        opWrapper.configure(params)
        opWrapper.start()
        logger.info("Created OpenPose wrapper")
