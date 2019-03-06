PKG = "openpose"
PROGRAM = openpose

.PHONY: build api dep test race msan

default: build docker-build docker-push

docker-build:
	./install-prereqs.sh
	./docker-build.sh

docker-push:
	docker tag mobiledgex/openpose registry.mobiledgex.net:5000/mobiledgex/openpose
	docker push registry.mobiledgex.net:5000/mobiledgex/openpose

clean:
	rm -rf openpose

