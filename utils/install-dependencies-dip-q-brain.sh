#!/bin/bash

# The DIP_Q_WORK_DIR environment variable should be the directory that contains the Dip-Q repository
if [[ -z "${DIP_Q_WORK_DIR}" ]]; then
  echo "ERROR: 'DIP_Q_WORK_DIR' environment variable has not been defined and it is necessary."
  exit 1 # exit and indicate error
fi

# Change to root dir
cd "${DIP_Q_WORK_DIR}" || exit 2

# Install Parlance
cd "${DIP_Q_WORK_DIR}"/python-modules/parlance-server || exit 3

# Make run-server script executable to run Parlance Server
sudo chmod +x init-server.sh

# Skip lock because we don't need it outside a development environment
pipenv install --skip-lock

# Install what we need for our negotiation agent
cd "${DIP_Q_WORK_DIR}"/agents/dip-q-brain || exit 4

# Here you actually NEED to skip lock, because one of the 'setup.py' has an outdated
# dependencies tree and will crash the normal install
pipenv install --skip-lock