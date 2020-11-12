include Makedefs
ORGNAME = mobiledgex-samples

# To build the version with GPU support, define GPU as "true", e.g.:
# TAG=20201110 GPU=true make docker-build
ifeq ($(GPU), true)
	DOCKERFILE = Dockerfile_gpu
	IMAGENAME = computervision-gpu
else
	DOCKERFILE = Dockerfile
	IMAGENAME = computervision
endif

default: docker-build docker-push

submodule-update:
	git submodule init
	git submodule update

docker-build: submodule-update
	docker build -t ${ORGNAME}/${IMAGENAME}:${TAG} -f $(DOCKERFILE) .

docker-push:
	docker tag ${ORGNAME}/${IMAGENAME}:${TAG} docker.mobiledgex.net/${ORGNAME}/images/${IMAGENAME}:${TAG}
	docker push docker.mobiledgex.net/${ORGNAME}/images/${IMAGENAME}:${TAG}
