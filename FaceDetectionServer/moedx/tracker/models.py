from django.db import models

class Detected(models.Model):
    x1 = models.IntegerField()
    y1 = models.IntegerField()
    x2 = models.IntegerField()
    y2 = models.IntegerField()

    class Meta:
        ordering = ('x1',)
