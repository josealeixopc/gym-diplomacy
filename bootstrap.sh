#!/usr/bin/env bash

# This is a bootstrap script for Ubuntu 18.04 based machines

# The DIP_Q_WORK_DIR environment variable should be the directory that contains the Dip-Q repository
if [[ -z "${DIP_Q_WORK_DIR}" ]]; then
  echo "ERROR: 'DIP_Q_WORK_DIR' environment variable has not been defined and it is necessary."
  exit 1 # exit and indicate error
fi

# Change to root dir
cd ${DIP_Q_WORK_DIR}

sudo apt-get update


# Install Maven only if Env is development or build (handy when we want to dev on a VM or use Docker to build the artifacts)
if [[ "${ENV}" = "development" ] || [ "${ENV}" = "build" ]]; then
  # JDK is needed to compile Java (it also installs JRE) 
  sudo apt-get install -y default-jdk 

  MAVEN_VERSION=3.6.0
  MAVEN_PACKAGE=apache-maven-$MAVEN_VERSION

  cd /opt/
  sudo apt-get install -y wget
  sudo wget http://www-eu.apache.org/dist/maven/maven-3/3.6.0/binaries/$MAVEN_PACKAGE-bin.tar.gz
  sudo tar -xf $MAVEN_PACKAGE-bin.tar.gz
  sudo mv $MAVEN_PACKAGE/ apache-maven/
  sudo rm $MAVEN_PACKAGE-bin.tar.gz

  echo "export PATH=/opt/apache-maven/bin:\$PATH" >> ~/.bashrc

  # Run bashrc to update Path
  source ~/.bashrc
else
  # JRE is sufficient to execute .jar files
  sudo apt-get install -y default-jre 
fi

# Install necessary Python versions and Pipenv
PYTHON_2_VERSION="2.7" # For Parlance
PYTHON_3_VERSION="3.7"  # For everything else

# Install Python, Pip and Pipenv
sudo apt-get install -y python${PYTHON_2_VERSION} python-pip
sudo apt-get install -y python${PYTHON_3_VERSION} python3-pip

python${PYTHON_3_VERSION} -m pip install -U pip
sudo -H pip3 install pipenv -U

# Install Parlance
cd ${DIP_Q_WORK_DIR}/python-modules/parlance-server

# Make run-server script executable to run Parlance Server
sudo chmod +x init-server.sh

# Skip lock because we don't need it outside a development environment
pipenv install --skip-lock

# Install what we need for our agent
cd ${DIP_Q_WORK_DIR}/dip-q-brain

# Here you actually NEED to skip lock, because one of the 'setup.py' has an outdated
# dependencies tree and will crash the normal install
pipenv install --skip-lock