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
