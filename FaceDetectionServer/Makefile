include Makedefs

default: docker-build docker-push

docker-build:
	docker build -t mobiledgex/facedetection:${TAG} -f Dockerfile .

docker-push:
	docker tag mobiledgex/facedetection:${TAG} docker.mobiledgex.net/mobiledgex/images/facedetection:${TAG}
	docker push docker.mobiledgex.net/mobiledgex/images/facedetection:${TAG}

