# This Dockerfile creates a Docker container with every dependency needed to/debug run the project.
# The Java artifacts should be generated BEFORE creating this image when using "deploy" mode, because JDK is not installed in this mode. 

FROM ubuntu:18.04

ENV DIP_Q_WORK_DIR="/usr/src/app"

# Set default ENV to "development"
ARG ENV_ARG="development"

# ENV_ARG values:
# "development": when we want to be able to build and debug inside the container
# "build": when we want to be able to build inside the container
# "deploy": when we JUST want to be able to run inside the container

ENV ENV=$ENV_ARG

# Install sudo so it runs commands started with "sudo"
RUN apt-get update && apt-get -y install sudo

# Create app directory
RUN mkdir -p ${DIP_Q_WORK_DIR}
WORKDIR ${DIP_Q_WORK_DIR}
 
COPY bootstrap.sh .

# Install Python (and Maven)
RUN chmod +x ./bootstrap.sh
RUN ./bootstrap.sh

# Install heavy Python packages for dip-q-brain
RUN mkdir -p ${DIP_Q_WORK_DIR}/dip-q-brain
COPY python-modules/dip-q-brain/install-essential.sh ${DIP_Q_WORK_DIR}/python-modules/dip-q-brain

WORKDIR ${DIP_Q_WORK_DIR}/python-modules/dip-q-brain
RUN chmod +x ./install-essential.sh
RUN ./install-essential.sh

# Install heavy Python packages for deepdip
RUN mkdir -p ${DIP_Q_WORK_DIR}/deepdip
COPY python-modules/deepdip/install-essential.sh ${DIP_Q_WORK_DIR}/python-modules/deepdip

WORKDIR ${DIP_Q_WORK_DIR}/python-modules/deepdip
RUN chmod +x ./install-essential.sh
RUN ./install-essential.sh

# Note: these packages are installed beforehand because this way the.
# image won't take much time to build when changes are made to the code.

# Install Python packages afterwards
WORKDIR ${DIP_Q_WORK_DIR}
COPY . .
RUN chmod +x ./utils/install-dependencies.sh
RUN ./utils/install-dependencies.sh