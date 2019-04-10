#!/usr/bin/env bash

# The DIP_Q_WORK_DIR environment variable should be the directory that contains the Dip-Q repository
if [[ -z "${DIP_Q_WORK_DIR}" ]]; then
  echo "ERROR: 'WORK_DIR' environment variable has not been defined and it is necessary."
  exit 1 # exit and indicate error
fi

sudo apt-get update

# Install Python and Pip
sudo apt-get install -y python3.7
sudo apt-get install -y python-minimal # Needed for Parlance (only supports 2.x)

# Permanently add alias to set default Python version to 3
sudo apt-get install -y python3-pip
python3.7 -m pip install --upgrade pip
sudo apt-get install -y python-pip
python2 -m pip install --upgrade pip

# Install gym, gym-diplomacy package and necessary Python package dependencies
# DO NOT USE SUDO OUT OF A VM. I only use it here because I cannot use the '--user' flag as root, otherwise it just installs for the root user.
sudo python3.7 -m pip install gym
sudo python3.7 -m pip install -e ${DIP_Q_WORK_DIR}/python-modules/gym-diplomacy

## If you're on your PC use these commands instead
# python3.7 -m pip install gym --user
# python3.7 -m pip install -e /vagrant/python-modules/gym-diplomacy --user

# Install Java

# Next time test whether JRE is needed or not
# sudo apt-get install default-jre
sudo apt-get install -y default-jdk

# Install Maven
MAVEN_VERSION=3.6.0
MAVEN_PACKAGE=apache-maven-$MAVEN_VERSION

cd /opt/
sudo apt-get install -y wget
sudo wget http://www-eu.apache.org/dist/maven/maven-3/3.6.0/binaries/$MAVEN_PACKAGE-bin.tar.gz
sudo tar -xf $MAVEN_PACKAGE-bin.tar.gz
sudo mv $MAVEN_PACKAGE/ apache-maven/
sudo rm $MAVEN_PACKAGE-bin.tar.gz

echo "export PATH=/opt/apache-maven/bin:\$PATH" >> ~/.bashrc

# Install Parlance
# DO NOT USE SUDO OUT OF A VM. I only use it here because I cannot use the '--user' flag as root, otherwise it just installs for the root user.
sudo python2 -m pip install -e ${DIP_Q_WORK_DIR}/python-modules/parlance-code

## If you're on your PC use this command instead
# python2 -m pip install parlance --user

# 'parlance-server' executable needs to be added to path
echo "export PATH=~/.local/bin:\$PATH" >> ~/.bashrc

# Run bashrc to update Path and Aliases
source ~/.bashrc
