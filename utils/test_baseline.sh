#!/bin/bash

cd ../python-modules/baselines-master
OPENAI_LOG_FORMAT=csv python -m baselines.run --alg=acktr --env=Diplomacy-v0 --save_path=./diplomacy.pkl --num_timesteps=30