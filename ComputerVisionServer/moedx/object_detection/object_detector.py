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

# First off, add the submodule's path so we can import its packages.
import os, sys
submodule = "../pytorch_objectdetecttrack"
sys.path.append(os.path.join(os.path.dirname(__file__), submodule))

from models import *
from utils import *

import time, datetime, random
import torch
from torch.utils.data import DataLoader
from torchvision import datasets, transforms
from torch.autograd import Variable

import matplotlib.pyplot as plt
import matplotlib.patches as patches
from PIL import Image
import cv2
import json
import logging
from threading import Thread

util_dir = "../utilities"
sys.path.append(os.path.join(os.path.dirname(__file__), util_dir))
from stats import RunningStats
from utilization import usage_cpu_and_mem, usage_gpu

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(threadName)s - %(levelname)s - %(message)s')
fh = logging.FileHandler('object_detector.log')
fh.setLevel(logging.DEBUG)
fh.setFormatter(formatter)
logger.addHandler(fh)
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
ch.setFormatter(formatter)
logger.addHandler(ch)

SERVER_STATS_INTERVAL = 1.5 # Seconds
SERVER_STATS_DELAY = 2 # Seconds

package_dir = os.path.dirname(os.path.abspath(__file__))+"/"+submodule
config_path=package_dir+'/config/yolov3.cfg'
weights_path=package_dir+'/config/yolov3.weights'
class_path=package_dir+'/config/coco.names'
img_size=416
conf_thres=0.8
nms_thres=0.4

# Load model and weights
model = Darknet(config_path, img_size=img_size)
model.load_weights(weights_path)

gpu_support = False
if torch.cuda.is_available():
    model.cuda()
    Tensor = torch.cuda.FloatTensor
    gpu_support = True
else:
    Tensor = torch.FloatTensor

model.eval()
classes = utils.load_classes(class_path)

