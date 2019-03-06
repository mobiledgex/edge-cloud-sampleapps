make will generate: registry.mobiledgex.net:5000/mobiledgex/openpose

For systems which may not be reachable from the registry, the container can be exported and imported:
- docker save registry.mobiledgex.net:5000/mobiledgex/openpose -o openpose.image
- scp to remote machine
- docker load openpose.image

To run (openpose entrypoint TBD):
docker run -it --runtime=nvidia --network=host --entrypoint /bin/bash registry.mobiledgex.net:5000/mobiledgex/openpose
