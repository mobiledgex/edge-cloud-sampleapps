# Copyright 2021 MobiledgeX, Inc. All rights and licenses reserved.
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

import argparse, os, sys
import numpy as np
import tritonclient.http
from PIL import Image
import json

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-v', '--verbose', action="store_true", required=False, default=False,
                        help='Enable verbose output')
    parser.add_argument('-u', '--url', type=str, required=False, default='localhost:8000',
                        help='Inference server URL. Default is localhost:8000.')
    parser.add_argument('--batch_size', type=int, required=False, default=1,
                        help='Batch size')
    parser.add_argument('--n_iter', type=int, required=False, default=-1,
                        help='Number of iterations , with `batch_size` size')
    parser.add_argument('--model_name', type=str, required=False, default="dali_backend",
                        help='Model name')
    img_group = parser.add_mutually_exclusive_group()
    img_group.add_argument('--img', type=str, required=False, default=None,
                           help='Run a img dali pipeline. Arg: path to the image.')
    img_group.add_argument('--img_dir', type=str, required=False, default=None,
                           help='Directory, with images that will be broken down into batches an infered. The directory must contain images only')
    return parser.parse_args()


def load_image(img_path: str):
    """
    Loads image as an encoded array of bytes.
    This is a typical approach you want to use in DALI backend
    """
    with open(img_path, "rb") as f:
        img = f.read()
        return np.array(list(img)).astype(np.uint8)


def load_images(dir_path: str):
    """
    Loads all files in given dir_path. Treats them as images
    """
    images = []

    # Traverses directory for files (not dirs) and returns full paths to them
    if os.path.isfile(dir_path):
        img_paths = [dir_path]
    else:
        path_generator = (os.path.join(dir_path, f) for f in os.listdir(dir_path) if
                          os.path.isfile(os.path.join(dir_path, f)))
        img_paths = [dir_path] if os.path.isfile(dir_path) else list(path_generator)

    for img in img_paths:
        images.append(load_image(img))
    return images


def array_from_list(arrays):
    """
    Convert list of ndarrays to single ndarray with ndims+=1
    """
    lengths = list(map(lambda x, arr=arrays: arr[x].shape[0], [x for x in range(len(arrays))]))
    max_len = max(lengths)
    arrays = list(map(lambda arr, ml=max_len: np.pad(arr, ((0, ml - arr.shape[0]))), arrays))
    for arr in arrays:
        assert arr.shape == arrays[0].shape, "Arrays must have the same shape"
    return np.stack(arrays)


def batcher(dataset, batch_size, n_iterations=-1):
    """
    Generator, that splits dataset into batches with given batch size
    """
    assert len(dataset) % batch_size == 0
    n_batches = len(dataset) // batch_size
    iter_idx = 0
    for i in range(n_batches):
        if 0 < n_iterations <= iter_idx:
            raise StopIteration
        iter_idx += 1
        yield dataset[i * batch_size:(i + 1) * batch_size]


def save_byte_image(bytes, size_wh=(224, 224), name_suffix=0):
    """
    Utility function, that can be used to save byte array as an image
    """
    im = Image.frombytes("RGB", size_wh, bytes, "raw")
    im.save("result_img_" + str(name_suffix) + ".jpg")


def main():
    FLAGS = parse_args()
    try:
        triton_client = tritonclient.http.InferenceServerClient(url=FLAGS.url, verbose=FLAGS.verbose)
    except Exception as e:
        print("Connection failed: " + str(e))
        sys.exit()

    model_name = FLAGS.model_name
    model_version = -1

    print("Loading images")

    image_data = load_images(FLAGS.img_dir if FLAGS.img_dir is not None else FLAGS.img)
    image_data = array_from_list(image_data)

    print("Images loaded, inferring")

    # Infer
    inputs = []
    outputs = []
    if model_name == "ensemble_dali_yolov4":
        input_name = "IMAGE"
        output_name = "OBJECTS_JSON"
    elif model_name == "ensemble_dali_inception":
        input_name = "INPUT"
        output_name = "OUTPUT"
    else:
        print(f"Unknown model: {model_name}")
        sys.exit()

    input_shape = list(image_data.shape)
    input_shape[0] = FLAGS.batch_size
    inputs.append(tritonclient.http.InferInput(input_name, input_shape, "UINT8"))
    if model_name == "ensemble_dali_inception":
        outputs.append(
            tritonclient.http.InferRequestedOutput(output_name,
                                            class_count=1))
    else:
        outputs.append(
            tritonclient.http.InferRequestedOutput(output_name))

    for batch in batcher(image_data, FLAGS.batch_size):
        # Initialize the data
        inputs[0].set_data_from_numpy(batch)

        # Test with outputs
        results = triton_client.infer(model_name=model_name,
                                      inputs=inputs,
                                      outputs=outputs)

        # Get the output arrays from the results
        output0_data = results.as_numpy(output_name)
        print(f"output0_data={output0_data}")
        
    print('PASS: infer')

if __name__ == '__main__':
    main()
