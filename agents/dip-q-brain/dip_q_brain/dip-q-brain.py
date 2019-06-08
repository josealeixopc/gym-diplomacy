import argparse
import csv
import logging
import os
import re
from datetime import datetime

import gym
# This import is needed to register the environment, even if it gives an "unused" warning
# noinspection PyUnresolvedReferences
import gym_diplomacy
import numpy as np
from stable_baselines.bench import Monitor
from stable_baselines.bench.monitor import LoadMonitorResultsError
from stable_baselines.common import set_global_seeds
from stable_baselines.common.vec_env import DummyVecEnv, SubprocVecEnv
from stable_baselines.results_plotter import load_results, ts2xy

import utils
from my_deepq import DQN
from my_ppo2 import PPO2
from stable_baselines.a2c import A2C
from plotter import plot_results

FORMAT = "%(asctime)s %(levelname)-8s -- [%(filename)s:%(lineno)s - %(funcName)20s()] %(message)s"
logging.basicConfig(format=FORMAT)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
logger.disabled = False

### CUSTOM GLOBAL CODE BEGIN

saving_interval = 1  # 1 update is equivalent to 128 steps in PPO because of the mini-batches
best_mean_reward, n_steps = -np.inf, 0
verbose_level = 1

# Create log dir
current_time_string = datetime.now().strftime('%Y-%m-%d-%H-%M-%S')
log_dir = "/tmp/dip-log/gym/"
pickle_dir = log_dir + "pickles/"
tensorboard_dir = log_dir + "tensorboard/"

os.makedirs(log_dir, exist_ok=True)
os.makedirs(pickle_dir, exist_ok=True)
os.makedirs(tensorboard_dir, exist_ok=True)


### CUSTOM GLOBAL CODE END

def main(args):
    if not args.results_only:
        train(args.algorithm, args.env_id, args.num_steps)
    plot_results(log_dir)


def make_env(env_id, rank, seed=0):
    """
        Utility function for creating multiprocess env.

        :param env_id: (str) the environment ID
        :param seed: (int) the initial seed for RNG
        :param rank: (int) index of the subprocess
        """

    def _init():
        env = gym.make(env_id)
        env.seed(seed + rank)
        return env

    set_global_seeds(seed)
    return _init


def load_model(algorithm, gym_env_id):
    global best_mean_reward

    model = None

    multiprocess = False
    num_cpu = 4  # Number of processes to use in multiprocess env

    env = None

    if multiprocess:
        env = SubprocVecEnv([make_env(gym_env_id, i) for i in range(num_cpu)])
    else:
        gym_env = gym.make(gym_env_id)
        monitor_file_path = log_dir + current_time_string + "-monitor.csv"
        env = Monitor(gym_env, monitor_file_path, allow_early_resets=True)

        # vectorized environments allow to easily multiprocess training
        # we demonstrate its usefulness in the next examples
        env = DummyVecEnv([lambda: env])  # The algorithms require a vectorized environment to run

    existing_pickle_files = utils.get_files_with_pattern(pickle_dir, r'(.*)' + algorithm + "-best-model.pkl")

    # Sort files in reverse alphabetical order, so that models with newer dates are chosen first.
    existing_pickle_files.sort(reverse=True)

    for file_name in existing_pickle_files:
        search = re.search(r'(.*)' + algorithm + "-best-model.pkl", file_name)

        if search:
            if algorithm == 'deepq':
                model = DQN.load(file_name, env=env, verbose=verbose_level, tensorboard_log=tensorboard_dir)
            elif algorithm == 'ppo2':
                model = PPO2.load(file_name, env=env, verbose=verbose_level, tensorboard_log=tensorboard_dir)
            else:
                raise Exception("Algorithm not supported: {}".format(algorithm))

            logger.info(
                "Loading existing pickle file '{}' for environment {} with algorithm {} and policy '{}'.".format(
                    file_name, gym_env_id, algorithm, model.policy))

            logger.info("Searching for previous best mean reward of algorithm '{}'...".format(algorithm))

            best_mean_reward = get_best_mean_reward_from_results()

            if best_mean_reward != -np.inf:
                logger.info("Found previous best mean reward: {}".format(best_mean_reward))
            else:
                logger.info("Could not find previous best mean reward. Starting with: {}".format(best_mean_reward))

            return model

    logger.info(
        "No pickle was found for environment {}. Creating new model with algorithm {} and policy 'MlpPolicy'...".format(
            gym_env_id, algorithm))

    if algorithm == 'deepq':
        model = DQN(policy='MlpPolicy', env=env, verbose=verbose_level, tensorboard_log=tensorboard_dir)
    if algorithm == 'ppo2':
        model = PPO2(policy='MlpPolicy', env=env, verbose=verbose_level, tensorboard_log=tensorboard_dir)

    return model


