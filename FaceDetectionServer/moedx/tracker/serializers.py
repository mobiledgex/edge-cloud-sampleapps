from rest_framework import serializers
from tracker.models import Detected

class DetectedSerializer(serializers.ModelSerializer):
    class Meta:
        model = Detected
        fields = ('x1', 'y1', 'x2', 'y2')