class ObjectDetector(object):
    MULTI_THREADED = False
    stats_processing_time = RunningStats()
    stats_cpu_utilization = RunningStats()
    stats_mem_utilization = RunningStats()
    stats_gpu_utilization = RunningStats()
    stats_gpu_mem_utilization = RunningStats()

    def __init__(self):
        self.stats_processing_time = RunningStats()
        self.stats_cpu_utilization = RunningStats()
        self.stats_mem_utilization = RunningStats()
        self.stats_gpu_utilization = RunningStats()
        self.stats_gpu_mem_utilization = RunningStats()
        self.media_file_name = None
        self.loop_count = 0
        self.num_repeat = 0
        self.num_vid_repeat = 0
        self.filename_list = []
        self.filename_list_index = 0
        self.video = None
        self.resize = True
        self.resize_long = 240
        self.resize_short = 180
        self.skip_frames = 1
        pass

    def start(self):
        self.running = True
        logger.debug("media file(s) %s" %(self.filename_list))
        video_extensions = ('mp4', 'avi', 'mov')
        if self.filename_list[0].endswith(video_extensions):
            logger.debug("It's a video")
            self.media_file_name = self.filename_list[0]
            self.video = cv2.VideoCapture(self.media_file_name)

        while True:
            image = self.get_next_image()
            if image is None:
                break

            results = self.process_image(image, self.outdir)
            if self.show_responses:
                logger.info(results)

        self.display_results()

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
                image = cv2.resize(image, (resize_w, resize_h))
                logger.debug("Resized image to: %dx%d" %(resize_w, resize_h))
        else:
            # If the filename_list array has more than 1, get the next value.
            if len(self.filename_list) > 1:
                self.filename_list_index += 1
                if self.filename_list_index >= len(self.filename_list):
                    self.filename_list_index = 0
            else:
                self.filename_list_index = 0

            if self.stats_processing_time.n >= self.num_repeat:
                return None

            self.media_file_name = self.filename_list[self.filename_list_index]
            image = cv2.imread(self.media_file_name)

        return image

    def display_results(self):
        self.running = False
        if not self.show_responses or not ObjectDetector.MULTI_THREADED:
            return

        if self.stats_processing_time.n > 0:
            logger.info("====> Average Processing Time=%.3f ms (stddev=%.3f)" %(self.stats_processing_time.mean(), self.stats_processing_time.stddev()))

    def measure_server_stats(self):
        time.sleep(SERVER_STATS_DELAY)
        while self.running:
            ret1 = usage_cpu_and_mem()
            ret2 = usage_gpu()
            ret = ret1.copy()
            ret.update(ret2)
            decoded_json = ret
            if 'cpu_utilization' in decoded_json:
                self.stats_cpu_utilization.push(float(decoded_json['cpu_utilization']))
                ObjectDetector.stats_cpu_utilization.push(float(decoded_json['cpu_utilization']))
            if 'mem_utilization' in decoded_json:
                self.stats_mem_utilization.push(float(decoded_json['mem_utilization']))
                ObjectDetector.stats_mem_utilization.push(float(decoded_json['mem_utilization']))
            if 'gpu_utilization' in decoded_json:
                self.stats_gpu_utilization.push(float(decoded_json['gpu_utilization']))
                ObjectDetector.stats_gpu_utilization.push(float(decoded_json['gpu_utilization']))
            if 'gpu_mem_utilization' in decoded_json:
                self.stats_gpu_mem_utilization.push(float(decoded_json['gpu_mem_utilization']))
                ObjectDetector.stats_gpu_mem_utilization.push(float(decoded_json['gpu_mem_utilization']))
            if self.show_responses:
                logger.info(json.dumps(decoded_json))
            time.sleep(SERVER_STATS_INTERVAL)

    def is_gpu_supported(self):
        return gpu_support

    def detect_image(self, img):
        # scale and pad image
        ratio = min(img_size/img.size[0], img_size/img.size[1])
        imw = round(img.size[0] * ratio)
        imh = round(img.size[1] * ratio)
        img_transforms = transforms.Compose([ transforms.Resize((imh, imw)),
             transforms.Pad((max(int((imh-imw)/2),0), max(int((imw-imh)/2),0), max(int((imh-imw)/2),0), max(int((imw-imh)/2),0)),
                            (128,128,128)),
             transforms.ToTensor(),
             ])
        # convert image to Tensor
        image_tensor = img_transforms(img).float()
        image_tensor = image_tensor.unsqueeze_(0)
        input_img = Variable(image_tensor.type(Tensor))
        # run inference on the model and get detections
        with torch.no_grad():
            detections = model(input_img)
            detections = utils.non_max_suppression(detections, 80, conf_thres, nms_thres)
        return detections[0]

    def process_image(self, img, outdir = None):
        """
        Convert and process the image. If "outdir" is included,
        save the processed image there. (Only for command-line executions.
        It will always be None when called by the Django server.)
        """
        # load image and get detections
        img = Image.fromarray(img, 'RGB')
        prev_time = time.time()
        detections = self.detect_image(img)
        millis = (time.time() - prev_time)*1000
        elapsed = "%.3f" %millis
        self.stats_processing_time.push(millis)
        ObjectDetector.stats_processing_time.push(millis)

        if outdir is not None:
            filename = img.filename
            img = np.array(img)
            # Get bounding-box colors
            cmap = plt.get_cmap('tab20b')
            colors = [cmap(i) for i in np.linspace(0, 1, 20)]

            plt.figure()
            fig, ax = plt.subplots(1, figsize=(12,9))
            ax.imshow(img)
        else:
            img = np.array(img)

        pad_x = max(img.shape[0] - img.shape[1], 0) * (img_size / max(img.shape))
        pad_y = max(img.shape[1] - img.shape[0], 0) * (img_size / max(img.shape))
        unpad_h = img_size - pad_y
        unpad_w = img_size - pad_x

        # Build list of results
        objects = []
        if detections is not None:
            if outdir is not None:
                unique_labels = detections[:, -1].cpu().unique()
                n_cls_preds = len(unique_labels)
                bbox_colors = random.sample(colors, n_cls_preds)
            # browse detections and draw bounding boxes
            for x1, y1, x2, y2, conf, cls_conf, cls_pred in detections:
                box_h = ((y2 - y1) / unpad_h) * img.shape[0]
                box_w = ((x2 - x1) / unpad_w) * img.shape[1]
                y1 = ((y1 - pad_y // 2) / unpad_h) * img.shape[0]
                x1 = ((x1 - pad_x // 2) / unpad_w) * img.shape[1]
                if outdir is not None:
                    color = bbox_colors[int(np.where(unique_labels == int(cls_pred))[0])]
                    bbox = patches.Rectangle((x1, y1), box_w, box_h, linewidth=2, edgecolor=color, facecolor='none')
                    ax.add_patch(bbox)
                    plt.text(x1, y1, s=classes[int(cls_pred)], color='white', verticalalignment='top',
                            bbox={'color': color, 'pad': 0})
                confidence = "%.2f" %float(cls_conf)
                object = {"rect": [int(x1), int(y1), int(x1+box_w), int(y1+box_h)], "class": classes[int(cls_pred)], "confidence": confidence}
                objects.append(object)

        if outdir is not None:
            plt.axis('off')
            outname = outdir+"/det-"+os.path.basename(filename)
            print("Saving to "+outname)
            plt.savefig(outname, bbox_inches='tight', pad_inches=0.0)
            #plt.show()
            plt.close('all')

        return objects

def main():
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", "--filename", required=False, help="Name of image file to send.")
    parser.add_argument("-d", "--directory", required=False, help="Directory containing image files to process.")
    parser.add_argument("-o", "--outdir", required=False, help="Directory to output processed images to.")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Number of times to repeat.")
    parser.add_argument("-t", "--threads", type=int, default=1, help="Number of concurrent execution threads.")
    parser.add_argument("--show-responses", action='store_true', help="Show responses.")
    parser.add_argument("--skip-frames", type=int, default=1, help="For video, send every Nth frame.")
    parser.add_argument("--fullsize", action='store_true', help="Maintain original image size. Default is to shrink the image before sending.")
    parser.add_argument("--server-stats", action='store_true', help="Get server stats every Nth frame.")
    args = parser.parse_args()

    start_time = time.time()

    if args.threads > 1:
        ObjectDetector.MULTI_THREADED = True
    for x in range(args.threads):
        detector = ObjectDetector()
        if args.filename != None and args.directory != None:
            logger.error("Can't include both filename and directory arguments")
            parser.print_usage()
            return False

        if args.filename != None:
            if not os.path.isfile(args.filename):
                return [False, "%s doesn't exist" %args.filename]
            detector.filename_list.append(args.filename)

        elif args.directory != None:
            if not os.path.isdir(args.directory):
                return [False, "%s doesn't exist" %args.directory]
            valid_extensions = ('jpg','jpeg', 'png')
            files = os.listdir(args.directory)
            for file in files:
                if file.endswith(valid_extensions):
                    detector.filename_list.append(args.directory+"/"+file)
            if len(detector.filename_list) == 0:
                return [False, "%s contains no valid image files" %args.directory]

        else:
            logger.error("Must include either filename or directory argument")
            parser.print_usage()
            return False

        detector.filename_list_index = -1
        detector.num_repeat = args.repeat * len(detector.filename_list)
        detector.resize = not args.fullsize
        detector.skip_frames = args.skip_frames
        detector.outdir = args.outdir
        detector.show_responses = args.show_responses
        detector.do_server_stats = args.server_stats

        thread = Thread(target=detector.start)
        thread.start()
        logger.debug("Started %s" %thread)

    if args.server_stats:
        thread = Thread(target=detector.measure_server_stats)
        thread.start()
        logger.debug("Started background measure_server_stats %s" %thread)

    logger.info("All started")
    thread.join()

    session_time = time.time() - start_time

    if ObjectDetector.stats_processing_time.n > 0:
        fps = ObjectDetector.stats_processing_time.n / session_time
        header1 = "Grand totals"
        header2 = "%d threads repeated %d times on %d files. %d total frames. FPS=%.2f" %(args.threads, args.repeat, len(detector.filename_list), ObjectDetector.stats_processing_time.n, fps)
        separator = ""
        for s in header2: separator += "="
        logger.info(separator)
        logger.info(header1)
        logger.info(header2)
        logger.info(separator)
        fps = 1/ObjectDetector.stats_processing_time.mean()*1000 * args.threads
        logger.info("====> Average Processing Time=%.3f ms (stddev=%.3f) FPS=%.2f" %(ObjectDetector.stats_processing_time.mean(), ObjectDetector.stats_processing_time.stddev(), fps))
        if ObjectDetector.stats_cpu_utilization.n > 0:
            logger.info("====> Average CPU Utilization=%.1f%%" %(ObjectDetector.stats_cpu_utilization.mean()))
        if ObjectDetector.stats_mem_utilization.n > 0:
            logger.info("====> Average Memory Utilization=%.1f%%" %(ObjectDetector.stats_mem_utilization.mean()))
        if ObjectDetector.stats_gpu_utilization.n > 0:
            logger.info("====> Average GPU Utilization=%.1f%%" %(ObjectDetector.stats_gpu_utilization.mean()))
        if ObjectDetector.stats_gpu_mem_utilization.n > 0:
            logger.info("====> Average GPU Memory Utilization=%.1f%%" %(ObjectDetector.stats_gpu_mem_utilization.mean()))

if __name__ == "__main__":
    print(main())
