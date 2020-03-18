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

import socket
import threading
import socketserver
import time
import io
import cv2
import numpy as np
import json
import base64
from imageio import imread, imwrite
import imghdr
import struct
import logging
from PIL import Image

opcodes = {0:'server_response', 1:'face_det', 2:'face_rec', 3:'pose_det', 4:'obj_det', 4:'ping_rtt'}

logger = logging.getLogger(__name__)

class ThreadedTCPRequestHandler(socketserver.BaseRequestHandler):

    def handle(self):
        logger.info("handle() for %s" %threading.current_thread())
        while True:
            logger.debug("while True for %s" %threading.current_thread())
            opcode, data = self.recv_one_message(self.request)
            if opcode == None:
                logger.info("Opcode=None. Connection closed for %s" %threading.current_thread())
                break;

            logger.info("Opcode: %s len(data)=%d %s" %(opcode, len(data), threading.current_thread()))

            if opcode == 1: #'face_det':
                now = time.time()
                image = imread(io.BytesIO(data))
                rects = myFaceDetector.detect_faces(image)
                elapsed = "%.3f" %((time.time() - now)*1000)
                if len(rects) == 0:
                    ret = {"success": "false", "server_processing_time": elapsed}
                else:
                    ret = {"success": "true", "server_processing_time": elapsed, "rects": rects.tolist()}

            elif opcode == 2: #'face_rec':
                now = time.time()
                image = imread(io.BytesIO(data))
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

            elif opcode == 3: #'pose_det':
                if myOpenPose == None:
                    error = "OpenPose not supported"
                    logger.error(error)
                    ret = {"success": "false", "error": error, "server_processing_time": 0}
                else:
                    now = time.time()
                    image = imread(io.BytesIO(data))
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

            elif opcode == 4: #'obj_det':
                now = time.time()
                image = imread(io.BytesIO(data))
                pillow_image = Image.fromarray(image, 'RGB')
                objects = myObjectDetector.process_image(pillow_image)
                elapsed = "%.3f" %((time.time() - now)*1000)
                # Create a JSON response to be returned in a consistent manner
                if len(objects) == 0:
                    ret = {"success": "false", "server_processing_time": elapsed, "gpu_support": myObjectDetector.is_gpu_supported()}
                else:
                    ret = {"success": "true", "server_processing_time": elapsed, "gpu_support": myObjectDetector.is_gpu_supported(), "objects": objects}

            elif opcode == 5: #'ping_rtt':
                ret = {"success": "pong"}

            else:
                error = "Unsupported opcode: %s" %opcode
                logger.error(error)
                ret = {"success": "false", "error": error, "server_processing_time": 0}

            response = bytes(json.dumps(ret), "utf-8")
            length = len(response)
            self.request.sendall(struct.pack('!I', length))
            self.request.sendall(response)
            # self.request.flush()
            logger.info("Wrote %d bytes to %s: %s" %(length, self.client_address[0], response))

    def recv_one_message(self, sock):
        """
        Decode the byte stream:
        4 bytes (integer) - opcode
        4 bytes (integer) - length (x) of image data
        x bytes - image data
        """
        opcodebuf = self.recvall(sock, 4)
        logger.debug("recv_one_message opcodebuf=%s" %opcodebuf)
        if opcodebuf == None:
            return None, None
        opcode, = struct.unpack('!I', opcodebuf)
        logger.debug("recv_one_message opcode=%s" %opcode)
        lengthbuf = self.recvall(sock, 4)
        logger.debug("recv_one_message lengthbuf=%s" %lengthbuf)
        if lengthbuf == None:
            return None, None
        length, = struct.unpack('!I', lengthbuf)
        logger.debug("recv_one_message length=%d" %length)
        return opcode, self.recvall(sock, length)

    def recvall(self, sock, count):
        buf = b''
        while count:
            newbuf = sock.recv(count)
            if not newbuf: return None
            buf += newbuf
            count -= len(newbuf)
        return buf

class ThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    pass

    def setFaceDetector(self, faceDetector):
        global myFaceDetector
        myFaceDetector = faceDetector
    def setFaceRecognizer(self, faceRecognizer):
        global myFaceRecognizer
        myFaceRecognizer = faceRecognizer
    def setOpenPose(self, openPose, opWrapper):
        global myOpenPose
        myOpenPose = openPose
        global myOpWrapper
        myOpWrapper = opWrapper
    def setObjectDetector(self, objectDetector):
        global myObjectDetector
        myObjectDetector = objectDetector

if __name__ == "__main__":
    HOST, PORT = "0.0.0.0", 8011

    server = ThreadedTCPServer((HOST, PORT), ThreadedTCPRequestHandler)
    ip, port = server.server_address
    print(ip, port)

    # Start a thread with the server -- that thread will then start one
    # more thread for each request
    server_thread = threading.Thread(target=server.serve_forever)
    # Exit the server thread when the main thread terminates
    server_thread.daemon = True
    server_thread.start()
    print("Server loop running in thread:", server_thread.name)

    while True:
        time.sleep(100)
