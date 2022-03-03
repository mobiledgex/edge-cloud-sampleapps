FROM python:3.7-slim AS compile-image
RUN apt-get update && apt-get install -y --no-install-recommends build-essential
# Use a virtualenv for all of our Python requirements.
RUN python -m venv /opt/venv
# Make sure we use the virtualenv:
ENV PATH="/opt/venv/bin:$PATH"
RUN pip3 install wheel
# Dependencies for our Django app.
COPY requirements.txt .
RUN pip install -r requirements.txt

FROM python:3.7-slim AS build-image
COPY --from=compile-image /opt/venv /opt/venv
RUN apt-get update && apt-get install -y libglib2.0-0 libsm6 libxrender1 libxext6 libgl1-mesa-glx wget iputils-ping
# Make sure we use the virtualenv:
ENV PATH="/opt/venv/bin:$PATH"

# Download weights file required for object detection
WORKDIR /usr/src/app/moedx/pytorch_objectdetecttrack/config
RUN wget http://opencv.facetraining.mobiledgex.net/files/yolov3.weights
WORKDIR /usr/src/app/moedx
COPY . /usr/src/app
# Initialize the database.
RUN python manage.py makemigrations tracker
RUN python manage.py migrate

RUN python manage.py collectstatic --noinput

# port for REST
EXPOSE 8008/tcp

# Fix for "click" Python library, a uvicorn dependency.
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

CMD ["gunicorn","moedx.asgi:application","--bind", "0.0.0.0:8008","-k","uvicorn.workers.UvicornWorker","--workers","1"]
