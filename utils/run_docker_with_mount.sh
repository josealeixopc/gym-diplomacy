#!/bin/bash

# This will run the dip-q image, while mounting the /tmp/gym folder on the host's /tmp/gym, meaning that any change to that folder
# will be reflected on the host's /tmp/gym folder as well.

docker run -d \
  -it \
  --rm \
  --name dip-q \
  --mount type=bind,source="/tmp/dip",target="/tmp/dip" \
  dip-q:latest