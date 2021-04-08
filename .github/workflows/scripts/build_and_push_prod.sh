#!/bin/bash
set -e

DOCKER_IMAGE="hsldevcom/opentripplanner"
DOCKER_TAG="otp2-prod"

COMMIT_HASH=$(git rev-parse --short "$GITHUB_SHA")

DOCKER_TAG_LONG=$DOCKER_TAG-$(date +"%Y-%m-%dT%H.%M.%S")-$COMMIT_HASH
DOCKER_IMAGE_TAG=$DOCKER_IMAGE:$DOCKER_TAG
DOCKER_IMAGE_TAG_LONG=$DOCKER_IMAGE:$DOCKER_TAG_LONG

# Build image
echo Building OTP
docker build --tag="$DOCKER_IMAGE:builder" -f Dockerfile.builder .
docker run --rm --entrypoint tar "$DOCKER_IMAGE:builder" -c target|tar x -C ./
docker build --tag="$DOCKER_IMAGE_TAG_LONG" -f Dockerfile .

docker login -u $DOCKER_USER -p $DOCKER_AUTH
echo "Pushing $DOCKER_TAG image"
docker push $DOCKER_IMAGE_TAG_LONG
docker tag $DOCKER_IMAGE_TAG_LONG $DOCKER_IMAGE_TAG
docker push $DOCKER_IMAGE_TAG

echo Build completed
