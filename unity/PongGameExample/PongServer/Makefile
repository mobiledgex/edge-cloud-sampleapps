default: docker-build docker-push

docker-build:
	docker build -t mobiledgex/yawsps:${TAG} -f Dockerfile .

docker-push:
	docker tag mobiledgex/yawsps:${TAG} registry.mobiledgex.net:5000/mobiledgex/yawsps:${TAG}
	docker push registry.mobiledgex.net:5000/mobiledgex/yawsps:${TAG}
