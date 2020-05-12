import threading
import os
import time
import logging
import redis
from facial_detection.tcp_server import ThreadedTCPServer, ThreadedTCPRequestHandler
from django.core.management.base import BaseCommand

logger = logging.getLogger(__name__)

PERSISTENT_TCP_PORT_DEFAULT = 8011 # Can be overridden with envvar FD_PERSISTENT_TCP_PORT

class Command(BaseCommand):
    def handle(self, *args, **options):

        # Check for environment variables to override settings.
        try:
            PORT = int(os.environ['FD_PERSISTENT_TCP_PORT'])
        except (ValueError, KeyError) as e:
            PORT = PERSISTENT_TCP_PORT_DEFAULT

        # Start the non-REST session-based TCP server.
        HOST = "0.0.0.0"

        try:
            server = ThreadedTCPServer((HOST, PORT), ThreadedTCPRequestHandler)
            # server.setFaceDetector(myFaceDetector)
            # server.setFaceRecognizer(myFaceRecognizer)
            # server.setOpenPose(myOpenPose, myOpWrapper)
            # server.setObjectDetector(myObjectDetector)
            ip, port = server.server_address
            logger.info("ip=%s port=%d" %(ip, port))

            # Start a thread with the server -- that thread will then start one
            # more thread for each request
            server_thread = threading.Thread(target=server.serve_forever)
            # Exit the server thread when the main thread terminates
            server_thread.daemon = True
            server_thread.start()
            logger.info("Persistent TCP Server loop running in thread: %s" %server_thread.name)
        except OSError:
            logger.warn("Persistent TCP Server already running on port %d" %PORT)

        while True:
            time.sleep(100)
