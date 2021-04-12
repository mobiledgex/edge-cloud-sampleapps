include Makedefs
ORGNAME = mobiledgex-samples
IMAGENAME = triton_server

default: docker-build docker-push

docker-build:
	docker build -t ${ORGNAME}/${IMAGENAME}:${TAG} .

docker-push:
	docker tag ${ORGNAME}/${IMAGENAME}:${TAG} docker.mobiledgex.net/${ORGNAME}/images/${IMAGENAME}:${TAG}
	docker push docker.mobiledgex.net/${ORGNAME}/images/${IMAGENAME}:${TAG}
