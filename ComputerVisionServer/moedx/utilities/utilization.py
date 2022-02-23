# Copyright 2018-2021 MobiledgeX, Inc. All rights and licenses reserved.
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

import subprocess
import json
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
    This is an example command line. We will use a version below with no header or units.
    nvidia-smi --query-gpu=utilization.gpu,utilization.memory,memory.total,memory.used --format=csv
    utilization.gpu [%], utilization.memory [%], memory.total [MiB], memory.used [MiB]
    0 %, 0 %, 7980 MiB, 5135 MiB
    """
    global gpu_supported
    if not gpu_supported:
        return {}

    try:
        p = Popen(["nvidia-smi",
            "--query-gpu=utilization.gpu,utilization.memory,memory.total,memory.used,memory.free",
            "--format=csv,noheader,nounits"], stdout=PIPE)
    except FileNotFoundError:
        logger.error("'nvidia-smi' not found. Check if your Nvidia driver install is correct. GPU usage not supported.")
        gpu_supported = False
        return {} # Empty dict
    stdout, stderror = p.communicate()
    output = stdout.decode('UTF-8')
    # The output is a single line in this format:
    # 0, 0, 7980, 5135
    vals = output.split(",")
    gpu_util = vals[0].strip()
    gpu_mem_util = vals[1].strip()
    memory_total = vals[2].strip()
    memory_used = vals[3].strip()
    memory_free = vals[4].strip()
    ret = {"gpu_util": gpu_util, "gpu_mem_util": gpu_mem_util,
        "gpu_memory_total": memory_total, "gpu_memory_used":memory_used, "gpu_memory_free":memory_free}
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
    stats_memory_total = RunningStats()
    stats_memory_used = RunningStats()
    stats_memory_free = RunningStats()

    print ("%CPU, %MEM, %GPU, %GPU_MEM, MEM_TOTAL (MiB), MEM_TOTAL (MiB), MEM_FREE (MiB),")
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
        output = ""
        stats_cpu_util.push(float(decoded_json['cpu_util']))
        output += str(decoded_json['cpu_util'])
        stats_mem_util.push(float(decoded_json['mem_util']))
        output += ", " + str(decoded_json['mem_util'])
        if gpu_supported:
            stats_gpu_util.push(float(decoded_json['gpu_util']))
            output += ", " + str(decoded_json['gpu_util'])
            stats_gpu_mem_util.push(float(decoded_json['gpu_mem_util']))
            output += ", " + str(decoded_json['gpu_mem_util'])
            stats_memory_total.push(float(decoded_json['memory_total']))
            output += ", " + str(decoded_json['memory_total'])
            stats_memory_used.push(float(decoded_json['memory_used']))
            output += ", " + str(decoded_json['memory_used'])
            stats_memory_free.push(float(decoded_json['memory_free']))
            output += ", " + str(decoded_json['memory_free'])
        else:
            output += "X, X, X, X, X"
        print(output)
        time.sleep(float(args.delay))

    print("%d samples" %stats_cpu_util.n)
    if stats_cpu_util.n > 0:
        logger.info("====> Average CPU Utilization=%.1f%%" %(stats_cpu_util.mean()))
    if stats_mem_util.n > 0:
        logger.info("====> Average Memory Utilization=%.1f%%" %(stats_mem_util.mean()))
    if stats_gpu_util.n > 0:
        logger.info("====> Average GPU Utilization=%.1f%%" %(stats_gpu_util.mean()))
    if stats_gpu_mem_util.n > 0:
        logger.info("====> Average GPU Memory Utilization=%.1f%%" %(stats_gpu_mem_util.mean()))
    if stats_memory_total.n > 0:
        logger.info("====> Average GPU Memory Total=%d" %(stats_memory_total.mean()))
    if stats_memory_used.n > 0:
        logger.info("====> Average GPU Memory Used=%d" %(stats_memory_used.mean()))
    if stats_memory_free.n > 0:
        logger.info("====> Average GPU Memory Free=%d" %(stats_memory_free.mean()))
    print("%.1f, %.1f, %.1f, %.1f, %d, %d, %d" %(stats_cpu_util.mean(), stats_mem_util.mean(),
        stats_gpu_util.mean(), stats_gpu_mem_util.mean(), stats_memory_total.mean(), stats_memory_used.mean(),
        stats_memory_free.mean()))
