include Makedefs

# We build differently depending on whether this machine has an Nvidia GPU available.
GPU := $(shell command -v nvidia-smi 2> /dev/null)
ifndef GPU
	DOCKERFILE = Dockerfile
else
	DOCKERFILE = Dockerfile_openpose
endif

default: docker-build docker-push

docker-build:
	docker build -t mobiledgex/facedetection:${TAG} -f $(DOCKERFILE) .

docker-push:
	docker tag mobiledgex/facedetection:${TAG} docker.mobiledgex.net/mobiledgex/images/facedetection:${TAG}
	docker push docker.mobiledgex.net/mobiledgex/images/facedetection:${TAG}
