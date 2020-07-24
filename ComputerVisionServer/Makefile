include Makedefs

# We build differently depending on whether this machine has an Nvidia GPU available.
GPU := $(shell command -v nvidia-smi 2> /dev/null)
ifndef GPU
	DOCKERFILE = Dockerfile
	IMAGENAME = computervision
else
	DOCKERFILE = Dockerfile_openpose
	IMAGENAME = computervision-gpu
endif

default: docker-build docker-push

submodule-update:
	git submodule init
	git submodule update

docker-build: submodule-update
	docker build -t mobiledgex/${IMAGENAME}:${TAG} -f $(DOCKERFILE) .

docker-push:
	docker tag mobiledgex/${IMAGENAME}:${TAG} docker.mobiledgex.net/mobiledgex/images/${IMAGENAME}:${TAG}
	docker push docker.mobiledgex.net/mobiledgex/images/${IMAGENAME}:${TAG}
