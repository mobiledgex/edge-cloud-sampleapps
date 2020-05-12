# Copyright 2020 MobiledgeX, Inc. All rights and licenses reserved.
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

"""
This module implements Consumer classes for processing images received over
a websocket. Depending on the type of consumer (e.g. Face Detector, Object
Detector), the image is processed and coordinates are returned to the client.
"""
from channels.generic.websocket import WebsocketConsumer
import json
import time
import io
import logging
import numpy as np
from PIL import Image
from imageio import imread, imwrite
from tracker.apps import myFaceDetector, myFaceRecognizer, myOpenPose, myOpWrapper, myObjectDetector

logger = logging.getLogger(__name__)

class ImageConsumerFaceDetector(WebsocketConsumer):
    def connect(self):
        self.accept()
        logger.info("ImageConsumerFaceDetector")

    def disconnect(self, close_code):
        logger.info("disconnect. close_code=%s" %close_code)

    def receive(self, text_data=None, bytes_data=None):
        if bytes_data != None:
            logger.info("bytes_data length=%d" %(len(bytes_data)))
            start = time.time()
            image = imread(io.BytesIO(bytes_data))
            rects = myFaceDetector.detect_faces(image)
            elapsed = "%.3f" %((time.time() - start)*1000)
            if len(rects) == 0:
                ret = {"success": "false", "server_processing_time": elapsed}
            else:
                ret = {"success": "true", "server_processing_time": elapsed, "rects": rects.tolist()}
            response = json.dumps(ret)
        else:
            logger.info("text_data=%s" %(text_data))
            # If text is received, just echo back what we received.
            response = text_data

        logger.info("response=%s" %response)
        self.send(text_data=response)

class ImageConsumerFaceRecognizer(WebsocketConsumer):
    def connect(self):
        self.accept()
        logger.info("ImageConsumerFaceRecognizer")

    def disconnect(self, close_code):
        logger.info("disconnect. close_code=%s" %close_code)

    def receive(self, text_data=None, bytes_data=None):
        if bytes_data != None:
            logger.info("bytes_data length=%d" %(len(bytes_data)))
            if myFaceRecognizer.is_update_in_progress() or myFaceRecognizer.read_training_data_if_needed():
                error = "Training data update in progress"
                ret = {"success": "false", "error": error, "server_processing_time": 0}
                response = json.dumps(ret)
            else:
                start = time.time()
                image = imread(io.BytesIO(bytes_data))
                predicted_img, subject, confidence, rect = myFaceRecognizer.predict(image)
                elapsed = "%.3f" %((time.time() - start)*1000)
                # Create a JSON response to be returned in a consistent manner
                if subject is None:
                    ret = {"success": "false", "server_processing_time": elapsed}
                else:
                    # Convert rect from [x, y, width, height] to [x, y, right, bottom]
                    rect2 = rect.tolist()
                    rect2 = [int(rect[0]), int(rect[1]), int(rect[0]+rect[2]), int(rect[1]+rect[3])]
                    ret = {"success": "true", "subject": subject, "confidence":"%.3f" %confidence, "server_processing_time": elapsed, "rect": rect2}
                response = json.dumps(ret)
        else:
            logger.info("text_data=%s" %(text_data))
            # If text is received, just echo back what we received.
            response = text_data

        logger.info("response=%s" %response)
        self.send(text_data=response)

class ImageConsumerOpenposeDetector(WebsocketConsumer):
    def connect(self):
        self.accept()
        logger.info("ImageConsumerOpenposeDetector")

    def disconnect(self, close_code):
        logger.info("disconnect. close_code=%s" %close_code)

    def receive(self, text_data=None, bytes_data=None):
        if bytes_data != None:

            if myOpenPose == None:
                error = "OpenPose not supported"
                logger.error(error)
                ret = {"success": "false", "error": error, "server_processing_time": 0}
            else:
                image = imread(io.BytesIO(bytes_data))
                logger.debug("Performing pose detection process")
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

            response = json.dumps(ret)
        else:
            logger.info("text_data=%s" %(text_data))
            # If text is received, just echo back what we received.
            response = text_data

        logger.info("response=%s" %response)
        self.send(text_data=response)

class ImageConsumerObjectDetector(WebsocketConsumer):
    def connect(self):
        self.accept()
        logger.info("ImageConsumerObjectDetect")

    def disconnect(self, close_code):
        logger.info("disconnect. close_code=%s" %close_code)

    def receive(self, text_data=None, bytes_data=None):
        if bytes_data != None:
            logger.info("bytes_data length=%d" %(len(bytes_data)))
            start = time.time()
            image = imread(io.BytesIO(bytes_data))
            try:
                pillow_image = Image.fromarray(image, 'RGB')
                objects = myObjectDetector.process_image(pillow_image)
            except Exception as e:
                logger.error("Could not process image. Exception: %s" %e)
                objects = []
            elapsed = "%.3f" %((time.time() - start)*1000)
            if len(objects) == 0:
                ret = {"success": "false", "server_processing_time": elapsed, "gpu_support": myObjectDetector.is_gpu_supported()}
            else:
                ret = {"success": "true", "server_processing_time": elapsed, "gpu_support": myObjectDetector.is_gpu_supported(), "objects": objects}
            response = json.dumps(ret)
        else:
            logger.info("text_data=%s" %(text_data))
            # If text is received, just echo back what we received.
            response = text_data

        logger.info("response=%s" %response)
        self.send(text_data=response)
