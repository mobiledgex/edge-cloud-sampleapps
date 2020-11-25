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

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(threadName)s - %(levelname)s - %(message)s')
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
ch.setFormatter(formatter)
logger.addHandler(ch)
fh = logging.FileHandler(__name__+'.log')
fh.setLevel(logging.DEBUG)
fh.setFormatter(formatter)

gpu_supported = True

def usage_gpu():
    """
    Execute the "nvidia-smi" command and parse the output to get the GPU usage.
    """
    global gpu_supported
    if not gpu_supported:
        return {}

    nvidia_smi = "nvidia-smi"
    try:
        p = Popen([nvidia_smi, "-q", "-d", "UTILIZATION"], stdout=PIPE)
    except FileNotFoundError:
        logger.error("%s not found. Check if your Nvidia driver install is correct. GPU usage not supported." %nvidia_smi)
        return {} # Empty dict

    stdout, stderror = p.communicate()
    output = stdout.decode('UTF-8')
    # Split on line break
    lines = output.split(os.linesep)

    index = 0
    while index < len(lines):
        line = lines[index]
        if line.strip() == "Utilization":
            line = lines[index+1]
            vals = line.split()
            gpu_util_snap = vals[2]
            line = lines[index+2]
            vals = line.split()
            gpu_mem_util_snap = vals[2]
        elif line.strip() == "GPU Utilization Samples":
            line = lines[index+3]
            vals = line.split()
            gpu_util_max = vals[2]
            line = lines[index+5]
            vals = line.split()
            gpu_util_avg = vals[2]
        elif line.strip() == "Memory Utilization Samples":
            line = lines[index+3]
            vals = line.split()
            gpu_mem_util_max = vals[2]
            line = lines[index+5]
            vals = line.split()
            gpu_mem_util_avg = vals[2]
        elif line.strip() == "ENC Utilization Samples":
            # When we hit this line, we're done
            break
        index += 1

    ret = {"gpu_util": gpu_util_snap,
            "gpu_util_max": gpu_util_max,
            "gpu_util_avg": gpu_util_avg,
            "gpu_mem_util": gpu_mem_util_snap,
            "gpu_mem_util_max": gpu_mem_util_max,
            "gpu_mem_util_avg": gpu_mem_util_avg}
    logger.debug("GPU Stats: %s" %(ret))
    return ret

def usage_gpu2():
    """
    Execute the "nvidia-smi" command and parse the output to get the GPU usage.
    """
    global gpu_supported
    if not gpu_supported:
        return {}

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
    gpu_util = vals[1]
    gpu_mem_util = vals[2]
    ret = {"gpu_util": gpu_util, "gpu_mem_util": gpu_mem_util}
    logger.debug("GPU Stats: %s" %(ret))
    return ret

def usage_cpu_and_mem():
    """
    Return the cpu and memory usage in percent.
    """
    import psutil
    cpu = psutil.cpu_percent()
    mem = dict(psutil.virtual_memory()._asdict())['percent']
    ret = {"cpu_util": cpu,
            "mem_util": mem}
    logger.debug("CPU Stats: %s" %(ret))
    return ret

if __name__ == "__main__":
    logger.addHandler(fh)

    parser = argparse.ArgumentParser()
    parser.add_argument("-s", "--seconds", required=True, help="Number of seconds to run")
    parser.add_argument("-d", "--delay", required=False, default=0.25, help="Number of seconds to wait between each sample")
    args = parser.parse_args()

    from stats import RunningStats
    stats_cpu_util = RunningStats()
    stats_mem_util = RunningStats()
    stats_gpu_util = RunningStats()
    stats_gpu_mem_util = RunningStats()
    stats_gpu_util_max = RunningStats()
    stats_gpu_mem_util_max = RunningStats()
    stats_gpu_util_avg = -1
    stats_gpu_mem_util_avg = -1

    print ("%CPU, %MEM, %GPU, %GPU_MAX, %GPU_AVG, %GPU_MEM, %GPU_MEM_MAX, %GPU_MEM_AVG")
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
        stats_cpu_util.push(float(decoded_json['cpu_util']))
        output += str(decoded_json['cpu_util'])
        stats_mem_util.push(float(decoded_json['mem_util']))
        output += ", " + str(decoded_json['mem_util'])
        if 'gpu_util' in decoded_json:
            stats_gpu_util.push(float(decoded_json['gpu_util']))
            output += ", " + str(decoded_json['gpu_util'])
        else:
            output += ", X"
        if 'gpu_util_max' in decoded_json:
            stats_gpu_util_max.push(float(decoded_json['gpu_util_max']))
            output += ", " + str(decoded_json['gpu_util_max'])
        else:
            output += ", X"
        if 'gpu_util_avg' in decoded_json:
            stat = float(decoded_json['gpu_util_avg'])
            if stat > stats_gpu_util_avg:
                stats_gpu_util_avg = stat
            output += ", " + str(stat)
        else:
            output += ", X"
        if 'gpu_mem_util' in decoded_json:
            stats_gpu_mem_util.push(float(decoded_json['gpu_mem_util']))
            output += ", " + str(decoded_json['gpu_mem_util'])
        else:
            output += ", X"
        if 'gpu_mem_util_max' in decoded_json:
            stats_gpu_mem_util_max.push(float(decoded_json['gpu_mem_util_max']))
            output += ", " + str(decoded_json['gpu_mem_util_max'])
        else:
            output += ", X"
        if 'gpu_mem_util_avg' in decoded_json:
            stat = float(decoded_json['gpu_mem_util_avg'])
            if stat > stats_gpu_mem_util_avg:
                stats_gpu_mem_util_avg = stat
            output += ", " + str(stat)
        else:
            output += ", X"
        print(output)
        time.sleep(float(args.delay))

    print("%d samples" %stats_cpu_util.n)
    if stats_cpu_util.n > 0:
        logger.info("====> Average CPU Utilization=%.1f%%" %(stats_cpu_util.mean()))
    if stats_mem_util.n > 0:
        logger.info("====> Average Memory Utilization=%.1f%%" %(stats_mem_util.mean()))
    if stats_gpu_util.n > 0:
        logger.info("====> Average GPU Utilization Snapshot=%.1f%%" %(stats_gpu_util.mean()))
    if stats_gpu_util_max.n > 0:
        logger.info("====> Average GPU Utilization Max=%.1f%%" %(stats_gpu_util_max.mean()))
    if stats_gpu_util_avg > 0:
        logger.info("====> Average GPU Utilization Avg=%.1f%%" %(stats_gpu_util_avg))
    if stats_gpu_mem_util.n > 0:
        logger.info("====> Average GPU Memory Utilization Snapshot=%.1f%%" %(stats_gpu_mem_util.mean()))
    if stats_gpu_mem_util_max.n > 0:
        logger.info("====> Average GPU Memory Utilization Max=%.1f%%" %(stats_gpu_mem_util_max.mean()))
    if stats_gpu_mem_util_avg > 0:
        logger.info("====> Average GPU Memory Utilization Avg=%.1f%%" %(stats_gpu_mem_util_avg))
    print("%.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f" %(stats_cpu_util.mean(), stats_mem_util.mean(),
        stats_gpu_util.mean(), stats_gpu_util_max.mean(), stats_gpu_util_avg,
        stats_gpu_mem_util.mean(), stats_gpu_mem_util_max.mean(), stats_gpu_mem_util_avg))
