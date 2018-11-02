include Makedefs

default: docker-build docker-push

docker-build:
	docker build -t mobiledgex/facedetection:${TAG} -f Dockerfile .

docker-push:
	docker tag mobiledgex/facedetection:${TAG} registry.mobiledgex.net:5000/mobiledgex/facedetection:${TAG}
	docker push registry.mobiledgex.net:5000/mobiledgex/facedetection:${TAG}

