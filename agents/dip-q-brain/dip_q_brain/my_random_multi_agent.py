import logging

import gym
from gym import wrappers
# This import is needed to register the environment, even if it gives an "unused" warning
# noinspection PyUnresolvedReferences
import gym_diplomacy

FORMAT = "%(asctime)s %(levelname)-8s -- [%(filename)s:%(lineno)s - %(funcName)20s()] %(message)s"
logging.basicConfig(format=FORMAT)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
logger.disabled = False


class RandomAgent(object):
    """The world's simplest agent!"""

    def __init__(self, action_space):
        self.action_space = action_space

    def act(self, observation, reward, done):
        return self.action_space.sample()


if __name__ == '__main__':
    # You can set the level to logger.DEBUG or logger.WARN if you
    # want to change the amount of output.

    env = gym.make('Diplomacy_Strategy_MA-v0')

    # You provide the directory to write to (can be an existing
    # directory, including one with existing data -- all monitor files
    # will be namespaced). You can also dump to a tempdir if you'd
    # like: tempfile.mkdtemp().

    n_agents = 7
    outdir = '/tmp/random-agent-results'
    # env = wrappers.Monitor(env, directory=outdir, force=True)
    # env.seed(0)
    agents = [RandomAgent(env.action_space)] * n_agents

    episode_count = 2
    reward_list = [0] * n_agents
    done_list = [False] * n_agents

    for i in range(episode_count):
        ob_list = env.reset()
        while True:
            action = tuple([agents[i].act(ob_list[i], reward_list[i], done_list[i]) for i in range(n_agents)])
            ob_list, reward_list, done_list, _ = env.step(action)
            if all(done_list):
                break
            # Note there's no env.render() here. But the environment still can open window and
            # render if asked by env.monitor: it calls env.render('rgb_array') to record video.
            # Video is not recorded every episode, see capped_cubic_video_schedule for details.

    # Close the env and write monitor result info to disk
    env.close()
