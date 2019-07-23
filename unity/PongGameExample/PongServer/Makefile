include Makedefs

default: docker-build docker-push

docker-build:
	docker build -t mobiledgex/yawsps:${TAG} -f Dockerfile .

docker-push:
	docker tag mobiledgex/yawsps:${TAG} docker.mobiledgex.net/mobiledgex/images/yawsps:${TAG}
	docker push docker.mobiledgex.net/mobiledgex/images/yawsps:${TAG}
