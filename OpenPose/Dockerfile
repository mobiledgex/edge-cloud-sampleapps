FROM nvidia/cuda:8.0-cudnn5-devel-ubuntu16.04 

ENV CGO_ENABLED=0
ENV GOPATH=/go
ENV PATH="/go/bin:${PATH}"
COPY . .
COPY requirements.txt /tmp
RUN apt-get update
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y tzdata
RUN apt-get -y install build-essential
RUN apt-get -y install libatlas-base-dev libprotobuf-dev libleveldb-dev libsnappy-dev libhdf5-serial-dev protobuf-compiler
RUN apt-get -y install libboost-all-dev
RUN apt-get -y install libopencv-dev
RUN apt-get --assume-yes install libgflags-dev libgoogle-glog-dev liblmdb-dev
# Python2 libs
RUN apt-get --assume-yes install python-setuptools python-dev build-essential
RUN easy_install pip
RUN pip install --upgrade numpy protobuf opencv-python
# Python3 libs
RUN apt-get --assume-yes install python3-setuptools python3-dev build-essential
RUN apt-get --assume-yes install python3-pip
RUN pip3 install --upgrade numpy protobuf opencv-python
# OpenCV 2.4 -> Added as option
# OpenCL Generic
RUN apt-get --assume-yes install opencl-headers ocl-icd-opencl-dev
RUN apt-get --assume-yes install libviennacl-dev
RUN mkdir openpose/build 

RUN apt-get -y install cmake
RUN cd openpose/build && cmake -DBUILD_PYTHON=ON .. && make -j`nproc`

