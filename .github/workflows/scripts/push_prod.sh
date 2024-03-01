#!/bin/bash
set -e

COMMIT_HASH=$(git rev-parse --short "$GITHUB_SHA")
ORG=hsldevcom

function imagedeploy {
    DOCKER_IMAGE=$ORG/$1
    DOCKER_TAG=${DOCKER_BASE_TAG:-prod}
    DOCKER_DEV_TAG=${DOCKER_DEV_TAG:-latest}

    DOCKER_TAG_LONG=$DOCKER_TAG-$(date +"%Y-%m-%dT%H.%M.%S")-$COMMIT_HASH
    DOCKER_IMAGE_TAG=$DOCKER_IMAGE:$DOCKER_TAG
    DOCKER_IMAGE_TAG_LONG=$DOCKER_IMAGE:$DOCKER_TAG_LONG
    DOCKER_IMAGE_DEV=$DOCKER_IMAGE:$DOCKER_DEV_TAG

    echo "processing prod release"
    docker pull $DOCKER_IMAGE_DEV
    docker tag $DOCKER_IMAGE_DEV $DOCKER_IMAGE_TAG
    docker tag $DOCKER_IMAGE_DEV $DOCKER_IMAGE_TAG_LONG
    docker push $DOCKER_IMAGE_TAG
    docker push $DOCKER_IMAGE_TAG_LONG
}

docker login -u $DOCKER_USER -p $DOCKER_AUTH

imagedeploy "opentripplanner"

echo Deploy complete
