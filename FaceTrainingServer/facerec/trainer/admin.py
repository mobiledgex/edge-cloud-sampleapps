from django.contrib import admin

# Register your models here.
from .models import Owner, Subject
admin.site.register(Owner)
admin.site.register(Subject)
