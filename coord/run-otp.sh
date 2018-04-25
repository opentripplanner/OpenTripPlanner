#!/bin/bash

docker build . -t otp
docker run -it -p 8080:8080 --add-host=gbfs-server:$(ipconfig getifaddr en0) otp
