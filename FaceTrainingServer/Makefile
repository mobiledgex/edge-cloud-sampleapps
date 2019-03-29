include Makedefs

default: docker-build docker-push

docker-build:
	docker build -t mobiledgex/facetraining:${TAG} -f Dockerfile .

docker-push:
	docker tag mobiledgex/facetraining:${TAG} registry.mobiledgex.net:5000/mobiledgex/facetraining:${TAG}
	docker push registry.mobiledgex.net:5000/mobiledgex/facetraining:${TAG}

