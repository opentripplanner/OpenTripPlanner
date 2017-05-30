#!/bin/bash
ORG=${ORG:-hsldevcom}
DOCKER_IMAGE=opentripplanner

# Set these environment variables
#DOCKER_TAG=
#DOCKER_EMAIL=
#DOCKER_USER=
#DOCKER_AUTH=

# Build image
docker build --tag="$ORG/$DOCKER_IMAGE:builder" -f Dockerfile.builder .
mkdir export
docker run --rm --entrypoint tar "$ORG/$DOCKER_IMAGE:builder" -c target|tar x -C ./

docker build --tag="$ORG/$DOCKER_IMAGE:$DOCKER_TAG" -f Dockerfile .
docker login -e $DOCKER_EMAIL -u $DOCKER_USER -p $DOCKER_AUTH
docker push $ORG/$DOCKER_IMAGE:$DOCKER_TAG
docker tag $ORG/$DOCKER_IMAGE:$DOCKER_TAG $ORG/$DOCKER_IMAGE:latest
docker push $ORG/$DOCKER_IMAGE:latest
