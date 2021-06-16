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

import math

class RunningStats:

    def __init__(self):
        self.n = 0
        self.current = 0
        self.old_mean = 0
        self.new_mean = 0
        self.old_sum_squares = 0
        self.new_sum_squares = 0

    def clear(self):
        self.n = 0

    def push(self, x):
        self.current = x
        self.n += 1

        if self.n == 1:
            self.old_mean = self.new_mean = x
            self.old_sum_squares = 0
        else:
            self.new_mean = self.old_mean + (x - self.old_mean) / self.n
            self.new_sum_squares = self.old_sum_squares + (x - self.old_mean) * (x - self.new_mean)

            self.old_mean = self.new_mean
            self.old_sum_squares = self.new_sum_squares

    def mean(self):
        return self.new_mean if self.n else 0.0

    def variance(self):
        return self.new_sum_squares / (self.n - 1) if self.n > 1 else 0.0

    def stddev(self):
        return math.sqrt(self.variance())
