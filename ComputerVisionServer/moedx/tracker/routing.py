from django.urls import re_path

from . import consumers

websocket_urlpatterns = [
    re_path('ws/detector/detect', consumers.ImageConsumerFaceDetector),
    re_path('ws/recognizer/predict', consumers.ImageConsumerFaceRecognizer),
    re_path('ws/openpose/detect', consumers.ImageConsumerOpenposeDetector),
    re_path('ws/object/detect', consumers.ImageConsumerObjectDetector),
]
