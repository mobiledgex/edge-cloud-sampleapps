from django.apps import AppConfig
from facerec.FaceRecognizer import FaceRecognizer
import logging

logger = logging.getLogger(__name__)

class TrainerConfig(AppConfig):
    name = 'trainer'

    def ready(self):
        # This global variable will be used by views.py.
        # read_trained_data() only needs to be read once here, instead
        # of every time a request comes in.``
        global myFaceRecognizer
        myFaceRecognizer = FaceRecognizer()
        logger.info("Created myFaceRecognizer")
