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
This script is for launching multiple simultaneous multi_client instances to benchmark a given server.
"""

import sys
import os
import json
import time
import logging
import argparse
import requests
from threading import Thread
util_dir = "../utilities"
sys.path.append(os.path.join(os.path.dirname(__file__), util_dir))
from stats import RunningStats

stats_latency_full_process = RunningStats()
stats_latency_network_only = RunningStats()
stats_server_processing_time = RunningStats()
stats_cpu_util = RunningStats()
stats_mem_util = RunningStats()
stats_gpu_util = RunningStats()
stats_gpu_util_max = RunningStats()
stats_gpu_util_avg = RunningStats()
stats_gpu_mem_util = RunningStats()
stats_gpu_mem_util_max = RunningStats()
stats_gpu_mem_util_avg = RunningStats()

LAUNCH_INTERVAL = 2 # Seconds
filename = "objects_320x180.mp4"

def launch_remote_benchmark(url):
    # Comment or uncomment lines to change test options.
    # body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c websocket -f %s -n PING --server-stats --skip-frames 5' %filename
    body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c websocket -f %s -n PING --server-stats ' %filename
    # body = '-s 80.187.140.9 -e /object/detect/ -c websocket -f %s -n PING --server-stats' %filename
    # body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c websocket -f %s -n PING --fullsize' %filename
    # body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c websocket -f %s -n PING' %filename
    # body = '-s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net --tls -e /object/detect/ -c rest -f %s -n PING --skip-frames 4' %filename
    response = requests.post("%s/client/benchmark/" %url, data=body)
    print("Response for %s:" %url)
    data = response.content.decode("utf-8")
    print(data)

    # Get the line of the output that contains the CSV stats, and add them to our
    # RunningStats instances so we can average them when all responses have arrived.
    lines = data.split(os.linesep)
    stats_line = lines[-3]
    stats = stats_line.split(',')
    # 2020-11-12 21:59:10,741 - Server, Full Process, Network Only, Server Time, CPU Util, Mem Util, GPU Util, GPU Util Max, GPU Util Avg, GPU Mem Util, GPU Mem Util Max, GPU Mem Util Avg
    # 2020-11-12 21:59:10,741 - cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net, 433.526, 9.040, 33.395, 26.626, 16.320, 26.833, 61.635, 33.974, 15.759, 34.826, 19.487

    stats_latency_full_process.push(float(stats[2]))
    stats_latency_network_only.push(float(stats[3]))
    stats_server_processing_time.push(float(stats[4]))
    stats_cpu_util.push(float(stats[5]))
    stats_mem_util.push(float(stats[6]))
    stats_gpu_util.push(float(stats[7]))
    stats_gpu_util_max.push(float(stats[8]))
    stats_gpu_util_avg.push(float(stats[9]))
    stats_gpu_mem_util.push(float(stats[10]))
    stats_gpu_mem_util_max.push(float(stats[11]))
    stats_gpu_mem_util_avg.push(float(stats[12]))
    print('{}/{} clients reporting:'.format(stats_latency_full_process.n, args.num_clients))

    # Full Details
    header = "Num Clients, Full Process, FPS/Client, Total FPS, Network Only, Server Time, % CPU, %MEM, %GPU, %GPU Max, %GPU Avg, %GPU Mem Util, %GPU Mem Max, %GPU Mem Avg"
    csv = '{}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}'.format(
        stats_latency_full_process.n, stats_latency_full_process.mean(),
        1/stats_latency_full_process.mean()*1000, 1/stats_latency_full_process.mean()*1000*stats_latency_full_process.n,
        stats_latency_network_only.mean(), stats_server_processing_time.mean(), stats_cpu_util.mean(), stats_mem_util.mean(),
        stats_gpu_util.mean(), stats_gpu_util_max.mean(), stats_gpu_util_avg.mean(),
        stats_gpu_mem_util.mean(), stats_gpu_mem_util_max.mean(), stats_gpu_mem_util_avg.mean())
    print(header)
    print(csv)

    # Only what we want to graph
    header = "Num Clients, FPS/Client, Total FPS, % CPU, %Mem, %GPU, %GPU Mem"
    csv = '{}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, {:.2f}'.format(
        stats_latency_full_process.n,
        1/stats_latency_full_process.mean()*1000, 1/stats_latency_full_process.mean()*1000*stats_latency_full_process.n,
        stats_cpu_util.mean(), stats_mem_util.mean(),
        stats_gpu_util.mean(),
        stats_gpu_mem_util.mean())
    print(header)
    print(csv)

    if stats_latency_full_process.n == args.num_clients:
        print("__CSV__: %s" %csv)


def remote_download(url):
    params = {'url': 'http://acrotopia.com/mobiledgex/%s' %filename}
    # params = {'url': 'http://opencv.facetraining.mobiledgex.net/videos/landscape/%s' %filename}
    response = requests.post("%s/client/download/" %url, data=params)
    print("Response for %s:" %url)
    print(response.content.decode("utf-8"))

urls = [
    "http://80.187.140.9:8008",
    # "https://cv-gpu-cluster.hamburg-main.tdg.mobiledgex.net:8008",
    "https://cv-gpu-cluster.berlin-main.tdg.mobiledgex.net:8008",
    "https://cv-gpu-cluster.munich-main.tdg.mobiledgex.net:8008",
    "https://cv-gpu-cluster.dusseldorf-main.tdg.mobiledgex.net:8008",
    "https://cv-cluster.berlin-main.tdg.mobiledgex.net:8008",
]

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("-n", "--num-clients", type=int, default=1, help="Number of clients to launch")
    parser.add_argument("-d", "--download", action='store_true', help="Download media file to client machine instead of launching")
    args = parser.parse_args()

    if args.download:
        for url in urls[:args.num_clients]:
            print("Starting download for %s" %url)
            remote_download(url)
        sys.exit()

    for url in urls[:args.num_clients]:
        print("Starting %s" %url)
        thread = Thread(target=launch_remote_benchmark, args=(url,))
        thread.start()
        time.sleep(LAUNCH_INTERVAL)

    print("All started")
    print()

    thread.join()
