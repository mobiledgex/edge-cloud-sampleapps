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
	docker build -t mobiledgex/mobiledgexsdkdemo20:${TAG} -f $(DOCKERFILE) .

docker-push:
	docker tag mobiledgex/mobiledgexsdkdemo20:${TAG} docker.mobiledgex.net/mobiledgex/images/mobiledgexsdkdemo20:${TAG}
	docker push docker.mobiledgex.net/mobiledgex/images/mobiledgexsdkdemo20:${TAG}
