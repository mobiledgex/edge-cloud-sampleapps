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
    BASE_URL = 'http://%s:8008'
    # API_ENDPOINT = '/detect3/'
    API_ENDPOINT = '/detector/detect/'

    def __init__(self):
        """ """
        return

    def send_image(self, host, image):
        """ """
        return requests.post(self.BASE_URL %host + self.API_ENDPOINT, data=image)

    def send_image_param(self, url, image):
        """
        Sends the encoded image as the "image" POST paramater.
        """
        return requests.post(url, data={'image':image})

    def face_detect(self, host, image_file_name):
        API_ENDPOINT = '/detector/detect/'
        try:
            image = ""
            with open(image_file_name, "rb") as f:
                image = base64.b64encode(f.read())
        except e:
            print(e)
            exit()

        now = time.time()
        content = self.send_image_param(self.BASE_URL %host + API_ENDPOINT, image).content
        elapsed = "%.3f" %((time.time() - now)*1000)
        print("%s ms - %s" %(elapsed, content))

    def face_recognize(self, host, image_file_name):
        API_ENDPOINT = '/recognizer/predict/'
        try:
            image = ""
            with open(image_file_name, "rb") as f:
                image = base64.b64encode(f.read())
        except e:
            print(e)
            exit()

        now = time.time()
        content = self.send_image_param(self.BASE_URL %host + API_ENDPOINT, image).content
        elapsed = "%.3f" %((time.time() - now)*1000)
        print("%s ms - %s" %(elapsed, content))

    def run_multi(self, host, image_file_name, thread_name):
        print("run_multi(%s, %s)\n" %(host, image_file_name))
        #for x in xrange(50):
        for x in xrange(5):
            print("x=%d" %x)
            self.face_recognize(host, image_file_name)
            # self.face_detect(host, image_file_name)
        print("%s Done" %thread_name)

if __name__ == "__main__":
    import sys
    import thread

    if len(sys.argv) < 4:
        print("Must supply a thread count, a filename, and IP address or hostname")
        print("Example:\n%s 4 104.42.217.135 face.png" %__file__)
        exit()

    num_threads = int(sys.argv[1])
    file_name = sys.argv[2]
    host = sys.argv[3]

    # rc = RequestClient()
    # rc.face_detect(host, file_name)
    # sys.exit()

    for i in xrange(num_threads):
        print("i=%d" %i)
        rc = RequestClient()
        thread.start_new_thread(rc.run_multi, (file_name, host, "thread-%d" %i) )

    while True:
        pass
