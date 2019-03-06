if [  -d ./openpose ]; then
   echo "openpose directory already exists, run 'make clean' to start over"
   exit 1
fi
git clone https://github.com/CMU-Perceptual-Computing-Lab/openpose
cp Cuda.cmake openpose/cmake/

cd openpose
echo "***** Note: apt-get and make errors can be ignored here *****"
./scripts/ubuntu/install_caffe_and_openpose_JetsonTX2_JetPack3.3.sh || true

echo "\n**** Ignoring build errors -- build to be done in docker **** "
