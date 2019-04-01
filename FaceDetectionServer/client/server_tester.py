"""
Simple Http client that sends an image to the facial detection
server and displays the result.
"""
import json
import requests
import base64
import time
import socket
import os

# Globals
total_latency_full_process = 0
count_latency_full_process = 0
total_latency_network_only = 0
count_latency_network_only = 0
json_params = None
do_server_stats = False
port_number = 8008

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
        global json_params
        data = {'image':image}
        if json_params != None:
            params = json.loads(json_params)
            data.update(params)

        return requests.post(url, data=data)

    def get_server_stats(self, host):
        url = self.BASE_URL %host + "/server/usage"
        print(requests.get(url).content)

    def time_open_socket(self, host, port):
        global total_latency_network_only
        global count_latency_network_only
        now = time.time()
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
        sock.settimeout(2)
        result = sock.connect_ex((host,port))
        if result != 0:
            print("Could not connect to %s on port %d" %(host, port))
            return
        millis = (time.time() - now)*1000
        elapsed = "%.3f" %millis
        print("%s ms to open socket" %(elapsed))
        total_latency_network_only = total_latency_network_only + millis
        count_latency_network_only += 1

    def encode_and_send_image(self, host, endpoint, image_file_name, show_responses):
        global total_latency_full_process
        global count_latency_full_process
        try:
            image = ""
            with open(image_file_name, "rb") as f:
                image = base64.b64encode(f.read())
        except e:
            print(e)
            exit()

        now = time.time()
        response = self.send_image_param(self.BASE_URL %host + endpoint, image)
        content = response.content
        if response.status_code != 200:
            print "non-200 response: %d: %s" %(response.status_code, content)
            return

        millis = (time.time() - now)*1000
        elapsed = "%.3f" %millis
        total_latency_full_process = total_latency_full_process + millis
        count_latency_full_process += 1
        decoded_json = json.loads(content)
        base64_size = len(image)
        if 'server_processing_time' in decoded_json:
            server_processing_time = decoded_json['server_processing_time']
        else:
            server_processing_time = "NA"
        if show_responses:
            print("%s ms - %s" %(elapsed, content))
        else:
            print("%s ms round trip. %s ms server_processing_time base64_size=%d" %(elapsed, server_processing_time, base64_size))

    def run_multi(self, num_repeat, host, endpoint, image_file_name, show_responses, thread_name):
        global do_server_stats
        global port_number
        # print("run_multi(%s, %s)\n" %(host, image_file_name))
        print("%s Starting" %thread_name)
        for x in range(num_repeat):
            self.encode_and_send_image(host, endpoint, image_file_name, show_responses)
            if x % 4 == 0:
                if do_server_stats:
                    self.get_server_stats(host)
                self.time_open_socket(host, port_number)
        print("%s Done" %thread_name)

if __name__ == "__main__":
    import sys
    from threading import Thread
    import argparse
    parser = argparse.ArgumentParser()

    parser.add_argument("-s", "--server", required=True, help="Server host name or IP address.")
    parser.add_argument("-f", "--filename", required=False, help="Name of image file to send.")
    parser.add_argument("-d", "--directory", required=False, help="Directory containing image files to send.")
    parser.add_argument("-e", "--endpoint", required=True, help="Endpoint of web service to call (e.g. /detector/detect/, /recognizer/predict/, /openpose/detect/)")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Number of times to repeat.")
    parser.add_argument("-t", "--threads", type=int, default=1, help="Number of concurent execution threads.")
    parser.add_argument("-p", "--port", type=int, default=8008, help="Port number")
    parser.add_argument("-j", "--json-params", required=False, help='Extra parameters to include with image. Ex: {subject":"Max Door", "owner":"Bruce Armstrong}')
    parser.add_argument("--show-responses", action='store_true', help="Show responses.")
    parser.add_argument("--server-stats", action='store_true', help="Get server stats every Nth frame.")
    args = parser.parse_args()

    do_server_stats = args.server_stats
    json_params = args.json_params
    port_number = args.port

    if args.filename != None and args.directory != None:
        print("Can't include both filename and directory arguments")
        sys.exit()

    if args.filename != None:
        for i in range(args.threads):
            rc = RequestClient()
            rc.BASE_URL = 'http://%s:'+str(args.port)
            thread = Thread(target=rc.run_multi, args=(args.repeat, args.server, args.endpoint, args.filename, args.show_responses, "thread-%d" %i))
            thread.start()

    elif args.directory != None:
        files = os.listdir(args.directory)
        for file in files:
            print("file=%s" %file)
            if file.startswith("."):
                continue
            for i in range(args.threads):
                rc = RequestClient()
                rc.BASE_URL = 'http://%s:'+str(args.port)
                thread = Thread(target=rc.run_multi, args=(args.repeat, args.server, args.endpoint, args.directory+"/"+file, args.show_responses, "thread-%d" %i))
                thread.start()
                thread.join()

    else:
        print("Must include either filename or directory argument")
        sys.exit()

    # Wait for the last thread to finish.
    thread.join()

    if count_latency_full_process > 0:
        average_latency_full_process = total_latency_full_process / count_latency_full_process
        print("Average Latency Full Process=%.3f ms" %average_latency_full_process)
    if count_latency_network_only > 0:
        average_latency_network_only = total_latency_network_only / count_latency_network_only
        print("Average Latency Network Only=%.3f ms" %average_latency_network_only)

    # file_size = os.path.getsize(args.filename)
    # The following line outputs CSV data that can be imported to a spreadsheet.
    #print("%s,%s,%d,%.3f,%.3f" %((args.server, args.filename, file_size, average_latency_full_process, average_latency_network_only)))
