# Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
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

from django.db import models

# Create your models here.
class Owner(models.Model):
    id = models.CharField(primary_key=True, max_length=50)
    name = models.CharField(max_length=200)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    def __str__(self):
        return "Owner '%s' (id=%s) created at %s, updated at %s" %(self.name,self.id,self.created_at,self.updated_at)

class Subject(models.Model):
    name = models.CharField(max_length=200, unique=True)
    owner = models.ForeignKey(Owner, on_delete=models.CASCADE)
    in_training = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    def __str__(self):
        return "Subject '%s', owned by '%s' in_training=%r created at %s, updated at %s" %(self.name,self.owner.name,self.in_training, self.created_at,self.updated_at)
