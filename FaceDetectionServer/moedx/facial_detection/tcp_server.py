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

# Depending on whether this file is loaded directly, or imported from another
# file, the following import is handled differently
try:
    from .facedetector import FaceDetector
except:
    from facedetector import FaceDetector

try:
    from .faceRecognizer import FaceRecognizer
except:
    from faceRecognizer import FaceRecognizer

logger = logging.getLogger(__name__)

myFaceDetector = FaceDetector()

myFaceRecognizer = FaceRecognizer()
logger.info("Created myFaceRecognizer")

class ThreadedTCPRequestHandler(socketserver.BaseRequestHandler):

    def handle(self):
        logger.info("handle() for %s" %threading.current_thread())
        while True:
            logger.info("while True for %s" %threading.current_thread())
            opcode, data = self.recv_one_message(self.request)
            if opcode == None:
                logger.info("Connection closed for %s" %threading.current_thread())
                break;

            if opcode == 'ping_rtt':
                logger.info("Opcode: %s" %opcode)
                ret = {"success": "pong"}
            elif opcode == 'face_det':
                logger.info("Opcode: %s" %opcode)
                logger.info("len(data)=%d %s" %(len(data), threading.current_thread()))
                now = time.time()
                image = imread(io.BytesIO(data))
                rects = myFaceDetector.detect_faces(image)
                elapsed = "%.3f" %((time.time() - now)*1000)
                if len(rects) == 0:
                    ret = {"success": "false", "server_processing_time": elapsed}
                else:
                    ret = {"success": "true", "server_processing_time": elapsed, "rects": rects.tolist()}
            elif opcode == 'face_rec':
                logger.info("Opcode: %s" %opcode)
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
                    if confidence >= 105:
                        subject = "Unknown"
                    ret = {"success": "true", "subject": subject, "confidence":"%.3f" %confidence, "server_processing_time": elapsed, "rect": rect2}
                logger.info("Returning: %s" %(ret))

            elif opcode == 'pose_det':
                logger.info("Opcode: %s" %opcode)
            else:
                logger.info("Unsupported opcode: %s" %opcode)
                continue

            response = bytes(json.dumps(ret), "utf-8")
            logger.info("%s wrote: %s" %(self.client_address[0], response))
            self.request.sendall(response)

    def recv_one_message(self, sock):
        opcodebuf = self.recvall(sock, 8)
        logger.info("recv_one_message opcodebuf=%s" %opcodebuf)
        if opcodebuf == None:
            return None, None
        opcode, = struct.unpack('!8s', opcodebuf)
        opcode = str(opcode, "utf-8")
        logger.info("recv_one_message opcode=%s" %opcode)
        if opcode == 'ping_rtt':
            return opcode, None
        lengthbuf = self.recvall(sock, 4)
        logger.info("recv_one_message lengthbuf=%s" %lengthbuf)
        if lengthbuf == None:
            return None, None
        length, = struct.unpack('!I', lengthbuf)
        logger.info("recv_one_message length=%d" %length)
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
