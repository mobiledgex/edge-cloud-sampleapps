# Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
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

import sys
import os
import subprocess
import struct
import json
import time
import logging
import datetime
import time
import psutil
import logging
import argparse
from subprocess import Popen, PIPE
try:
    from stats import RunningStats
except Exception as e:
    from .stats import RunningStats

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(threadName)s - %(levelname)s - %(message)s')
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
ch.setFormatter(formatter)
logger.addHandler(ch)

gpu_supported = True

def usage_gpu():
    """
    Execute the "nvidia-smi" command and parse the output to get the GPU usage.
    """
    global gpu_supported
    try:
        p = Popen(["nvidia-smi", "dmon", "-s", "u", "-c", "1"], stdout=PIPE)
    except FileNotFoundError:
        logger.error("'nvidia-smi' not found. Check if your Nvidia driver install is correct. GPU usage not supported.")
        gpu_supported = False
        return {} # Empty dict
    stdout, stderror = p.communicate()
    output = stdout.decode('UTF-8')
    # Split on line break
    lines = output.split(os.linesep)
    vals = lines[2].split()
    gpu_utilization = vals[1]
    gpu_mem_utilization = vals[2]
    ret = {"gpu_utilization": gpu_utilization, "gpu_mem_utilization": gpu_mem_utilization}
    logger.debug("GPU Stats: %s" %(ret))
    return ret

def usage_cpu_and_mem():
    """
    Return the cpu and memory usage in percent.
    """
    import psutil
    cpu = psutil.cpu_percent()
    mem = dict(psutil.virtual_memory()._asdict())['percent']
    ret = {"cpu_utilization": cpu,
            "mem_utilization": mem}
    logger.debug("CPU Stats: %s" %(ret))
    return ret

if __name__ == "__main__":
    fh = logging.FileHandler(__name__+'.log')
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(formatter)
    logger.addHandler(fh)

    parser = argparse.ArgumentParser()
    parser.add_argument("-s", "--seconds", required=True, help="Number of seconds to run")
    parser.add_argument("-d", "--delay", required=False, default=0.25, help="Number of seconds to wait between each sample")
    args = parser.parse_args()

    stats_cpu_utilization = RunningStats()
    stats_mem_utilization = RunningStats()
    stats_gpu_utilization = RunningStats()
    stats_gpu_mem_utilization = RunningStats()

    print ("%CPU, %MEM, %GPU, %GPU_MEM")
    time.sleep(float(0.1))
    finish_time = datetime.datetime.now() + datetime.timedelta(seconds=float(args.seconds))
    while datetime.datetime.now() < finish_time:
        ret1 = usage_cpu_and_mem()
        ret = ret1.copy()
        if gpu_supported:
            ret2 = usage_gpu()
            ret.update(ret2)
        decoded_json = ret
        json_ret = json.dumps(ret)
        # print(json_ret)
        output = ""
        stats_cpu_utilization.push(float(decoded_json['cpu_utilization']))
        output += str(decoded_json['cpu_utilization'])
        stats_mem_utilization.push(float(decoded_json['mem_utilization']))
        output += ", " + str(decoded_json['mem_utilization'])
        if 'gpu_utilization' in decoded_json:
            stats_gpu_utilization.push(float(decoded_json['gpu_utilization']))
            output += ", " + str(decoded_json['gpu_utilization'])
        else:
            output += ", X"
        if 'gpu_mem_utilization' in decoded_json:
            stats_gpu_mem_utilization.push(float(decoded_json['gpu_mem_utilization']))
            output += ", " + str(decoded_json['gpu_mem_utilization'])
        else:
            output += ", X"
        print(output)
        time.sleep(float(args.delay))

    print("%d samples" %stats_cpu_utilization.n)
    if stats_cpu_utilization.n > 0:
        logger.info("====> Average CPU Utilization=%.1f%%" %(stats_cpu_utilization.mean()))
    if stats_mem_utilization.n > 0:
        logger.info("====> Average Memory Utilization=%.1f%%" %(stats_mem_utilization.mean()))
    if stats_gpu_utilization.n > 0:
        logger.info("====> Average GPU Utilization=%.1f%%" %(stats_gpu_utilization.mean()))
    if stats_gpu_mem_utilization.n > 0:
        logger.info("====> Average GPU Memory Utilization=%.1f%%" %(stats_gpu_mem_utilization.mean()))
    print("%.1f, %.1f, %.1f, %.1f" %(stats_cpu_utilization.mean(), stats_mem_utilization.mean(), stats_gpu_utilization.mean(), stats_gpu_mem_utilization.mean()))
