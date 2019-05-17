#!/bin/bash

# This helps Docker image build faster, because TF and Gym will never change and can be kept on cache
pipenv install tensorflow~=1.13 gym~=0.12 stable-baselines~=2.5.1 --skip-lock