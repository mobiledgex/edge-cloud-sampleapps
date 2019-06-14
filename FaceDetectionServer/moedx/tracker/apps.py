from django.apps import AppConfig
from facial_detection.facedetector import FaceDetector
from facial_detection.faceRecognizer import FaceRecognizer
from facial_detection.tcp_server import ThreadedTCPServer, ThreadedTCPRequestHandler
import threading
import logging
import sys
import time

logger = logging.getLogger(__name__)

class TrackerConfig(AppConfig):
    name = 'tracker'

    def ready(self):
        # This global variable will be used by views.py.
        global myFaceRecognizer
        myFaceRecognizer = FaceRecognizer()
        logger.info("Created myFaceRecognizer")

        global myFaceDetector
        myFaceDetector = FaceDetector()

        global myOpenPose
        global myOpWrapper
        supportOpenPose = False

        sys.path.append('/openpose/build/python')
        try:
            from openpose import pyopenpose as myOpenPose
            supportOpenPose = True
        except:
            logger.error('Error: OpenPose library could not be found. Did you enable `BUILD_PYTHON` in CMake and have this Python script in the right folder?')
            logger.warn('/openpose/ web services calls will not be allowed.')
            myOpenPose = None
            myOpWrapper = None

        if supportOpenPose:
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
            myOpWrapper = myOpenPose.WrapperPython()
            myOpWrapper.configure(params)
            myOpWrapper.start()
            logger.info("Created OpenPose wrapper")

        # Start the non-REST session-based TCP server.
        HOST, PORT = "0.0.0.0", 8011

        try:
            server = ThreadedTCPServer((HOST, PORT), ThreadedTCPRequestHandler)
            server.setFaceDetector(myFaceDetector)
            server.setFaceRecognizer(myFaceRecognizer)
            server.setOpenPose(myOpenPose, myOpWrapper)
            ip, port = server.server_address
            logger.info("ip=%s port=%d" %(ip, port))

            # Start a thread with the server -- that thread will then start one
            # more thread for each request
            server_thread = threading.Thread(target=server.serve_forever)
            # Exit the server thread when the main thread terminates
            server_thread.daemon = True
            server_thread.start()
            logger.info("Server loop running in thread: %s" %server_thread.name)
        except OSError:
            logger.warn("Server already running on port %d" %PORT)
