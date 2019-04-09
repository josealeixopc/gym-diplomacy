#!/bin/env python

import argparse

import gym
import gym_diplomacy
from gym import wrappers, logger


class RandomAgent(object):
    """The world's simplest agent!"""
    def __init__(self, action_space):
        self.action_space = action_space

    def act(self, observation, reward, done):
        act_orders = []
        player, observation = observation[-1], observation[:-1]
        owner = observation[::2]
        supplycenter = observation[1::2]
        for i, (province_owner, province_sc) in enumerate(zip(owner, supplycenter)):
            if province_owner == player:
                unit_order = [i] + self.action_space.sample().tolist()
                act_orders.append(unit_order)
        return act_orders


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=None)
    parser.add_argument('env_id', nargs='?', default='Diplomacy_Strategy-v0', help='Select the environment to run')
    args = parser.parse_args()

    # You can set the level to logger.DEBUG or logger.WARN if you
    # want to change the amount of output.
    logger.set_level(logger.INFO)

    env = gym.make(args.env_id)

    # You provide the directory to write to (can be an existing
    # directory, including one with existing data -- all monitor files
    # will be namespaced). You can also dump to a tempdir if you'd
    # like: tempfile.mkdtemp().
    outdir = '/tmp/random-agent-results'
    env = wrappers.Monitor(env, directory=outdir, force=True)
    env.seed(0)
    agent = RandomAgent(env.action_space)

    episode_count = 10
    reward = 0
    done = False

    for i in range(episode_count):
        ob = env.reset()
        logger.debug("First observation: {}".format(ob))
        while True:
            action = agent.act(ob, reward, done)
            # TODO: Instead of having a loop with episode count, try to get num_of_steps working
            # TODO: Probably the step function needs to be in a separate thread. It would
            ob, reward, done, info = env.step(action)
            if done:
                logger.info("Game/episode {} has ended.".format(i))
                break
            # Note there's no env.render() here. But the environment still can open window and
            # render if asked by env.monitor: it calls env.render('rgb_array') to record video.
            # Video is not recorded every episode, see capped_cubic_video_schedule for details.

    # Close the monitor and write monitor result info to disk
    env.close()

    # Explicitly close env, because Monitor does not call env close.
    # Issues has been fixed (https://github.com/openai/gym/pull/1023) but I still don't have the new version.
    env.env.close()
