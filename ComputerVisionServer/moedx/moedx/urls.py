"""moedx URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/2.1/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path
from django.conf import settings
from tracker import views

urlpatterns = [
    path('admin/', admin.site.urls),
    path('', views.show_index),
    path('detector/detect/', views.detector_detect),
    path('recognizer/add/', views.recognizer_add),
    path('recognizer/train/', views.recognizer_train),
    path('recognizer/predict/', views.recognizer_predict),
    path('openpose/detect/', views.openpose_detect),
    path('object/detect/', views.object_detect),
    path('client/benchmark/', views.client_benchmark),
    path('client/download/', views.client_download),
    path('server/usage/', views.server_usage),
    path('server/capabilities/', views.server_capabilities),
    path('getdata/', views.get_data),
    path('uploaddata/', views.upload_data),
    path('test/', views.test_connection),
]
