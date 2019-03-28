#!/usr/bin/env bash

sudo apt-get update

# Install Python and Pip
sudo apt-get install -y python3.7
sudo apt-get install -y python-minimal # Needed for Parlance (only supports 2.x)

# Permanently add alias to set default Python version to 3
echo "alias python=python3.7" >> ~/.bash_aliases
sudo apt-get install -y python3-pip
python -m pip install --upgrade pip
sudo apt-get install -y python-pip
python2 -m pip install --upgrade pip

# Install gym, gym-diplomacy package and necessary Python package dependencies
python -m pip install gym
python -m pip install -e /vagrant/python-modules/gym-diplomacy --user

# Install Java

# Next time test whether JRE is needed or not
# sudo apt-get install default-jre
sudo apt-get install -y default-jdk

# Install Maven
MAVEN_VERSION=3.6.0
MAVEN_PACKAGE=apache-maven-$MAVEN_VERSION

cd /opt/
sudo wget http://www-eu.apache.org/dist/maven/maven-3/3.6.0/binaries/$MAVEN_PACKAGE-bin.tar.gz
sudo tar -xf $MAVEN_PACKAGE-bin.tar.gz
sudo mv $MAVEN_PACKAGE/ apache-maven/
sudo rm $MAVEN_PACKAGE-bin.tar.gz

echo "export PATH=/opt/apache-maven/bin:\$PATH" >> ~/.bashrc

# Install Parlance
pip2 install parlance --user

# Run bashrc to update Path and Aliases
source ~/.bashrc
