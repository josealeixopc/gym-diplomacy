#!/bin/sh

# This script can be used to install the Parlance package from 'parlance-code' onto a Pipenv

# IMPORTANT: This script needs Pipenv to be installed
# Parlance needs Python 2

BASEDIR=$(dirname "$0")

pipenv install -e ${BASEDIR}/../parlance-code
chmod +x ${BASEDIR}/init-server.sh