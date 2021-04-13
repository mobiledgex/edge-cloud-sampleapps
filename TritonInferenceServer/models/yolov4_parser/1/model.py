# Copyright 2020-2021 MobiledgeX, Inc. All rights and licenses reserved.
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

import numpy as np
import sys
import json
import os
import logging

# You need to include triton_python_backend_utils here to be able to work with
# inference requests and responses. It also contains some utility functions for
# extracting information from model_config and converting Triton input/output
# types to numpy types. You must copy
# src/resources/triton_python_backend_utils.py in the appropriate location so
# that the import below works properly.
import triton_python_backend_utils as pb_utils

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
ch.setFormatter(formatter)
logger.addHandler(ch)

BBOX_CONF_THRESH = 0.7
CONFIDENCE_THRESHOLD = 0.8

class TritonPythonModel:
    """Your Python model must use the same class name. Every Python model
    that is created must have "TritonPythonModel" as the class name.
    """

    def initialize(self, args):
        """`initialize` is called only once when the model is being loaded.
        Implementing `initialize` function is optional. This function allows
        the model to intialize any state associated with this model.

        Parameters
        ----------
        args : dict
          Both keys and values are strings. The dictionary keys and values are:
          * model_config: A JSON string containing the model configuration
          * model_instance_kind: A string containing model instance kind
          * model_instance_device_id: A string containing model instance device ID
          * model_repository: Model repository path
          * model_version: Model version
          * model_name: Model name
        """

        # You must parse model_config. JSON string is not parsed here
        self.model_config = model_config = json.loads(args['model_config'])

        # Get yolov4_parser output configuration
        detected_objects_config = pb_utils.get_output_config_by_name(
            model_config, "DETECTED_OBJECTS_JSON")

        # Convert Triton types to numpy types
        self.detected_objects_dtype = pb_utils.triton_string_to_numpy(
            detected_objects_config['data_type'])

        logger.info(f"detected_objects_dtype={self.detected_objects_dtype}")

        dir_path = os.path.dirname(os.path.realpath(__file__))
        logger.info(f"dir_path={dir_path}")
        self.class_names = [c.strip() for c in open(dir_path+'/coco.names').readlines()]
        logger.info(self.class_names)

    def execute(self, requests):
        """`execute` MUST be implemented in every Python model. `execute`
        function receives a list of pb_utils.InferenceRequest as the only
        argument. This function is called when an inference request is made
        for this model. Depending on the batching configuration (e.g. Dynamic
        Batching) used, `requests` may contain multiple requests. Every
        Python model, must create one pb_utils.InferenceResponse for every
        pb_utils.InferenceRequest in `requests`. If there is an error, you can
        set the error argument when creating a pb_utils.InferenceResponse

        Parameters
        ----------
        requests : list
          A list of pb_utils.InferenceRequest

        Returns
        -------
        list
          A list of pb_utils.InferenceResponse. The length of this list must
          be the same as `requests`
        """

        detected_objects_dtype = self.detected_objects_dtype

        responses = []

        # Every Python backend must iterate over every one of the requests
        # and create a pb_utils.InferenceResponse for each of them.
        for request in requests:
            # Get Yolov4 "prob" output as our input
            in_0 = pb_utils.get_input_tensor_by_name(request, "prob")

            output0_data = in_0.as_numpy()
            n_bbox = int(output0_data[0, 0, 0])

            bbox_matrix = output0_data[1: (n_bbox * 7 + 1), 0, 0].reshape(-1, 7)

            detected_objects = []
            if n_bbox:
                labels = set(bbox_matrix[:, 5])
                for label in labels:
                    indices = np.where(
                        (bbox_matrix[:, 5] == label) & (bbox_matrix[:, 6] >= BBOX_CONF_THRESH)
                    )
                    sub_bbox_matrix = bbox_matrix[indices]
                    box_confidences = bbox_matrix[indices, 6]
                    keep_indices = nms(sub_bbox_matrix[:, :4], sub_bbox_matrix[:, 6])
                    if len(keep_indices) < 1:
                        logger.info("No keepers for this bbox")
                        continue
                    sub_bbox_matrix = sub_bbox_matrix[keep_indices]

                    for idx in range(sub_bbox_matrix.shape[0]):
                        x, y, w, h, _, label, score = sub_bbox_matrix[idx, :]
                        object = {"rect": [int(x), int(y), int(w), int(h)], "label": f"{label:0.2}", "score": f"{score:0.3}"}
                        x1 = (x - w / 2)
                        x2 = (x + w / 2)
                        y1 = (y - h / 2)
                        y2 = (y + h / 2)
                        
                        if x1 == x2:
                            continue
                        if y1 == y2:
                            continue
                        object = {"rect": [int(x1), int(y1), int(x2), int(y2)], "class": self.class_names[int(label)], "confidence": f"{score:0.4}"}
                        logger.debug(f"[x1, y1, x2, y2] rect object={object}")
                        if score > CONFIDENCE_THRESHOLD:
                            detected_objects.append(object)

            logger.info(f"detected_objects json={json.dumps(detected_objects)}")
            success = len(detected_objects) > 0
            ret = {"success": success, "gpu_support": True, "objects": detected_objects}

            out_0 = np.array(json.dumps(ret))

            # Create output tensors. You need pb_utils.Tensor
            # objects to create pb_utils.InferenceResponse.
            out_tensor_0 = pb_utils.Tensor("DETECTED_OBJECTS_JSON", out_0.astype(detected_objects_dtype))

            # Create InferenceResponse. 
            inference_response = pb_utils.InferenceResponse(output_tensors=[out_tensor_0])
            responses.append(inference_response)

        # You should return a list of pb_utils.InferenceResponse. Length
        # of this list must match the length of `requests` list.
        return responses

    def finalize(self):
        """`finalize` is called only once when the model is being unloaded.
        Implementing `finalize` function is OPTIONAL. This function allows
        the model to perform any necessary clean ups before exit.
        """
        print('Cleaning up...')

