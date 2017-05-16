#!/usr/bin/env bash

# remove stop sign if exists
rm cfg/stop

# Docker volumes can only be mounted on absolute files.
# The configuration directory.
CONFIG_DIR=$PWD/cfg
# The directory for logs
LOG_DIR=$PWD/logs
# The destination directory
DESTINATION_DIR=$PWD/destination

docker run -it --rm --name rs_aggregator \
    -v $CONFIG_DIR:/code/cfg \
    -v $LOG_DIR:/code/logs \
    -v $DESTINATION_DIR:/code/destination \
    bhenk/rs-aggregator

