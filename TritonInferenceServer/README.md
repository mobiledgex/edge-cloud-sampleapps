# Triton Inference Server

This repo is used to build upon the NVIDIA [Triton Inference Server](https://github.com/triton-inference-server/server) docker image by adding some models that accept raw binary image data (JPG or PNG), and return the inferred metadata in an easy to parse manner.

## Included Models

## ensemble_dali_yolov4 

YOLOV4 Pre- and Post-Processing Enemble Pipeline

The `ensemble_dali_yolov4` model is an [ensemble pipeline](https://github.com/triton-inference-server/server/blob/master/docs/architecture.md#ensemble-models) made up of the following models:

* yolov4_preprocess - A DALI (Data Loading Library) model. Image is pre-processed with Python code: Resized and normalized as required by the next model in the pipeline. Source code is [dali_src/yolov4_pipeline.py](dali_src/yolov4_pipeline.py) and if updated, must be compiled seperately. See [dali_src/build.sh](dali_src/build.sh).
* yolov4 - TensorRT model pre-trained on the COCO dataset. Object Detection is performed. Output tensors are passed to the next stage for parsing.
* yolov4_parser - Using Python code, YOLOV4 output tensor is parsed to find the image metadata: bounding boxes, class names, and confidence scores. This data is returned as a JSON object. Source code is [models/yolov4_parser/1/model.py](models/yolov4_parser/1/model.py).

Example output:

```
{"success": true, "gpu_support": true, "objects": [{"rect": [0, 59, 79, 92], "class": "car", "confidence": "0.996"}, {"rect": [305, 35, 571, 114], "class": "truck", "confidence": "0.807"}, {"rect": [21, 99, 71, 154], "class": "fire hydrant", "confidence": "0.997"}, {"rect": [1, 176, 412, 568], "class": "bench", "confidence": "0.985"}, {"rect": [155, 156, 584, 585], "class": "chair", "confidence": "0.963"}]}
```

Please note that the coordinates returned are based on images resized to 608x608 as required by YOLOV4. To draw the bounding boxes correctly, the client must calculate a ratio for both X and Y values and use those to determine actual drawing locations. Here is example Python code to do this. 

```python
    ratio_x = image_w / 608
    ratio_y = image_h / 608
    rect = json_object['rect']
    x1 = int(rect[0] * ratio_x)
    y1 = int(rect[1] * ratio_y)
    x2 = int(rect[2] * ratio_x)
    y2 = int(rect[3] * ratio_y)
```

See [clients/rest_client.py](clients/rest_client.py) for a full implementation.

### ensemble_dali_inception
This is the DALI example from https://github.com/triton-inference-server/dali_backend

Example output:

```
[[b'0.816218:560:FOLDING CHAIR']]
```

## Build Docker Container

```bash
TAG=2021-03-25 make docker-build
```

## Run Docker Container

```bash
docker run --net=host --gpus all --name triton_server --rm mobiledgex-samples/triton_server:2021-03-19
```

## Deploy Docker Container to Registry

In the [Makefile](Makefile), edit the ORGNAME and IMAGENAME values as necessary, and then perform the `make`.

```bash
TAG=2021-03-25 make
```
