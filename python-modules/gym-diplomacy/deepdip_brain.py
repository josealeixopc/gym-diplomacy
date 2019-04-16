#!/bin/env python

import argparse

import gym
import gym_diplomacy
from gym import wrappers, logger

import math
import numpy as np
import torch

from agent import Model
from agent_config import Config


config = Config()

config.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

#epsilon variables
config.epsilon_start    = 0.01
config.epsilon_final    = 0.01
config.epsilon_decay    = 30000
config.epsilon_by_frame = lambda frame_idx: config.epsilon_final + (config.epsilon_start - config.epsilon_final) * math.exp(-1. * frame_idx / config.epsilon_decay)

#misc agent variables
config.GAMMA = 0.99
config.LR    = 1e-4

#memory
config.TARGET_NET_UPDATE_FREQ = 1000
config.EXP_REPLAY_SIZE        = 10000
config.BATCH_SIZE             = 32

#Learning control variables
config.LEARN_START = 10000
config.MAX_FRAMES  = 1000000
config.UPDATE_FREQ = 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=None)
    parser.add_argument('env_id', nargs='?', default='Diplomacy_Strategy-v0', help='Select the environment to run')
    args = parser.parse_args()

    logger.set_level(logger.INFO)

    outdir = '/tmp/random-agent-results'
    env = gym.make(args.env_id)
    env = wrappers.Monitor(env, directory=outdir, force=True)
    model  = Model(env=env, config=config)
    model.load_w()
    model.load_replay()
    print(model.model)

    frame_idx = 0
    episode_count = 1
    episode_reward = 0
    reward = 0
    done = False

    observation = env.reset()
    for i in range(episode_count):
        while True:
            epsilon = config.epsilon_by_frame(frame_idx)
            action = model.get_action(observation, epsilon)
            
            prev_observation = observation
            observation, reward, done, _ = env.step(action)
            observation = None if done else observation
            frame_idx += 1
            
            model.update(prev_observation, action, reward, observation, frame_idx)
            episode_reward += reward
            
            if done:
                frame_idx = 0
                logger.info("Game/episode {} has ended.".format(i))
                model.finish_nstep()
                model.reset_hx()
                observation = env.reset()
                model.save_reward(episode_reward)
                episode_reward = 0
                break

    model.save_w()
    model.save_replay()
    env.close()

