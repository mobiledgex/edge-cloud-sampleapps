# Load Testing
This directory contains a very simple load test that can be deployed as a docker container. The `dockerfile` shows the requirements, but at a high level you require the [vegeta](https://github.com/tsenart/vegeta) executable (the provided version is for amd64). 

## Full Requirements
- `jq` to handle the processing and creation of the json payload.
- `vegeta` to run the tests. You will need to download this binary and place it in this directory for it to be copied into the docker image. Releases can be found in the github project repository [tsenart/vegata](https://github.com/tsenart/vegeta) 
- `env` if you choose to not use the hardcoded values of 30 seconds at 30 connections/second.
- `run.sh` to serve as the entrypoint.

## How to Run

### Build the docker image
```
docker build -t loadtest .
Sending build context to Docker daemon  8.694MB
Step 1/8 : FROM ubuntu:latest
 ---> 1d622ef86b13
Step 2/8 : RUN apt update
 ---> Using cache
 ---> 159214b0992d
Step 3/8 : RUN apt -y install jq ca-certificates
 ---> Using cache
 ---> bed64e1fc256
Step 4/8 : COPY vegeta /usr/local/bin/vegeta
 ---> Using cache
 ---> 16c04779928a
Step 5/8 : COPY run.sh /usr/local/bin/run.sh
 ---> Using cache
 ---> d297b14ff017
Step 6/8 : ENV THERATE=30
 ---> Using cache
 ---> cf88aa198229
Step 7/8 : ENV THEDURATION=30s
 ---> Using cache
 ---> d0bad762f288
Step 8/8 : ENTRYPOINT ["/usr/local/bin/run.sh"]
 ---> Using cache
 ---> c0a310d63490
Successfully built c0a310d63490
Successfully tagged loadtest:latest
```

### Run the docker image

```
docker run --rm --env-file ./env loadtest:latest
POST TEST: Rate is 50/s DURATION is 60s
Requests      [total, rate, throughput]         3000, 50.02, 30.89
Duration      [total, attack, wait]             1m24s, 59.98s, 24.425s
Latencies     [min, mean, 50, 90, 95, 99, max]  2.835ms, 12.195s, 10.734s, 27.025s, 30.002s, 30.003s, 30.023s
Bytes In      [total, mean]                     44319, 14.77
Bytes Out     [total, mean]                     27570, 9.19
Success       [ratio]                           86.90%
Status Codes  [code:count]                      0:393  201:2607
Error Set:
Post "https://hamburg-main.tdg.mobiledgex.net:10001/posts": EOF
Post "https://hamburg-main.tdg.mobiledgex.net:10001/posts": net/http: request canceled (Client.Timeout exceeded while awaiting headers)
Post "https://hamburg-main.tdg.mobiledgex.net:10001/posts": context deadline exceeded (Client.Timeout exceeded while awaiting headers)
Post "https://hamburg-main.tdg.mobiledgex.net:10001/posts": context deadline exceeded
GET TEST: Rate is 50/s DURATION is 60s
Requests      [total, rate, throughput]         3000, 50.02, 49.86
Duration      [total, attack, wait]             1m0s, 59.98s, 170.875ms
Latencies     [min, mean, 50, 90, 95, 99, max]  167.057ms, 176.698ms, 169.769ms, 171.22ms, 172.171ms, 491.49ms, 1.029s
Bytes In      [total, mean]                     71605, 23.87
Bytes Out     [total, mean]                     0, 0.00
Success       [ratio]                           99.97%
Status Codes  [code:count]                      200:2999  404:1
Error Set:
404 Not Found
```

## Things to Change
- The `env` file contains two variables, `THERATE` and `THEDURATION`; these can be adjusted to change the duration of the test. Note that affects both POSTS and GETS.
    - Yes, you can use `-e` or `--env` to pass variables on the CLI. I just like the cleaner way of using a file.
- The `run.sh` script is ludicrously simple; feel free to adjust that to use some of the more advanced features of `vegeta`.
- The URL is hardcoded; for this I apologize but I wasn't in the mood to muck w/ shell quoting today.
- If you run from different locations (ie, a multi-client load test) you may want to adjsut the index that is used in the `run.sh` script a bit to prevent collisions.
