"""
Simple Http client that sends an image to the facial detection
server and displays the result.
"""
import json
import requests
import base64
import time

total_latency = 0

class RequestClient(object):
    """ """
    BASE_URL = 'http://%s:8008'

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

    def encode_and_send_image(self, host, endpoint, image_file_name, show_responses):
        global total_latency
        try:
            image = ""
            with open(image_file_name, "rb") as f:
                image = base64.b64encode(f.read())
        except e:
            print(e)
            exit()

        now = time.time()
        content = self.send_image_param(self.BASE_URL %host + endpoint, image).content
        millis = (time.time() - now)*1000
        elapsed = "%.3f" %millis
        total_latency = total_latency + millis
        if show_responses:
            print("%s ms - %s" %(elapsed, content))
        else:
            print("%s ms" %(elapsed))

    def run_multi(self, num_repeat, host, endpoint, image_file_name, show_responses, thread_name):
        # print("run_multi(%s, %s)\n" %(host, image_file_name))
        for x in xrange(num_repeat):
            self.encode_and_send_image(host, endpoint, image_file_name, show_responses)
        print("%s Done" %thread_name)

if __name__ == "__main__":
    import sys
    from threading import Thread
    import argparse
    parser = argparse.ArgumentParser()

    parser.add_argument("-s", "--server", required=True, help="Server host name or IP address.")
    parser.add_argument("-f", "--filename", required=True, help="Name of image file to send.")
    parser.add_argument("-e", "--endpoint", required=True, help="Endpoint of web service to call (e.g. /detector/detect/, /recognizer/predict/, /openpose/detect/)")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Number of times to repeat.")
    parser.add_argument("-t", "--threads", type=int, default=1, help="Number of concurent execution threads.")
    parser.add_argument("--show-responses", action='store_true', help="Show responses.")
    args = parser.parse_args()

    for i in xrange(args.threads):
        rc = RequestClient()
        thread = Thread(target=rc.run_multi, args=(args.repeat, args.server, args.endpoint, args.filename, args.show_responses, "thread-%d" %i))
        thread.start()
        thread.join()

    average_latency = total_latency / (args.repeat*args.threads)
    print("Average Latency=%.3f ms" %average_latency)
