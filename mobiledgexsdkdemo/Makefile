PKG = "mobiledgexsdkdemo
GOPATH ?= $(shell go env GOPATH)
GO_PACKAGES := $(shell go list ./... | grep -v /vendor/)
PROGRAM = mobiledgexsdkdemo

.PHONY: build api dep test race msan

default: build docker-build docker-push

docker-build:
	./docker-build.sh

docker-push:
	docker tag mobiledgex/mobiledgexsdkdemo docker.mobiledgex.net/mobiledgex/images/mobiledgexsdkdemo
	docker push docker.mobiledgex.net/mobiledgex/images/mobiledgexsdkdemo

build: api dep ## Build mobiledgexsdkdemo
	go get ./...
	mkdir -p build
	CGO_ENABLED=0 go build  -o build/${PROGRAM} .

dep: api ## Fetch dependencies
	@go get ./...


clean: ## Clean compiled binaries
	@rm -f build/${PROGRAM}

realclean: ## Clean compiled binaries and all generated files
	@rm -f build/${PROGRAM}

test: dep ## Run tests
	@go test -short ${GO_PACKAGES}

race: dep ## Run tests with race detector
	@go test -race -short ${GO_PACKAGES}

msan: dep ## Run tests with memory sanitizer
	@go test -msan -short ${GO_PACKAGES}
