# Copyright 2020 MobiledgeX, Inc. All rights and licenses reserved.
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

import redis
import signal
import sys
import traceback
import time
import logging
import json
import channels.layers
from asgiref.sync import async_to_sync
from channels.db import database_sync_to_async
from redis.exceptions import ConnectionError
from django.core.management.base import BaseCommand
from django.conf import settings
from tracker.models import CentralizedTraining
from tracker.apps import myFaceRecognizer

logger = logging.getLogger(__name__)

TRAINING_DATA_HOSTNAME = 'opencv.facetraining.mobiledgex.net'
PUB_CHANNEL_ADDED = 'training.*'

class Command(BaseCommand):
    help = u'Opens a connection to Redis and listens for training.* messages, and updates training data accordingly.'

    def __init__(self, *args, **kwargs):
        super(Command, self).__init__(*args, **kwargs)
        signal.signal(signal.SIGINT, signal_handler)
        self.training_data_hostname = TRAINING_DATA_HOSTNAME
        self.channel = PUB_CHANNEL_ADDED

    def handle(self, *args, **options):
        logger.info('Initializing redis listener...[subscribing channel: "%s"]' % self.channel)
        self.redis = None
        self.pubsub = None
        self.loop()

    def connect(self):
        while True:
            logger.debug('Trying to connect to redis ...')
            try:
                self.redis = redis.StrictRedis(host=self.training_data_hostname, password='S@ndhi11')
                self.redis.ping()
            except (ConnectionError, ConnectionRefusedError):
                time.sleep(1)
            else:
                break
        logger.info('Connected to redis on %s.' %self.training_data_hostname)
        self.pubsub = self.redis.pubsub()
        self.pubsub.psubscribe(self.channel)
        logger.info('Pattern subscribed to channel %s' %self.channel)

    def loop(self):
        self.connect()
        while True:
            try:
                for item in self.pubsub.listen():
                    logger.info("TODO: Remove. item: %s" %item)
                    if item['type'] == 'pmessage':
                        logger.info("item: %s" %item)
                        self.handle_message(item)
                        # data = json.loads(item['data'].decode('utf-8'))
                        # self.broadcast_message(data)
            except ConnectionError:
                logger.error('Lost connection to redis.')
                self.connect()

    def handle_message(self, message):
        # {'type': 'pmessage', 'pattern': b'training.*', 'channel': b'training.removed', 'data': b'Mary'}
        if message['channel'] == b'training.added':
            index = message['data'].decode('utf8')
            if index == "":
                subject = "ALL"
            else:
                subject_images_key = 'subject_images:%s' % index
                subject_name_key = 'subject_name:%s' % index
                subject = self.redis.get(subject_name_key)
            logger.info("Training images added for %s" %subject)

        elif message['channel'] == b'training.removed':
            subject = message['data'].decode('utf8')
            logger.info("Training images removed for %s" %subject)

        ct, created = CentralizedTraining.objects.get_or_create(
            server_name = self.training_data_hostname)
        logger.info("ct, created = %s, %s" %(ct, created))
        self.update_training_data()

    def update_training_data(self):
        # TODO: Pull new images from Redis


        myFaceRecognizer.update_training_data_from_redis(self.redis)
        # myFaceRecognizer.read_trained_data()

    def broadcast_message(self, data):
        logger.info('Broadcast message: timestamp=%d, command="%s", status_code=%d' % (
            data['timestamp'],
            data['command'],
            data['status_code'],
        ))
        channel_layer = channels.layers.get_channel_layer()
        async_to_sync(channel_layer.group_send)(
            settings.CHANNELS_DEVICE_BROADCAST_GROUP, {
                "type": data['command'],
                "params": data['params'],
            })


def signal_handler(signal, frame):
    sys.exit(0)
