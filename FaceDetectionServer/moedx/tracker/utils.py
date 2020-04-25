import psutil
import logging
import os
import json
from subprocess import Popen, PIPE

logger = logging.getLogger(__name__)

def usage_gpu():
    """
    Execute the "nvidia-smi" command and parse the output to get the GPU usage.
    """
    nvidia_smi = "nvidia-smi"
    try:
        p = Popen([nvidia_smi,"-q", "-d", "UTILIZATION"], stdout=PIPE)
    except FileNotFoundError:
        logger.error("%s not found. GPU usage not supported." %nvidia_smi)
        return {} # Empty dict

    stdout, stderror = p.communicate()
    output = stdout.decode('UTF-8')
    # Split on line break
    lines = output.split(os.linesep)
    gpu_utilization_snapshot_section = False
    gpu_utilization_samples_section = False
    memory_utilization_samples_section = False
    gpu_utilization_snapshot = -1
    gpu_utilization_high = -1
    memory_utilization_high = -1

    for line in lines:
        # print(line, gpu_utilization_snapshot_section, gpu_utilization_samples_section, memory_utilization_samples_section)
        if line.strip() == "Utilization":
            gpu_utilization_snapshot_section = True
            gpu_utilization_samples_section = False
            memory_utilization_samples_section = False
        elif line.strip() == "GPU Utilization Samples":
            gpu_utilization_snapshot_section = False
            gpu_utilization_samples_section = True
            memory_utilization_samples_section = False
        elif line.strip() == "Memory Utilization Samples":
            gpu_utilization_snapshot_section = False
            gpu_utilization_samples_section = False
            memory_utilization_samples_section = True
        elif line.strip() == "ENC Utilization Samples":
            # When we hit this line, we're done
            break
        elif line.strip().startswith("Gpu"):
            if gpu_utilization_snapshot_section:
                vals = line.split()
                gpu_utilization_snapshot = vals[2]
        elif line.strip().startswith("Max"):
            if gpu_utilization_samples_section:
                vals = line.split()
                gpu_utilization_high = vals[2]
            elif memory_utilization_samples_section:
                vals = line.split()
                memory_utilization_high = vals[2]

    ret = {"gpu_utilization_snapshot": gpu_utilization_snapshot,
            "gpu_utilization_high": gpu_utilization_high,
            "gpu_memory_utilization_high": memory_utilization_high}
    logger.info("GPU Stats: %s" %(ret))
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
    logger.info("CPU Stats: %s" %(ret))
    return ret

if __name__ == "__main__":
    print(usage_cpu_and_mem())
    print(usage_gpu())
