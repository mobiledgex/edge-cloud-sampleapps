import socket
import threading
import socketserver
import time
from facedetector import FaceDetector
import io
import cv2
import numpy as np
import json
import base64
from imageio import imread, imwrite
import imghdr

fd = FaceDetector()

class ThreadedUDPRequestHandler(socketserver.BaseRequestHandler):
    """
    This class works similar to the TCP handler class, except that
    self.request consists of a pair of data and client socket, and since
    there is no connection the client address must be given explicitly
    when sending data back via sendto().
    """
    def handle(self):
        # print(self.request)
        data = self.request[0].strip()
        print("len(data)", len(data), threading.current_thread())
        socket = self.request[1]

        now = time.time()
        """
        decoded_json = json.loads(data.decode("utf-8"))
        # print("decoded_json", decoded_json)
        image = base64.b64decode(str(decoded_json.get("image")))
        """
        image = data
        image = imread(io.BytesIO(image))
        rects = fd.detect_faces(image)
        elapsed = "%.3f" %((time.time() - now)*1000)
        if len(rects) == 0:
            ret = {"success": "false", "server_processing_time": elapsed}
        else:
            ret = {"success": "true", "server_processing_time": elapsed, "rects": rects.tolist()}

        print("{} wrote: {}".format(self.client_address[0], json.dumps(ret)))
        # print(data)
        # socket.sendto(data.upper(), self.client_address)
        socket.sendto(bytes(json.dumps(ret), "utf-8"), self.client_address)

class ThreadedUDPServer(socketserver.ThreadingMixIn, socketserver.UDPServer):
    max_packet_size = 1024*32
    pass

if __name__ == "__main__":
    HOST, PORT = "0.0.0.0", 8010

    server = ThreadedUDPServer((HOST, PORT), ThreadedUDPRequestHandler)
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
