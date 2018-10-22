# We Use an official Python runtime as a parent image
FROM python:3.6

ENV PYTHONUNBUFFERED 1

RUN mkdir /MobiledgeX-Backend

# Set the working directory to /music_service
WORKDIR /MobiledgeX-Backend/

ADD . /MobiledgeX-Backend/

EXPOSE 8000

# Install any needed packages specified in requirements.txt
RUN pip install -r requirements.txt
WORKDIR moedx/
CMD ["gunicorn", "moedx.wsgi:application", "--bind", "0.0.0.0:8000", "--certfile=server.crt", "--keyfile=server.key", "--workers=10"]