FROM registry.mobiledgex.net:5000/mobiledgex/build AS build

WORKDIR /go/src/github.com/mobiledgex/edge-cloud-sampleapps/
COPY . .
ENV CGO_ENABLED=0
ENV GOPATH=/go
ENV PATH="/go/bin:${PATH}"
WORKDIR /go/src/github.com/mobiledgex/edge-cloud-sampleapps/FaceDetectionServer/facerepo/
RUN go get -d -v ./...
RUN go install -v ./...
RUN mkdir /data

FROM alpine:latest
COPY --from=build /go/bin/facerepo /usr/local/bin
ENTRYPOINT [ "facerepo" ]
CMD []
