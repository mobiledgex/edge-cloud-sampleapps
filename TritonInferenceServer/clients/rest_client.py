# Copyright 2021 MobiledgeX, Inc. All rights and licenses reserved.
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
REST client script to send images to the Triton Inference Server and measure latency.
Has no tritonclient or nvidia dependencies.
"""
import sys
import os
import os.path
import platform
import requests
import subprocess
import re
import socket
import ssl
import json
import time
import logging
import io
import cv2
import argparse
from threading import Thread
import numpy as np

util_dir = "../utilities"
sys.path.append(os.path.join(os.path.dirname(__file__), util_dir))
from stats import RunningStats

PING_INTERVAL = 1 # Seconds
SERVER_STATS_INTERVAL = 1.5 # Seconds
SERVER_STATS_DELAY = 2 # Seconds

hex_colors = ["#238bc0", "#ff9209", "#32ab39", "#e03d34", "#a57ec8", "#9e6a5d", "#ea90cc", "#919191",
            "#c8c62b", "#00c8d8", "#bbd1ec", "#ffc689", "#a6e19b", "#ffaaa6", "#cfbfdd", "#cfaca5",
            "#fac4da", "#d1d1d1", "#e1e09e", "#ace0e9"]
colors=[]
for h in hex_colors:
    h = h.lstrip('#')
    colors.append(tuple(int(h[i:i+2], 16) for i in (0, 2, 4)))

colors = colors * 3 # Extend array in case we get more than 20 objects detected

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(threadName)s - %(levelname)s - %(message)s')
fh = logging.FileHandler('rest_client.log')
fh.setLevel(logging.DEBUG)
fh.setFormatter(formatter)
logger.addHandler(fh)
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
ch.setFormatter(formatter)
logger.addHandler(ch)

if platform.system() == "Darwin":
    PING_EXEC = "/sbin/ping"
    PING_REGEX = r'round-trip min/avg/max/stddev = (.*)/(.*)/(.*)/(.*) ms'
else:
    PING_EXEC = "/bin/ping"
    PING_REGEX = r'rtt min/avg/max/mdev = (.*)/(.*)/(.*)/(.*) ms'

class Client:
    """ Base Client class """

    MULTI_THREADED = False
    # Initialize "Grand total" class variables.
    stats_latency_full_process = RunningStats()
    stats_latency_network_only = RunningStats()
    num_success = 0

    def __init__(self, host, port):
        # Initialize instance variables.
        self.host = host
        self.port = port
        self.model_name = None
        self.net_latency_method = None
        self.tls = False
        self.continue_on_error = False
        self.running = False
        self.num_success = 0
        self.do_server_stats = False
        self.show_responses = False
        self.stats_latency_full_process = RunningStats()
        self.stats_latency_network_only = RunningStats()
        self.media_file_name = None
        self.out_dir = None
        self.latency_start_time = 0
        self.loop_count = 0
        self.num_repeat = 0
        self.num_vid_repeat = 0
        self.filename_list = []
        self.filename_list_index = 0
        self.video = None
        self.video_frame_num = 0
        self.resize = True
        self.resize_long = 240
        self.resize_short = 180
        self.skip_frames = 1
        logger.debug("host:port = %s:%d" %(self.host, self.port))

    def start(self):
        self.running = True
        logger.debug("media file(s) %s" %(self.filename_list))
        video_extensions = ('mp4', 'avi', 'mov')
        if self.filename_list[0].endswith(video_extensions):
            logger.debug("It's a video")
            self.media_file_name = self.filename_list[0]
            self.video = cv2.VideoCapture(self.media_file_name)

    def get_next_image(self):
        if self.video is not None:
            for x in range(self.skip_frames):
                ret, image = self.video.read()
                self.media_file_name = f"video-frame-{self.video_frame_num:04}.jpg"
                self.video_frame_num += 1
                if not ret:
                    logger.debug("End of video")
                    return None
        else:
            # If the filename_list array has more than 1, get the next value.
            if len(self.filename_list) > 1:
                self.filename_list_index += 1
                if self.filename_list_index >= len(self.filename_list):
                    self.filename_list_index = 0
            else:
                self.filename_list_index = 0

            if self.stats_latency_full_process.n >= self.num_repeat:
                return None

            self.media_file_name = self.filename_list[self.filename_list_index]
            image = cv2.imread(self.media_file_name)

        if self.resize:
            image = self.resize_image(image)

        # Whether it's from a video frame or image file, at this point the image
        # data is a numpy array. Here we convert it to a raw byte stream.
        res, image = cv2.imencode('.JPEG', image)
        image = image.tobytes()

        logger.debug("Image data (first 32 bytes logged): %s" %image[:32])
        return image

    def resize_image(self, image):
        w = image.shape[1]
        h = image.shape[0]
        logger.debug("Frame size: %dx%d" %(w, h))
        if w > h:
            resize_w = self.resize_long
            resize_h = self.resize_short
        else:
            resize_w = self.resize_short
            resize_h = self.resize_long
        image = cv2.resize(image, (resize_w, resize_h))

        logger.debug("Resized image to: %dx%d" %(resize_w, resize_h))
        return image

    def measure_network_latency(self):
        while self.running:
            if self.net_latency_method == "socket":
                self.time_open_socket()
            elif self.net_latency_method == "ping":
                self.icmp_ping()
            time.sleep(PING_INTERVAL)

    def measure_server_stats(self):
        # TODO: Use /v2/models/yolov4/versions/1/stats
        print("TODO")

    def time_open_socket(self):
        now = time.time()
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(2)
        result = sock.connect_ex((self.host, self.port))
        if result != 0:
            logger.error("Could not connect to %s on port %d" %(self.host, self.port))
            return
        millis = (time.time() - now)*1000
        elapsed = "%.3f" %millis
        if self.show_responses:
            logger.info("%s ms to open socket" %(elapsed))
        self.stats_latency_network_only.push(millis)
        Client.stats_latency_network_only.push(millis)


    def icmp_ping(self):
        args=[PING_EXEC, '-c', '1', '-W', '1', self.host]
        p_ping = subprocess.Popen(args,
                                  shell=False,
                                  stdout=subprocess.PIPE)
        # save ping stdout
        p_ping_out = str(p_ping.communicate()[0])

        if (p_ping.wait() == 0):
            # logger.info(p_ping_out)
            # rtt min/avg/max/mdev = 61.994/61.994/61.994/0.000 ms
            search = re.search(PING_REGEX, p_ping_out, re.M|re.I)
            ping_rtt = float(search.group(2))
            if self.show_responses:
                logger.info("%s ms ICMP ping" %(ping_rtt))
            self.stats_latency_network_only.push(ping_rtt)
            Client.stats_latency_network_only.push(ping_rtt)
        else:
            logger.error("ICMP ping failed")

    def process_result(self, result, image, inference_header_content_length):
        millis = (time.time() - self.latency_start_time)*1000
        self.stats_latency_full_process.push(millis)
        Client.stats_latency_full_process.push(millis)

        if self.model_name == "ensemble_dali_inception":
            data = result[inference_header_content_length:]
            cls = data.split(':')
            confidence = float(cls[0])
            class_name = cls[2]
            output = f"{class_name} - Confidence={confidence:0.2}"

        elif self.model_name == "ensemble_dali_yolov4":
            try:
                # This is the overall response, in JSON format. We have
                # to drill down a bit to get the data we're interested in,
                # which is stored as a string object, but is also JSON-encoded.
                decoded_json = json.loads(result)
                decoded_json = decoded_json['outputs'][0]['data'][0]
                output = decoded_json
                # Parse the actual object detection inference results
                decoded_json = json.loads(output)
            except Exception as e:
                logger.error("Could not decode result. Exception: %s. Result: %s" %(e, result))
                return
            if 'success' in decoded_json:
                if decoded_json['success'] == True:
                    self.num_success += 1
                    Client.num_success += 1

            if self.out_dir is not None:
                nparr = np.frombuffer(image, np.uint8)
                img_np = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                # img_np = cv2.cvtColor(img_np, cv2.COLOR_BGR2RGB)
                image_w = img_np.shape[1]
                image_h = img_np.shape[0]
                ratio_x = image_w / 608
                ratio_y = image_h / 608
                obj_num = 0
                for json_object in decoded_json['objects']:
                    label = json_object['class']
                    score = float(json_object['confidence'])
                    percent = f"{score*100:0.1f}"
                    rect = json_object['rect']
                    x1 = int(rect[0] * ratio_x)
                    y1 = int(rect[1] * ratio_y)
                    x2 = int(rect[2] * ratio_x)
                    y2 = int(rect[3] * ratio_y)
                    text = f"{label} ({percent}%)"
                    retval, _ = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, 0.67, 2)
                    cv2.rectangle(img_np, (x1, y1), (x2, y2), colors[obj_num], 2)
                    cv2.putText(img_np, text, (x1 + 2, y1 + retval[1] + 2), cv2.FONT_HERSHEY_SIMPLEX, 0.67, colors[obj_num], 2)
                    obj_num += 1

                cv2.imwrite(self.out_dir + "/" + os.path.basename(self.media_file_name), img_np)  

        if self.show_responses:
            elapsed = "%.3f" %millis
            logger.info("%s ms to send and receive: %s" %(elapsed, output))

    def display_results(self):
        self.running = False
        if not self.show_responses or not Client.MULTI_THREADED:
            return

        if self.stats_latency_full_process.n > 0:
            logger.info("====> Average Latency Full Process=%.3f ms (stddev=%.3f)" %(self.stats_latency_full_process.mean(), self.stats_latency_full_process.stddev()))
        if self.stats_latency_network_only.n > 0:
            logger.info("====> Average Latency Network Only=%.3f ms (stddev=%.3f)" %(self.stats_latency_network_only.mean(), self.stats_latency_network_only.stddev()))

class RestClient(Client):
    def __init__(self, host, port=8000):
        if port is None:
            port = 8000
        Client.__init__(self, host, port)

    def start(self):
        Client.start(self)
        self.url = f"http://{self.host}:{self.port}/v2/models/{self.model_name}/infer"
        if self.tls:
            self.url = self.url.replace("http", "https", 1)

        while True:
            image = self.get_next_image()
            if image is None:
                break

            self.latency_start_time = time.time()
            response = self.send_image(image)
            logger.debug(f"{response.headers =}")
            if 'Inference-Header-Content-Length' in response.headers:
                inference_header_content_length = int(response.headers['Inference-Header-Content-Length'])
            else:
                inference_header_content_length = 0
            content = str(response.content, 'utf-8')
            if response.status_code != 200:
                logger.error("non-200 response: %d: %s" %(response.status_code, content))
                self.num_repeat -= 1
                if self.continue_on_error:
                    continue
                else:
                    break
            self.process_result(content, image, inference_header_content_length+4)

        logger.debug("Done")
        self.display_results()

    def send_image(self, image):
        """
        Sends the raw image data with a 'Content-Type' of 'image/jpeg'.
        """
        if self.model_name == "ensemble_dali_yolov4":
            body_template = '{"inputs":[{"name":"IMAGE","shape":[1,$size],"datatype":"UINT8","parameters":{"binary_data_size":$size}}],"outputs":[{"name":"OBJECTS_JSON"}]}'
        elif self.model_name == "ensemble_dali_inception":
            body_template = '{"inputs":[{"name":"INPUT","shape":[1,$size],"datatype":"UINT8","parameters":{"binary_data_size":$size}}],"outputs":[{"name":"OUTPUT","parameters":{"classification":1,"binary_data":true}}]}'
        else:
            logger.error(f"Unknown model name: {model_name}")
            sys.exit(1)

        data = body_template.replace("$size", str(len(image)))
        headers = {'Inference-Header-Content-Length': str(len(data))}
        logger.debug(f"POST {self.url}, headers {headers}")
        data = bytes(data, 'utf-8') + image
        return requests.post(self.url, data=data, headers=headers, verify=self.tls_verify)

class ErrorCatchingArgumentParser(argparse.ArgumentParser):
    def exit(self, status=0, message=None):
        if status:
            raise Exception(f'ArgumentParser error: {message}')

def benchmark(arguments=None, django=False):
    # This handler will save everything logged to a String which
    # can be accessed with log_stream.getvalue()
    log_stream = io.StringIO()
    sh = logging.StreamHandler(log_stream)
    formatter = logging.Formatter('%(asctime)s - %(process)d - %(message)s')
    sh.setLevel(logging.INFO)
    sh.setFormatter(formatter)
    logger.addHandler(sh)
    if django:
        logger.removeHandler(fh)
        logger.removeHandler(ch)
        parser = ErrorCatchingArgumentParser()
    else:
        parser = argparse.ArgumentParser()

    parser.add_argument('-s', '--server', required=True, help='Server host name or IP address.')
    parser.add_argument('-m', '--model_name', required=True, choices=['ensemble_dali_yolov4', 'ensemble_dali_inception'], help='Model name to use for inference')
    parser.add_argument('-n', '--network-latency', required=False, choices=['ping', 'socket', 'NONE'], default='socket', help='Network-only latency test method.')
    parser.add_argument('-c', '--connection-method', required=False, choices=['rest'], default='rest', help='Connection type.')
    parser.add_argument('-f', '--filename', required=False, help='Name of image file to send.')
    parser.add_argument('-d', '--directory', required=False, help='Directory containing image files to send (*.jpg, *.png).')
    parser.add_argument('-r', '--repeat', type=int, default=1, help='Number of times to repeat.')
    parser.add_argument('-t', '--threads', type=int, default=1, help='Number of concurrent execution threads.')
    parser.add_argument('--skip-frames', type=int, default=1, help='For video, send every Nth frame.')
    parser.add_argument('-p', '--port', type=int, help='Port number')
    parser.add_argument('--fullsize', action='store_true', help='Maintain original image size. Default is to shrink the image before sending.')
    parser.add_argument('--tls', action='store_true', help='Use https connection')
    parser.add_argument('--noverify', action='store_true', help='Disable TLS cert verification')
    parser.add_argument('--continue-on-error', action='store_true', default=False, help='Continue processing when error occurs')
    parser.add_argument('--show-responses', action='store_true', help='Show responses.')
    parser.add_argument('--server-stats', action='store_true', help='Get server stats every Nth frame.')
    parser.add_argument('--out-dir', type=str, required=False, default=None, help='Directory to write processed image to')
    args = parser.parse_args(arguments)

    # Clear the Class variables. Otherwise, in the case we are instantiated by
    # a Django view, the accumulation of stats would continue session to session.
    Client.stats_latency_full_process.clear()
    Client.stats_latency_network_only.clear()

    start_time = time.time()

    if args.threads > 1:
        Client.MULTI_THREADED = True
    for x in range(args.threads):
        if args.connection_method == "rest":
            client = RestClient(args.server, args.port)
        else:
            # This should be impossible because the ArgumentParser enforces a valid choice.
            logger.error("Unknown connection-method: %s" %args.connection_method)
            return False

        if args.filename != None and args.directory != None:
            logger.error("Can't include both filename and directory arguments")
            parser.print_usage()
            return False

        if args.filename != None:
            if not os.path.isfile(args.filename):
                return [False, "%s doesn't exist" %args.filename]
            client.filename_list.append(args.filename)

        elif args.directory != None:
            if not os.path.isdir(args.directory):
                return [False, "%s doesn't exist" %args.directory]
            valid_extensions = ('jpg','jpeg', 'png')
            files = os.listdir(args.directory)
            for file in files:
                if file.endswith(valid_extensions):
                    client.filename_list.append(args.directory+"/"+file)
            if len(client.filename_list) == 0:
                return [False, "%s contains no valid image files" %args.directory]

        else:
            logger.error("Must include either filename or directory argument")
            parser.print_usage()
            return False

        client.filename_list_index = -1
        client.num_repeat = args.repeat * len(client.filename_list)
        client.do_server_stats = args.server_stats
        client.show_responses = args.show_responses
        client.model_name = args.model_name
        client.net_latency_method = args.network_latency
        client.resize = not args.fullsize
        client.skip_frames = args.skip_frames
        client.tls = args.tls
        client.tls_verify = not args.noverify
        client.continue_on_error = args.continue_on_error
        client.out_dir = args.out_dir

        thread = Thread(target=client.start)
        thread.start()
        logger.debug("Started %s" %thread)
        time.sleep(0.5) # stagger threads

    if args.network_latency != "NONE":
        thread = Thread(target=client.measure_network_latency)
        thread.start()
        logger.debug("Started background measure_network_latency %s" %thread)

    if args.server_stats:
        thread = Thread(target=client.measure_server_stats)
        thread.start()
        logger.debug("Started background measure_server_stats %s" %thread)

    thread.join()

    session_time = time.time() - start_time

    if Client.stats_latency_full_process.n + Client.stats_latency_network_only.n > 0:
        fps = Client.stats_latency_full_process.n / session_time
        header1 = f"Grand totals for {args.server} {args.model_name} {args.connection_method}"
        header2 = f"{args.threads} threads repeated {args.repeat} times on {len(client.filename_list)} files. {Client.stats_latency_full_process.n} total frames. FPS={fps:.2f}"
        separator = ""
        for s in header1: separator += "="
        logger.info(separator)
        logger.info(header1)
        logger.info(header2)
        logger.info(separator)
        if Client.stats_latency_full_process.n > 0:
            fps = 1/Client.stats_latency_full_process.mean()*1000
            logger.info("====> Average Latency Full Process=%.3f ms (stddev=%.3f) FPS=%.2f" %(Client.stats_latency_full_process.mean(), Client.stats_latency_full_process.stddev(), fps))
        if Client.stats_latency_network_only.n > 0:
            logger.info("====> Average Latency Network Only=%.3f ms (stddev=%.3f)" %(Client.stats_latency_network_only.mean(), Client.stats_latency_network_only.stddev()))

        # The following line outputs CSV data that can be imported to a spreadsheet.
        logger.info("")
        logger.info("Server, Full Process, Network Only")
        logger.info(f"{args.server}, {Client.stats_latency_full_process.mean()}, {Client.stats_latency_network_only.mean()}")

        TEST_PASS = (Client.stats_latency_full_process.n == Client.num_success)
        logger.info("TEST_PASS=%r" %TEST_PASS)

    else:
        logger.info("No results")

    return [True, log_stream.getvalue()]

if __name__ == "__main__":
    ret = benchmark()
    if not ret[0]:
        print(ret)
