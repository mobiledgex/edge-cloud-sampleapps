### Submodule Requirements
This project depends on a submodule. We link to another repository instead of
copying the code into our own repository because the example code used for
object detection does not include any license information.
To fetch the submodule, please run these commands after cloning the repository:
```bash
cd edge-cloud-sampleapps/
git submodule init
git submodule update
```
Alternatively, use this command to do the initial clone:
```bash
git clone --recurse-submodules https://github.com/mobiledgex/edge-cloud-sampleapps.git
```
### To publish a new Docker image:
Make sure you are logged in to docker.mobiledgex.net, set the TAG and run `make`. Example:
```bash
TAG=2020-3-26 make
```
If you see an error, particularly `ModuleNotFoundError: No module named 'models'`, you probably neglected the `git submodule` commands above.
### To run the server manually:
```bash
virtualenv env -ppython3.7
source env/bin/activate
pip install -r requirements.txt
cd moedx/
python manage.py makemigrations tracker
python manage.py migrate
python manage.py collectstatic --noinput
cd pytorch_objectdetecttrack/config/
wget http://opencv.facetraining.mobiledgex.net/files/yolov3.weights
cd ../..
gunicorn moedx.asgi:application --bind 0.0.0.0:8008 -k uvicorn.workers.UvicornWorker
#or
python manage.py runserver 0:8008
```
### How to install OpenPose on a GPU-enabled server
This assumes CUDA and CUDNN are already installed.
We need cmake version at least 3.12 for CUDA 10

```bash
sudo apt-get purge cmake
wget https://cmake.org/files/v3.14/cmake-3.14.1.tar.gz
tar -xzvf cmake-3.14.1.tar.gz
cd cmake-3.14.1
./bootstrap
make -j4
sudo make install
```
Now download, build, and install OpenPose
```bash
git clone https://github.com/CMU-Perceptual-Computing-Lab/openpose
cd openpose/
sudo bash ./scripts/ubuntu/install_deps.sh
sudo apt-get install libopencv-dev
mkdir build
cd build
# Depending on how cuDNN was installed, you may need to create symbolic links
# for the header and library files needed for the cmake and make process.
sudo ln -s /usr/include/cudnn.h /usr/local/cuda/include/cudnn.h
sudo ln -s /usr/lib/x86_64-linux-gnu/libcudnn.so /usr/local/cuda/lib64/libcudnn.so
/usr/local/bin/cmake -DBUILD_PYTHON=ON ..
make -j`nproc`
sudo make install
```
To run OpenPose we also need a symbolic link to our installation directory,
to get access to some model files that were downloaded as part of the build process:
```bash
sudo ln -s /home/ubuntu/openpose/ /openpose
```

Note that this symbolic link step isn't necessary when running the Docker image.
It is only needed when building OpenPose manually and starting the server manually.
