#!/usr/bin/env bash

DOCKER_IMAGE="bhenk/rs-aggregator"
CONTAINER_NAME="rs_aggregator"
# Docker volumes can only be mounted on absolute files.
# The configuration directory.
CONFIG_DIR=$PWD/cfg
# The directory for logs
LOG_DIR=$PWD/logs
# Resources and metadata are stored in destination
DESTINATION_DIR=$PWD/destination

# build image if it does not exist
if [[ "$(docker images -q $DOCKER_IMAGE:latest 2> /dev/null)" == "" ]]; then
  echo "Image $DOCKER_IMAGE does not exists. Building it"
  cd ../
  ./docker-build.sh
  cd docker
else
  echo "Image $DOCKER_IMAGE found"
fi

# remove stop sign if exists
rm cfg/stop

echo "Starting docker container $CONTAINER_NAME in detached mode"
docker run -d --rm --name $CONTAINER_NAME \
    -v $CONFIG_DIR:/code/cfg \
    -v $LOG_DIR:/code/logs \
    -v $DESTINATION_DIR:/code/destination \
    $DOCKER_IMAGE

echo "Following logs of $CONTAINER_NAME"
docker logs -f $CONTAINER_NAME


