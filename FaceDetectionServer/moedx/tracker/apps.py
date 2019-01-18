from django.apps import AppConfig
from facial_detection.faceRecognizer import FaceRecognizer
import logging
import sys

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
        logger.info("Calling update_training_data()")
        myFaceRecognizer.update_training_data()
        logger.info("update_training_data() complete")
        logger.info("Calling read_trained_data()")
        myFaceRecognizer.read_trained_data()
        logger.info("read_trained_data() complete")

        global myOpenPose
        sys.path.append('/home/mobiledgex/openpose/build/python') #TODO: Fix this!
        try:
            from openpose import openpose as op
        except:
            raise Exception('Error: OpenPose library could not be found. Did you enable `BUILD_PYTHON` in CMake and have this Python script in the right folder?')

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
        params["default_model_folder"] = "/home/mobiledgex/openpose/models/" #TODO: Fix this!
        # Construct OpenPose object allocates GPU memory
        myOpenPose = op.OpenPose(params)
        logger.info("Created myOpenPose")
