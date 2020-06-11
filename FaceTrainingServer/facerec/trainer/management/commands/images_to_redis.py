import redis
import signal
import sys
import os
import traceback
import time
import logging
import json
import io
import imageio
import pickle
from redis.exceptions import ConnectionError
from django.core.management.base import BaseCommand
from django.conf import settings
from trainer.apps import myFaceRecognizer

logger = logging.getLogger(__name__)

class Command(BaseCommand):
    help = 'Copies training images into the Redis db. If no "subject" parameter is specified, all subjects are copied.'

    def __init__(self, *args, **kwargs):
        super(Command, self).__init__(*args, **kwargs)
        signal.signal(signal.SIGINT, signal_handler)

    def add_arguments(self, parser):
        # Optional argument
        parser.add_argument('-s', '--subject', type=str, help='The subject name, which is the directory containing images.', )

    def handle(self, *args, **options):
        subject_dir = options['subject']
        myFaceRecognizer.redis_save_subject_images(subject_dir)

def signal_handler(signal, frame):
    sys.exit(0)
