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

import detectron2

# import some common detectron2 utilities
from detectron2.engine import DefaultPredictor
from detectron2.config import get_cfg
from detectron2.data import MetadataCatalog

import os, sys
import time
import cv2
import requests
import numpy as np
import json
import logging

logger = logging.getLogger(__name__)
logger.info(f"Detectron2 version is {detectron2.__version__}")

# create config
cfg = get_cfg()
# below path applies to current installation location of Detectron2
cfg.merge_from_file("/home/ubuntu/detectron2/configs/COCO-Detection/faster_rcnn_R_101_FPN_3x.yaml")
cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = 0.5  # set threshold for this model
cfg.MODEL.WEIGHTS = "detectron2://COCO-Detection/faster_rcnn_R_101_FPN_3x/137851257/model_final_f6e8b1.pkl"
classes = MetadataCatalog.get(cfg.DATASETS.TRAIN[0]).thing_classes

# create predictor
predictor = DefaultPredictor(cfg)

# TODO: Programmatically determine if GPU support exists.
gpu_support = False

# Globals
total_server_processing_time = 0
count_server_processing_time = 0

class ObjectDetector(object):
    def __init__(self):
        pass

    def is_gpu_supported(self):
        return gpu_support

    def detect_image(self, img):
        # make prediction
        output = predictor(img)

        instances = output["instances"]
        scores = instances.get_fields()["scores"].tolist()
        pred_classes = instances.get_fields()["pred_classes"].tolist()
        pred_boxes = instances.get_fields()["pred_boxes"].tensor.tolist()

        objects = []

        i = 0
        for box in pred_boxes:
            object = {"rect": [int(box[0]), int(box[1]), int(box[2]), int(box[3])],
                "class": classes[int(pred_classes[i])],
                "confidence": "%.2f" %scores[i]}
            objects.append(object)
            i += 1

        return objects

    def process_image(self, img, outdir = None):
        """
        Convert and process the image. If "outdir" is included,
        save the processed image there. (Only for command-line executions.
        It will always be None when called by the Django server.)
        """
        global total_server_processing_time
        global count_server_processing_time
        # load image and get detections
        prev_time = time.time()
        detections = self.detect_image(img)
        millis = (time.time() - prev_time)*1000
        elapsed = "%.3f" %millis
        count_server_processing_time += 1
        total_server_processing_time += millis

        return detections


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("-f", "--filename", required=False, help="Name of image file to send.")
    parser.add_argument("-d", "--directory", required=False, help="Directory containing image files to process.")
    parser.add_argument("-o", "--outdir", required=False, help="Directory to output processed images to.")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Number of times to repeat.")
    args = parser.parse_args()

    if args.filename != None and args.directory != None:
        print("Can't include both filename and directory arguments")
        sys.exit()
    if args.filename is None and args.directory is None:
        print("Must include either filename or directory arguments")
        sys.exit()

    detector = ObjectDetector()

    if args.filename != None:
        files = [args.filename]
        dir_prefix = ""

    elif args.directory != None:
        files = os.listdir(args.directory)
        dir_prefix = args.directory+"/"

    # valid_extensions = ('jpg', 'jpeg', 'png')
    valid_extensions = ('jpg', 'jpeg')

    for x in range(args.repeat):
        for image_name in files:
            if not image_name.endswith(valid_extensions):
                continue
            img = cv2.imread(dir_prefix+image_name)
            results = detector.process_image(img, args.outdir)
            print(image_name, results)

    if count_server_processing_time > 0:
        average_server_processing_time = total_server_processing_time / count_server_processing_time
        print("Average Server Processing Time=%.3f ms" %average_server_processing_time)
