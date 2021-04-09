include Makedefs
ORGNAME = mobiledgex-samples
IMAGENAME = triton_server

default: docker-build docker-push

submodule-update:
	git submodule init
	git submodule update

docker-build: submodule-update
	docker build -t ${ORGNAME}/${IMAGENAME}:${TAG} .

docker-push:
	docker tag ${ORGNAME}/${IMAGENAME}:${TAG} docker.mobiledgex.net/${ORGNAME}/images/${IMAGENAME}:${TAG}
	docker push docker.mobiledgex.net/${ORGNAME}/images/${IMAGENAME}:${TAG}
