# Start with the "min" image, and from the "full" image, copy only what we need.
# See https://github.com/triton-inference-server/server/blob/master/docs/compose.md
FROM nvcr.io/nvidia/tritonserver:21.03-py3 as full
FROM nvcr.io/nvidia/tritonserver:21.03-py3-min
COPY --from=full /opt/tritonserver/bin /opt/tritonserver/bin
COPY --from=full /opt/tritonserver/lib /opt/tritonserver/lib
COPY --from=full /opt/tritonserver/backends/dali /opt/tritonserver/backends/dali
COPY --from=full /opt/tritonserver/backends/python /opt/tritonserver/backends/python
COPY --from=full /opt/tritonserver/backends/tensorflow1 /opt/tritonserver/backends/tensorflow1
COPY --from=full /usr/lib/x86_64-linux-gnu/libre2.so.5 /usr/lib/x86_64-linux-gnu/libre2.so.5
COPY --from=full /usr/lib/x86_64-linux-gnu/libb64.so.0d /usr/lib/x86_64-linux-gnu/libb64.so.0d
COPY --from=full /usr/local/lib/python3.8/dist-packages /usr/local/lib/python3.8/dist-packages

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 python3-pip

# Download the large models. Github frowns upon such large files, so we store them elsewhere.
WORKDIR /models/yolov4/1/
RUN curl http://opencv.facetraining.mobiledgex.net/TritonInferenceServer/models/yolov4/1/model.plan --output model.plan
WORKDIR /models/inception_graphdef/1
RUN curl http://opencv.facetraining.mobiledgex.net/TritonInferenceServer/models/inception_graphdef/1/model.graphdef --output model.graphdef

COPY ./models /models
COPY ./plugins /plugins
# dali_src not needed to function, but good to have available for reference.
COPY ./dali_src /dali_src

# ports for REST, GRPC, and Stats
EXPOSE 8000/tcp
EXPOSE 8001/tcp
EXPOSE 8002/tcp

WORKDIR /opt/tritonserver/bin

ENV LD_LIBRARY_PATH /usr/local/cuda/compat/lib
ENV LIBRARY_PATH /usr/local/cuda/lib64/stubs
ENV PATH /opt/tritonserver/bin:/usr/local/mpi/bin:/usr/local/nvidia/bin:/usr/local/cuda/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/ucx/bin
ENV LD_PRELOAD=/plugins/libmyplugins.so
CMD ["tritonserver", "--model-repository=/models", "--strict-model-config=false", "--grpc-infer-allocation-pool-size=16", "--log-verbose", "1"]
