# This script is for launching multiple simultaneous multi_client instances to benchmark a given server.

import sys
import os
import json
import time
import logging
import argparse
import requests
from threading import Thread

LAUNCH_INTERVAL = 1 # Seconds
# filename = "objects_320x180_x2.mp4"
filename = "objects_320x180.mp4"
# filename = "objects.mp4"

def launch_remote_benchmark(url):
    # Comment or uncomment lines to change test options.
    body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c websocket -f %s -n PING --server-stats' %filename
    # body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c websocket -f %s -n PING --fullsize' %filename
    # body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c websocket -f %s -n PING' %filename
    # body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c rest -f %s -n PING --skip-frames 4' %filename
    # body = '-s 80.187.140.9 -e /object/detect/ -c websocket -f %s -n PING --server-stats' %filename
    response = requests.post("%s/client/benchmark/" %url, data=body)
    print("Response for %s:" %url)
    print(response.content.decode("utf-8"))

def remote_download(url):
    # params = {'url': 'http://acrotopia.com/mobiledgex/%s' %filename}
    params = {'url': 'http://opencv.facetraining.mobiledgex.net/videos/landscape/%s' %filename}
    response = requests.post("%s/client/download/" %url, data=params)
    print("Response for %s:" %url)
    print(response.content.decode("utf-8"))

urls = [
    "http://80.187.140.9:8008",
    "https://cv-gpu-cluster.hamburg-main.tdg.mobiledgex.net:8008",
    "https://cv-gpu-cluster.berlin-main.tdg.mobiledgex.net:8008",
    "https://cv-gpu-cluster.munich-main.tdg.mobiledgex.net:8008",
    "https://cv-gpu-cluster.dusseldorf-main.tdg.mobiledgex.net:8008",
]

# for url in urls:
#     print("Starting %s" %url)
#     remote_download(url)

for url in urls:
    print("Starting %s" %url)
    thread = Thread(target=launch_remote_benchmark, args=(url,))
    thread.start()
    time.sleep(LAUNCH_INTERVAL)

print("All started")
print()
