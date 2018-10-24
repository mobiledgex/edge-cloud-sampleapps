"""
Simple Http client that sends an image to the facial detection
server and displays the result.
"""
import json
import requests
import cv2
import base64
import time

class RequestClient(object):
    """ """
    BASE_URL = 'http://%s:8000'
    API_ENDPOINT = '/detect3/'

    def __init__(self):
        """ """
        return

    def send_image(self, host, image):
        """ """
        return requests.post(self.BASE_URL %host + self.API_ENDPOINT, data=image)

if __name__ == "__main__":
    import sys

    if len(sys.argv) < 3:
        print("Must supply a filename, and IP address or hostname")
        print("Example:\n%s 104.42.217.135 face.png" %__file__)
        exit()

    try:
        image = ""
        with open(sys.argv[2], "rb") as f:
            image = base64.b64encode(f.read())
    except e:
        print(e)
        exit()

    now = time.time()
    rc = RequestClient()
    content = rc.send_image(sys.argv[1], image).content
    elapsed = "%.3f" %((time.time() - now)*1000)
    print("%s ms - %s" %(elapsed, content))
