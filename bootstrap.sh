#!/bin/bash

# This is a bootstrap script for Ubuntu 18.04 based machines

sudo apt-get update

# Install Maven only if Env is development or build (handy when we want to dev on a VM or use Docker to build the artifacts)
if [[ "${ENV}" = "development" ]] || [[ "${ENV}" = "build" ]]; then
  # JDK is needed to compile Java (it also installs JRE) 
  sudo apt-get install -y default-jdk 

  MAVEN_VERSION=3.6.0
  MAVEN_PACKAGE=apache-maven-$MAVEN_VERSION

  cd /opt/ || exit 1
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