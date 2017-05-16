#!/usr/bin/env bash

mode="$1" # ./start.sh d # if you want to run the container in detached mode.

DOCKER_IMAGE="bhenk/rs-aggregator"

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

# Docker volumes can only be mounted on absolute files.
# The configuration directory.
CONFIG_DIR=$PWD/cfg
# The directory for logs
LOG_DIR=$PWD/logs
# Resources and metadata are stored in destination
DESTINATION_DIR=$PWD/destination

if [ "$mode" == "d" ]; then
    echo "Starting docker container in detached mode"
    docker run -d --rm --name rs_aggregator \
        -v $CONFIG_DIR:/code/cfg \
        -v $LOG_DIR:/code/logs \
        -v $DESTINATION_DIR:/code/destination \
        $DOCKER_IMAGE
else
    docker run -it --rm --name rs_aggregator \
        -v $CONFIG_DIR:/code/cfg \
        -v $LOG_DIR:/code/logs \
        -v $DESTINATION_DIR:/code/destination \
        $DOCKER_IMAGE
fi
