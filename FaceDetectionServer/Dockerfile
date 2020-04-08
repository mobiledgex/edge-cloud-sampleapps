FROM python:3.6.6-onbuild
COPY requirements.txt /tmp
WORKDIR /tmp
RUN pip install -r requirements.txt
# Download weights file required for object detection
WORKDIR /usr/src/app/moedx/pytorch_objectdetecttrack/config
RUN wget https://pjreddie.com/media/files/yolov3.weights
WORKDIR /usr/src/app/moedx
# Initialize the database.
RUN python manage.py makemigrations tracker
RUN python manage.py migrate
# port for REST
EXPOSE 8008/tcp
# port for persistent TCP server
EXPOSE 8011/tcp
ENV PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# Fix for "click" Python library, a uvicorn dependency.
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

CMD ["uvicorn","moedx.asgi:application","--host", "0.0.0.0","--port","8008","--workers","2"]
