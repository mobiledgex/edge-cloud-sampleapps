Sample client programs to demonstrate usage of the included models.

## rest_client.py
This Python script makes calls to the REST endpoints, e.g. `/v2/models/ensemble_dali_yolov4/infer` and does not require any Nvidia or Triton specific packages.

Example usage:

```
python rest_client.py -m ensemble_dali_yolov4 -s cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net -f objects_004.jpg --show-responses
```

## dali_grpc_client.py
Based on NVIDIA's [sample DALI client](https://github.com/triton-inference-server/dali_backend/blob/main/client/dali_grpc_client.py), with added support for the ensemble_dali_yolov4 model. This script requires that the [Triton Python client libraries](https://github.com/bytedance/triton-inference-server/blob/master/docs/client_libraries.md#download-using-python-package-installer-pip) be installed.

Example usage:

```
python dali_grpc_client.py --model_name ensemble_dali_yolov4 -u cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net:8001 --img objects_004.jpg --out-dir /tmp
```

## dali_http_client.py
Based on NVIDIA's [sample DALI client](https://github.com/triton-inference-server/dali_backend/blob/main/client/dali_grpc_client.py) with the following modifications:

1. Converted from GRPC to HTTP.
1. Added support for the ensemble_dali_yolov4 model.

This script requires that the [Triton Python client libraries](https://github.com/bytedance/triton-inference-server/blob/master/docs/client_libraries.md#download-using-python-package-installer-pip) be installed.

Example usage:

```
python dali_http_client.py --model_name ensemble_dali_yolov4 -u cv-gpu-cluster.frankfurt-main.tdg.mobiledgex.net:8000 --img objects_004.jpg
```

## ensemble_image_client.py
