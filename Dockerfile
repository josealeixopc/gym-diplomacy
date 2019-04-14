FROM ubuntu:18.04

ENV DIP_Q_WORK_DIR="/usr/src/app"

# Set default ENV to "development"
ARG ENV_ARG="development"

# ENV_ARG values:
# "development": when we want to be able to build and debug inside the container
# "build": when we want to be able to build inside the container
# "devploy": when we JUST want to be able to run inside the container

ENV ENV=$ENV_ARG

# Install sudo so it runs commands started with "sudo"
RUN apt-get update && apt-get -y install sudo

# Create app directory
RUN mkdir -p ${DIP_Q_WORK_DIR}
WORKDIR ${DIP_Q_WORK_DIR}

# Copy source code to workdir
COPY . .

# Install everything needed to run/build
RUN chmod +x ./bootstrap.sh
RUN ./bootstrap.sh