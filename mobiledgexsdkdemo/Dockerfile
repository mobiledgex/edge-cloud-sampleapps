FROM golang AS BUILD

WORKDIR /go/src/github.com/mobiledgex/edge-cloud-sampleapps/
COPY . .
ENV CGO_ENABLED=0
ENV GOPATH=/go
ENV PATH="/go/bin:${PATH}"
WORKDIR /go/src/github.com/mobiledgex/edge-cloud-sampleapps/mobiledgexsdkdemo
RUN go install -v ./...

FROM alpine:latest
COPY --from=build /go/bin/mobiledgexsdkdemo /usr/local/bin
ENTRYPOINT [ "mobiledgexsdkdemo" ]
CMD []