def nms(boxes, box_confidences, iou_threshold=0.5):
    """Apply the Non-Maximum Suppression (NMS) algorithm on the bounding boxes with their
    confidence scores and return an array with the indexes of the bounding boxes we want to
    keep (and display later).

    Parameters
    ----------
    boxes: np.ndarray
        NumPy array containing N bounding-box coordinates that survived filtering,
        with shape (N,4); 4 for x,y,height,width coordinates of the boxes
    box_confidences: np.ndarray
        a Numpy array containing the correspohnding confidences with shape N
    iou_threshold: float
        a threshold between (0, 1)

    Returns
    -------
    selected_index: List[int]
        the selected index to keep
    """
    x_coord = boxes[:, 0]
    y_coord = boxes[:, 1]
    width = boxes[:, 2]
    height = boxes[:, 3]

    areas = width * height
    ordered = box_confidences.argsort()[::-1]

    keep = list()
    while ordered.size > 0:
        # Index of the current element:
        i = ordered[0]
        keep.append(i)
        xx1 = np.maximum(x_coord[i], x_coord[ordered[1:]])
        yy1 = np.maximum(y_coord[i], y_coord[ordered[1:]])
        xx2 = np.minimum(
            x_coord[i] + width[i], x_coord[ordered[1:]] + width[ordered[1:]]
        )
        yy2 = np.minimum(
            y_coord[i] + height[i], y_coord[ordered[1:]] + height[ordered[1:]]
        )

        width1 = np.maximum(0.0, xx2 - xx1 + 1)
        height1 = np.maximum(0.0, yy2 - yy1 + 1)
        intersection = width1 * height1
        union = areas[i] + areas[ordered[1:]] - intersection

        # Compute the Intersection over Union (IoU) score:
        iou = intersection / union

        # The goal of the NMS algorithm is to reduce the number of adjacent bounding-box
        # candidates to a minimum. In this step, we keep only those elements whose overlap
        # with the current bounding box is lower than the threshold:
        indexes = np.where(iou <= iou_threshold)[0]
        ordered = ordered[indexes + 1]

    keep = np.array(keep)
    return keep

        
