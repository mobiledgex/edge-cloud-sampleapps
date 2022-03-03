from django.urls import re_path

from . import consumers

websocket_urlpatterns = [
    re_path('ws/detector/detect', consumers.ImageConsumerFaceDetector.as_asgi()),
    re_path('ws/recognizer/predict', consumers.ImageConsumerFaceRecognizer.as_asgi()),
    re_path('ws/openpose/detect', consumers.ImageConsumerOpenposeDetector.as_asgi()),
    re_path('ws/object/detect', consumers.ImageConsumerObjectDetector.as_asgi()),
]
