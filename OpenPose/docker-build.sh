#!/bin/bash
# this has to be in shell script because we have to run at top level
pwd
docker build -t mobiledgex/openpose -f Dockerfile .

