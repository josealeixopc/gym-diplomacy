#!/bin/env python

import argparse

import gym
import gym_diplomacy
from gym import wrappers, logger

import math
import numpy as np
import torch
from time import time
from timeit import default_timer as timer
from datetime import timedelta

from agent import Model
from agent_config import Config
from plot import plot_reward


start = timer()

config = Config()

config.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

#epsilon variables
config.epsilon_start    = 1.0
config.epsilon_final    = 0.01
config.epsilon_decay    = 30000
config.epsilon_by_frame = lambda frame_idx: config.epsilon_final + (config.epsilon_start - config.epsilon_final) * math.exp(-1. * frame_idx / config.epsilon_decay)

#misc agent variables
config.GAMMA = 0.99
config.LR    = 1e-4

#memory
config.TARGET_NET_UPDATE_FREQ = 2
config.EXP_REPLAY_SIZE        = 10000
config.BATCH_SIZE             = 32

#Learning control variables
config.LEARN_START = 100
config.MAX_FRAMES  = 1000
config.UPDATE_FREQ = 1


parser = argparse.ArgumentParser(description=None)
parser.add_argument('env_id', nargs='?', default='Diplomacy_Strategy-v0', help='Select the environment to run')
args = parser.parse_args()

logger.set_level(logger.INFO)

log_dir = '/tmp/random-agent-results/'
env_id = args.env_id
env = gym.make(env_id)
env = wrappers.Monitor(env, log_dir + str(time()) + '/')
model = Model(env=env, config=config)
model.load_w()
model.load_replay()
print(model.model)

frame_idx = 0
episode_reward = 0
reward = 0
done = False

observation = env.reset()
for frame_idx in range(1, config.MAX_FRAMES + 1):
    epsilon = config.epsilon_by_frame(frame_idx)
    action = model.get_action(observation, epsilon)
    
    prev_observation = observation
    observation, reward, done, _ = env.step(action)
    observation = None if done else observation
    
    model.update(prev_observation, action, reward, observation, frame_idx)
    episode_reward += reward
    
    if done:
        #logger.info("Finished episode at frame {} with a reward of {}.".format(frame_idx, episode_reward))
        model.finish_nstep()
        model.reset_hx()
        observation = env.reset()
        model.save_reward(episode_reward)
        episode_reward = 0

    if frame_idx % 100 == 0:
        print('')
        print('')
        print('FRAME_IDX: {}'.format(frame_idx))
        print('')
        print('')
        plot_reward(log_dir, env_id, 'DeepDip', config.MAX_FRAMES, bin_size=10, smooth=1, time=timedelta(seconds=int(timer()-start)), save_filename='./results.png', ipynb=False)

model.save_w()
model.save_replay()
env.close()
