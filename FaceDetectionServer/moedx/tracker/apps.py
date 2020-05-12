# Copyright 2019-2020 MobiledgeX, Inc. All rights and licenses reserved.
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


import sys
import logging
from django.apps import AppConfig
import threading
import time
import os

logger = logging.getLogger(__name__)

myFaceRecognizer = None
myFaceDetector = None
myObjectDetector = None
myOpenPose = None
myOpWrapper = None

class TrackerConfig(AppConfig):
    name = 'tracker'

    def ready(self):
        # These global variables will be used by views.py.
        global myFaceRecognizer
        global myFaceDetector
        global myObjectDetector
        global myOpenPose
        global myOpWrapper

        # If this script is called by manage.py with with either "makemigrations" or "migrate",
        # we don't need to continue with initialization.
        if len(sys.argv) >= 2 and sys.argv[0] == "manage.py" and "migrat" in sys.argv[1]:
            logger.info("Called by '%s %s'. Aborting app initialization." %(sys.argv[0], sys.argv[1]))
            return

        from facial_detection.face_detector import FaceDetector
        from facial_detection.face_recognizer import FaceRecognizer
        from object_detection.object_detector import ObjectDetector

        myFaceRecognizer = FaceRecognizer()
        logger.info("Created myFaceRecognizer")

        myFaceDetector = FaceDetector()
        logger.info("Created myFaceDetector")

        myObjectDetector = ObjectDetector()
        logger.info("Created myObjectDetector")

        supportOpenPose = False

        # Our docker container has the openpose libraries at the root
        # Note that the "docker run" command must include the "--gpus all" parameter. Example:
        # sudo docker run --gpus all --net=host mobiledgex/mobiledgexsdkdemo20:20200423
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
            # Starting the wrapper fails during "docker build" because --runtime=nvidia is not available.
            try:
                myOpWrapper.start()
                logger.info("Created OpenPose wrapper")
            except:
                logger.error("Failed to start OpenPose wrapper")
                logger.warn('/openpose/ web services calls will not be allowed.')
                myOpenPose = None
                myOpWrapper = None
