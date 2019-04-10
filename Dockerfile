FROM ubuntu:18.04

ENV DIP_Q_WORK_DIR="/usr/src/app"

# Install sudo so it runs commands started with "sudo"
RUN apt-get update && apt-get -y install sudo

# Create app directory
RUN mkdir -p ${DIP_Q_WORK_DIR}
WORKDIR ${DIP_Q_WORK_DIR}

# Copy source code to workdir
COPY . .

# Build maven project
RUN chmod +x ./bootstrap.sh
RUN ./bootstrap.sh