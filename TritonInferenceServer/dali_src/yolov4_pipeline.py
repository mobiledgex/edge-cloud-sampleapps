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

import nvidia.dali as dali
import nvidia.dali.types as types
import argparse

def main(filename):
    pipe = dali.pipeline.Pipeline(batch_size=3, num_threads=1, device_id=0)
    shapes = dali.ops.Shapes(device = "cpu")
    with pipe:
        images = dali.fn.external_source(device="cpu", name="RAW_IMAGE")
        images = dali.fn.image_decoder(images, device="cpu", output_type=types.BGR)
        shapes = shapes(images)
        images = dali.fn.resize(images, resize_x=608, resize_y=608)
        images = dali.fn.crop_mirror_normalize(images,
                                               dtype=types.FLOAT,
                                               output_layout="CHW",
                                               crop=(608, 608))
        images /= 255.0
        pipe.set_outputs(images, shapes)
        pipe.serialize(filename=filename)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Serialize pipeline and save it to file")
    parser.add_argument('file_path', type=str, help='Path, where to save serialized pipeline')
    args = parser.parse_args()
    main(args.file_path)
