from django.db import models

def auto_str(cls):
    def __str__(self):
        return '%s(%s)' % (
            type(self).__name__,
            ', '.join('%s=%s' % item for item in vars(self).items())
        )
    cls.__str__ = __str__
    return cls

@auto_str
class CentralizedTraining(models.Model):
    server_name = models.CharField(max_length=250, primary_key=True)
    last_download_timestamp = models.IntegerField(default=0)
    last_check_timestamp = models.IntegerField(default=0)
    update_in_progress = models.BooleanField(default=False)
    update_started_timestamp = models.IntegerField(default=0)

    def dump_timestamps(self):
        return "last_dl_ts=%d last_check_ts=%d" %(self.last_download_timestamp, self.last_check_timestamp)

    def dump_update_flags(self):
        return "update_in_progress=%r update_started_ts=%d" %(self.update_in_progress, self.update_started_timestamp)
