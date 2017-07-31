#!/bin/bash
set +e

# This is run at ci, created an image that contains all the tools needed in
# databuild
#
# Set these environment variables
#DOCKER_USER // dockerhub credentials
#DOCKER_AUTH

ORG=${ORG:-hsldevcom}
DOCKER_TAG=${TRAVIS_BUILD_ID:-latest}
DOCKER_IMAGE=$ORG/opentripplanner
DOCKER_IMAGE_LATEST=$ORG/opentripplanner:latest

echo Building otp: $DOCKER_IMAGE

# Build image
docker build --tag="$ORG/$DOCKER_IMAGE:builder" -f Dockerfile.builder .
mkdir export
docker run --rm --entrypoint tar "$ORG/$DOCKER_IMAGE:builder" -c target|tar x -C ./

docker build --tag="$ORG/$DOCKER_IMAGE:$DOCKER_TAG" -f Dockerfile .

if [ "${TRAVIS_PULL_REQUEST}" == "false" ]; then
  docker login -u $DOCKER_USER -p $DOCKER_AUTH
  docker push $DOCKER_IMAGE:$DOCKER_TAG
  docker tag $DOCKER_IMAGE $DOCKER_IMAGE_LATEST
  docker push $DOCKER_IMAGE_LATEST
fi

echo Build completed