def train(algorithm, gym_env_id, total_timesteps):
    if algorithm != 'ppo2' and algorithm != 'deepq':
        logger.error("Given algorithm '{}' is not available. Only 'ppo2' and 'deepq'")
        return None

    if algorithm == 'deepq':
        logger.warn("The 'deepq' algorithm may not work with large action spaces. If the action space is too big, "
                    "and there isn't enough RAM, the program will crash.")

    model = load_model(algorithm, gym_env_id)

    model.learn(total_timesteps, callback=callback)

    return model


def callback(_locals, _globals):
    """
    Callback called at each step (for DQN an others) or after n steps (see ACER or PPO2MA)
    :param _locals: (dict)
    :param _globals: (dict)
    """
    global n_steps, best_mean_reward, saving_interval, pickle_dir

    # Print stats every X calls
    if (n_steps + 1) % saving_interval == 0:
        # Evaluate policy training performance
        x, y = ts2xy(load_results(log_dir), 'timesteps')
        if len(x) > 0:
            mean_reward = np.mean(y[-100:])
            logger.info("{} timesteps".format(x[-1]))
            logger.info(
                "Best mean reward: {:.2f} - Last mean reward per episode: {:.2f}".format(best_mean_reward, mean_reward))

            # New best model, you could save the agent here
            if mean_reward > best_mean_reward:
                best_mean_reward = mean_reward
                # Example for saving best model
                logger.info("Saving new best model")
                _locals['self'].save(pickle_dir + current_time_string + '-ppo2-best-model.pkl')
                logger.info("Saving new best mean reward.")
                save_best_mean_reward(float(best_mean_reward))

    n_steps += 1
    # Returning False will stop training early
    return True


def evaluate(env, model, num_steps=100):
    """
    Evaluate a RL agent
    :param env:
    :param model: (BaseRLModel object) the RL Agent
    :param num_steps: (int) number of timesteps to evaluate it
    :return: (float) Mean reward for the last 100 episodes
    """
    episode_rewards = [0.0]
    obs = env.reset()
    for i in range(num_steps):
        # _states are only useful when using LSTM policies
        action, _states = model.predict(obs)
        # here, action, rewards and dones are arrays
        # because we are using vectorized env
        obs, rewards, dones, info = env.step(action)

        # Stats
        episode_rewards[-1] += rewards[0]
        if dones[0]:
            obs = env.reset()
            episode_rewards.append(0.0)
    # Compute mean reward for the last 100 episodes
    mean_100ep_reward = round(float(np.mean(episode_rewards[-100:])), 1)
    print("Mean reward:", mean_100ep_reward, "Num episodes:", len(episode_rewards))

    return mean_100ep_reward


def save_best_mean_reward(reward: float):
    best_mean_reward_file_name = log_dir + current_time_string + "-best-mean-reward.csv"
    f = open(best_mean_reward_file_name, 'a')
    f.write("{}\n".format(reward))
    f.close()


def get_best_mean_reward_from_results():
    existing_reward_files = utils.get_files_with_pattern(log_dir, r'(.*)' + "-best-mean-reward.csv")
    existing_reward_files.sort(reverse=True)

    if len(existing_reward_files) == 0:
        return -np.inf

    with open(existing_reward_files[0], 'r') as f:
        # Get latest reward
        last_row = list(csv.reader(f))[-1]

        # Each row has only one element: the reward
        return float(last_row[0])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=None)
    parser.add_argument('--algorithm', nargs='?', default='ppo2', help='Choose the algorithm to use.')
    parser.add_argument('--env_id', nargs='?', default='Diplomacy_Negotiation-v0',
                        help='Select the environment to run')
    parser.add_argument('--num_steps', type=int, help='The number of steps to train the agent')
    parser.add_argument('--results_only', '-r', action='store_true')
    args = parser.parse_args()
    main(args)
