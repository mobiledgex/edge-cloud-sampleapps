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
Client script that can use multiple communication methods to send images to the
computervision backend and measure latency.
"""
import sys
import os
import os.path
import platform
import requests
import subprocess
import re
import base64
import socket
import websocket
import ssl
import struct
import json
import time
import logging
from io import StringIO
import cv2
import argparse
from threading import Thread
try:
    from stats import RunningStats
except Exception as e:
    from .stats import RunningStats

WEBSOCKET_OPCODE_BINARY = 0x2
PING_INTERVAL = 1 # Seconds
SERVER_STATS_INTERVAL = 1.5 # Seconds
SERVER_STATS_DELAY = 2 # Seconds
TEST_PASS = False

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(threadName)s - %(levelname)s - %(message)s')
fh = logging.FileHandler('multi_client.log')
fh.setLevel(logging.DEBUG)
fh.setFormatter(formatter)
logger.addHandler(fh)
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
ch.setFormatter(formatter)
logger.addHandler(ch)

if platform.system() == "Darwin":
    PING = "/sbin/ping"
    PING_REGEX = r'round-trip min/avg/max/stddev = (.*)/(.*)/(.*)/(.*) ms'
else:
    PING = "/bin/ping"
    PING_REGEX = r'rtt min/avg/max/mdev = (.*)/(.*)/(.*)/(.*) ms'

class Client:
    """ Base Client class """

    MULTI_THREADED = False
    # Initialize "Grand total" class variables.
    stats_latency_full_process = RunningStats()
    stats_latency_network_only = RunningStats()
    stats_server_processing_time = RunningStats()
    stats_cpu_utilization = RunningStats()
    stats_mem_utilization = RunningStats()
    stats_gpu_utilization = RunningStats()
    stats_gpu_mem_utilization = RunningStats()

    def __init__(self, host, port):
        # Initialize instance variables.
        self.host = host
        self.port = port
        self.running = False
        self.do_server_stats = False
        self.show_responses = False
        self.stats_latency_full_process = RunningStats()
        self.stats_latency_network_only = RunningStats()
        self.stats_server_processing_time = RunningStats()
        self.stats_cpu_utilization = RunningStats()
        self.stats_mem_utilization = RunningStats()
        self.stats_gpu_utilization = RunningStats()
        self.stats_gpu_mem_utilization = RunningStats()
        self.media_file_name = None
        self.latency_start_time = 0
        self.loop_count = 0
        self.num_repeat = 0
        self.num_vid_repeat = 0
        self.filename_list = []
        self.filename_list_index = 0
        self.json_params = None
        self.base64 = False
        self.video = None
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
                if not ret:
                    logger.debug("End of video")

                    return None
            vw = image.shape[1]
            vh = image.shape[0]
            logger.debug("Video size: %dx%d" %(vw, vh))
            if self.resize:
                if vw > vh:
                    resize_w = self.resize_long
                    resize_h = self.resize_short
                else:
                    resize_w = self.resize_short
                    resize_h = self.resize_long
                # if vw > vh:
                #     resize_w = int(self.resize_short / (vh/vw))
                #     resize_h = self.resize_short
                # else:
                #     resize_w = int(self.resize_long / (vw/vh))
                #     resize_h = self.resize_long

                image = cv2.resize(image, (resize_w, resize_h))
                logger.debug("Resized image to: %dx%d" %(resize_w, resize_h))
            res, image = cv2.imencode('.JPEG', image)
            image = image.tobytes()
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
            f = open(self.media_file_name, "rb")
            image = f.read()

        logger.debug("Image data (first 32 bytes logged): %s" %image[:32])
        return image

    def measure_network_latency(self):
        while self.running:
            if self.net_latency_method == "SOCKET":
                self.time_open_socket()
            elif self.net_latency_method == "PING":
                self.icmp_ping()
            time.sleep(PING_INTERVAL)

    def measure_server_stats(self):
        url = "http://%s:%d%s" %(self.host, self.port, "/server/usage/")
        if self.tls:
            url = url.replace("http", "https", 1)
        time.sleep(SERVER_STATS_DELAY)
        while self.running:
            decoded_json = json.loads(requests.get(url).content)
            if 'cpu_utilization' in decoded_json:
                self.stats_cpu_utilization.push(float(decoded_json['cpu_utilization']))
                Client.stats_cpu_utilization.push(float(decoded_json['cpu_utilization']))
            if 'mem_utilization' in decoded_json:
                self.stats_mem_utilization.push(float(decoded_json['mem_utilization']))
                Client.stats_mem_utilization.push(float(decoded_json['mem_utilization']))
            if 'gpu_utilization' in decoded_json:
                self.stats_gpu_utilization.push(float(decoded_json['gpu_utilization']))
                Client.stats_gpu_utilization.push(float(decoded_json['gpu_utilization']))
            if 'gpu_mem_utilization' in decoded_json:
                self.stats_gpu_mem_utilization.push(float(decoded_json['gpu_mem_utilization']))
                Client.stats_gpu_mem_utilization.push(float(decoded_json['gpu_mem_utilization']))
            if self.show_responses or True:
                logger.info(requests.get(url).content)
            time.sleep(SERVER_STATS_INTERVAL)

    def time_open_socket(self):
        now = time.time()
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
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
        args=[PING, '-c', '1', '-W', '1', self.host]
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

    def process_result(self, result):
        global TEST_PASS
        try:
            decoded_json = json.loads(result)
        except Exception as e:
            logger.error("Could not decode result. Exception: %s. Result: %s" %(e, result))
            TEST_PASS = False
            return
        if 'success' in decoded_json:
            if decoded_json['success'] == "true":
                TEST_PASS = True
            else:
                TEST_PASS = False
        if 'latency_start' in decoded_json:
            millis = (time.time() - decoded_json['latency_start'])*1000
            self.stats_latency_network_only.push(millis)
            Client.stats_latency_network_only.push(millis)
        else:
            millis = (time.time() - self.latency_start_time)*1000
            self.stats_latency_full_process.push(millis)
            Client.stats_latency_full_process.push(millis)
            if 'server_processing_time' in decoded_json:
                server_processing_time = decoded_json['server_processing_time']
                self.stats_server_processing_time.push(float(server_processing_time))
                Client.stats_server_processing_time.push(float(server_processing_time))

        if self.show_responses:
            elapsed = "%.3f" %millis
            logger.info("%s ms to send and receive: %s" %(elapsed, result))

    def display_results(self):
        self.running = False
        if not self.show_responses or not Client.MULTI_THREADED:
            return

        if self.stats_latency_full_process.n > 0:
            logger.info("====> Average Latency Full Process=%.3f ms (stddev=%.3f)" %(self.stats_latency_full_process.mean(), self.stats_latency_full_process.stddev()))
        if self.stats_latency_network_only.n > 0:
            logger.info("====> Average Latency Network Only=%.3f ms (stddev=%.3f)" %(self.stats_latency_network_only.mean(), self.stats_latency_network_only.stddev()))
        if self.stats_server_processing_time.n > 0:
            logger.info("====> Average Server Processing Time=%.3f ms (stddev=%.3f)" %(self.stats_server_processing_time.mean(), Client.stats_server_processing_time.stddev()))

class RestClient(Client):
    def __init__(self, host, port=8008):
        if port is None:
            port = 8008
        Client.__init__(self, host, port)

    def start(self):
        Client.start(self)
        self.url = "http://%s:%d%s" %(self.host, self.port, self.endpoint)
        if self.tls:
            self.url = self.url.replace("http", "https", 1)

        while True:
            image = self.get_next_image()
            if image is None:
                break

            self.latency_start_time = time.time()
            if self.base64:
                response = self.send_image_json(image)
            else:
                response = self.send_image(image)
            content = response.content
            if response.status_code != 200:
                logger.error("non-200 response: %d: %s" %(response.status_code, content))
                self.num_repeat -= 1
                continue
            self.process_result(content)

        logger.debug("Done")
        self.display_results()

    def send_image(self, image):
        """
        Sends the raw image data with a 'Content-Type' of 'image/jpeg'.
        """
        headers = {'Content-Type': 'image/jpeg', "Mobiledgex-Debug": "true"} # Enable saving debug images
        # headers = {'Content-Type': 'image/jpeg'}
        return requests.post(self.url, data=image, headers=headers, verify=self.tls_verify)

    def send_image_json(self, image):
        """
        Base64 encodes the image, and sends it as the "image" value
        in the JSON paramater set. Content-Type=application/x-www-form-urlencoded
        """
        data = {'image': base64.b64encode(image)}
        if self.json_params != None:
            params = json.loads(self.json_params)
            data.update(params)

        return requests.post(self.url, data=data)

class PersistentTcpClient(Client):
    def __init__(self, host, port=8011):
        if port is None:
            port = 8011
        Client.__init__(self, host, port)

    def start(self):
        Client.start(self)
        if self.endpoint == "/detector/detect/":
            op_code = 1
        elif self.endpoint == "/recognizer/predict/":
            op_code = 2
        elif self.endpoint == "/openpose/detect/":
            op_code = 3
        elif self.endpoint == "/object/detect/":
            op_code = 4
        else:
            logger.error("Unknown endpoint: %s" %self.endpoint)
            return

        # Open the connection one time, then send the image data multiple times.
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        logger.info("host:port = %s:%d" %(self.host, self.port))
        sock.connect((self.host, self.port))

        logger.debug("repeating %s %d times" %(op_code, self.num_repeat))
        while True:
            data = self.get_next_image()
            if data is None:
                break

            length = len(data)
            logger.debug("data length = %d" %length)
            self.latency_start_time = time.time()
            sock.sendall(struct.pack('!I', op_code))
            sock.sendall(struct.pack('!I', length))
            sock.sendall(data)

            lengthbuf = sock.recv(4)
            length, = struct.unpack('!I', lengthbuf)
            result = str(sock.recv(length), "utf-8")

            self.process_result(result)

        logger.debug("Done")
        self.display_results()

    def recvall(sock, count):
        buf = b''
        while count:
            newbuf = sock.recv(count)
            if not newbuf: return None
            buf += newbuf
            count -= len(newbuf)
        return buf

class WebSocketClient(Client):
    def __init__(self, host, port=8008):
        if port is None:
            port = 8008
        Client.__init__(self, host, port)

    def start(self):
        Client.start(self)
        url = "ws://%s:%s/ws%s" %(self.host, self.port, self.endpoint)
        if self.tls:
            url = url.replace("ws", "wss", 1)
        logger.debug("url: %s" %url)
        ws = websocket.WebSocketApp(url,
                    on_message = lambda ws,msg: self.on_message(ws, msg),
                    on_error   = lambda ws,msg: self.on_error(ws, msg),
                    on_close   = lambda ws:     self.on_close(ws),
                    on_open    = lambda ws:     self.on_open(ws))
        # websocket.enableTrace(True)
        if self.tls_verify:
            ws.run_forever()
        else:
            ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})

    def on_message(self, ws, message):
        # As each response is received, process the response, then send the next image.
        # logger.info("on_message: %s loop_count: %s", %(message,self.loop_count))
        self.process_result(message)

        self.loop_count += 1

        image = self.get_next_image()
        if image is None:
            logger.debug("repeating done")
            self.display_results()
            ws.close()
            return
        logger.debug("loop_count: %d media_file_name: %s filename_list_index: %s num_repeat: %s count_latency_full_process: %s" %(self.loop_count, self.media_file_name, self.filename_list_index, self.num_repeat, self.stats_latency_full_process.n))
        self.latency_start_time = time.time()
        ws.send(image, WEBSOCKET_OPCODE_BINARY)

    def on_error(self, ws, error):
        logger.info("on_error: %s" %error)

    def on_close(self, ws):
        self.running = False
        logger.info("on_close")

    def on_open(self, ws):
        # As soon as the websocket is open, send the first image.
        image = self.get_next_image()
        self.loop_count += 1
        self.latency_start_time = time.time()
        ws.send(image, WEBSOCKET_OPCODE_BINARY)

class ErrorCatchingArgumentParser(argparse.ArgumentParser):
    def exit(self, status=0, message=None):
        if status:
            raise Exception(f'ArgumentParser error: {message}')

def benchmark(arguments=None, django=False):
    # This handler will save everything logged to a String which
    # can be accessed with log_stream.getvalue()
    log_stream = StringIO()
    sh = logging.StreamHandler(log_stream)
    formatter = logging.Formatter('%(asctime)s - %(message)s')
    sh.setLevel(logging.INFO)
    sh.setFormatter(formatter)
    logger.addHandler(sh)
    if django:
        logger.removeHandler(fh)
        logger.removeHandler(ch)
        parser = ErrorCatchingArgumentParser()
    else:
        parser = argparse.ArgumentParser()

    parser.add_argument("-s", "--server", required=True, help="Server host name or IP address.")
    parser.add_argument("-e", "--endpoint", required=True, choices=["/detector/detect/", "/recognizer/predict/", "/openpose/detect/", "/object/detect/", "/trainer/add/", "/trainer/predict/"], help="Endpoint of web service to call.")
    parser.add_argument("-n", "--network-latency", required=False, choices=["PING", "SOCKET", "NONE"], default="SOCKET", help="Network-only latency test method.")
    parser.add_argument("-c", "--connection-method", required=True, choices=["rest", "socket", "websocket"], help="Connection type.")
    parser.add_argument("-f", "--filename", required=False, help="Name of image file to send.")
    parser.add_argument("-d", "--directory", required=False, help="Directory containing image files to send (*.jpg, *.png).")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Number of times to repeat.")
    parser.add_argument("-t", "--threads", type=int, default=1, help="Number of concurrent execution threads.")
    parser.add_argument("--skip-frames", type=int, default=1, help="For video, send every Nth frame.")
    parser.add_argument("-p", "--port", type=int, help="Port number")
    parser.add_argument("-j", "--json-params", required=False, help='Extra parameters to include with image. Ex: {"subject":"Max Door", "owner":"Bruce Armstrong"}')
    parser.add_argument("--fullsize", action='store_true', help="Maintain original image size. Default is to shrink the image before sending.")
    parser.add_argument("--base64", action='store_true', help="Base64 encode image")
    parser.add_argument("--tls", action='store_true', help="Use https connection")
    parser.add_argument("--noverify", action='store_true', help="Disable TLS cert verification")
    parser.add_argument("--show-responses", action='store_true', help="Show responses.")
    parser.add_argument("--server-stats", action='store_true', help="Get server stats every Nth frame.")
    args = parser.parse_args(arguments)

    start_time = time.time()

    if args.threads > 1:
        Client.MULTI_THREADED = True
    for x in range(args.threads):
        if args.connection_method == "rest":
            client = RestClient(args.server, args.port)
        elif args.connection_method == "socket":
            client = PersistentTcpClient(args.server, args.port)
        elif args.connection_method == "websocket":
            client = WebSocketClient(args.server, args.port)
        else:
            # This should be impossible because the ArgumentParser enforces a valid choice.
            logger.error("Unknown connection-method: %s" %args.connection_method)
            return False

        if args.base64 and args.connection_method != "rest":
            logger.warning("base64 parameter ignored for %s" %args.connection_method)

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
        client.endpoint = args.endpoint
        client.json_params = args.json_params
        client.base64 = args.base64
        client.net_latency_method = args.network_latency
        client.resize = not args.fullsize
        client.skip_frames = args.skip_frames
        client.tls = args.tls
        client.tls_verify = not args.noverify

        Client.stats_latency_full_process.clear()
        Client.stats_latency_network_only.clear()
        Client.stats_server_processing_time.clear()
        Client.stats_cpu_utilization.clear()
        Client.stats_mem_utilization.clear()
        Client.stats_gpu_utilization.clear()
        Client.stats_gpu_mem_utilization.clear()

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

    if Client.stats_latency_full_process.n + Client.stats_latency_network_only.n + Client.stats_server_processing_time.n > 0:
        fps = Client.stats_latency_full_process.n / session_time
        header1 = "Grand totals for %s %s %s" %(args.server, args.endpoint, args.connection_method)
        header2 = "%d threads repeated %d times on %d files. %d total frames. FPS=%.2f" %(args.threads, args.repeat, len(client.filename_list), Client.stats_latency_full_process.n, fps)
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
        if Client.stats_server_processing_time.n > 0:
            logger.info("====> Average Server Processing Time=%.3f ms (stddev=%.3f)" %(Client.stats_server_processing_time.mean(), Client.stats_server_processing_time.stddev()))

        if Client.stats_cpu_utilization.n > 0:
            logger.info("====> Average CPU Utilization=%.1f%%" %(Client.stats_cpu_utilization.mean()))
        if Client.stats_mem_utilization.n > 0:
            logger.info("====> Average Memory Utilization=%.1f%%" %(Client.stats_mem_utilization.mean()))
        if Client.stats_gpu_utilization.n > 0:
            logger.info("====> Average GPU Utilization=%.1f%%" %(Client.stats_gpu_utilization.mean()))
        if Client.stats_gpu_mem_utilization.n > 0:
            logger.info("====> Average GPU Memory Utilization=%.1f%%" %(Client.stats_gpu_mem_utilization.mean()))


        # The following line outputs CSV data that can be imported to a spreadsheet.
        logger.info("")
        logger.info("Server, Full Process, Network Only, Server Time, CPU Util, Mem Util, GPU Util, GPU Mem Util")
        logger.info("%s, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f" %(args.server, Client.stats_latency_full_process.mean(),
            Client.stats_latency_network_only.mean(), Client.stats_server_processing_time.mean(), Client.stats_cpu_utilization.mean(),
            Client.stats_mem_utilization.mean(), Client.stats_gpu_utilization.mean(), Client.stats_gpu_mem_utilization.mean()))

        logger.info("TEST_PASS=%r" %TEST_PASS)
    else:
        logger.info("No results")

    return [True, log_stream.getvalue()]

if __name__ == "__main__":
    benchmark()
