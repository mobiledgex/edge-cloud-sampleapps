from django.apps import AppConfig
from facial_detection.faceRecognizer import FaceRecognizer
import logging

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
