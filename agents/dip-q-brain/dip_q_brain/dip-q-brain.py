import argparse
import os
import re
from datetime import datetime

import gym
# This import is needed to register the environment, even if it gives an "unused" warning
import gym_diplomacy

import numpy as np
import matplotlib.pyplot as plt

from stable_baselines.common.vec_env import DummyVecEnv, SubprocVecEnv
from stable_baselines.common import set_global_seeds

from stable_baselines.bench import Monitor
from stable_baselines.results_plotter import load_results, ts2xy

from my_deepq import DQN
from my_ppo2 import PPO2
import utils

import logging

FORMAT = "%(levelname)-8s -- [%(filename)s:%(lineno)s - %(funcName)20s()] %(message)s"
logging.basicConfig(format=FORMAT)

logging_level = 'DEBUG'
level = getattr(logging, logging_level)
logger = logging.getLogger(__name__)
logger.setLevel(level)

### CUSTOM GLOBAL CODE BEGIN

saving_interval = 200
best_mean_reward, n_steps = -np.inf, 0

# Create log dir
current_time_string = datetime.now().strftime('%Y-%m-%d-%H-%M-%S')
log_dir = "/tmp/dip/gym/"
pickle_dir = log_dir + "pickles/"
os.makedirs(log_dir, exist_ok=True)
os.makedirs(pickle_dir, exist_ok=True)

### CUSTOM GLOBAL CODE END

def main(args):
    train('ppo2', args.env_id, 400)
    plot_results(log_dir)

def make_env(env_id, rank, seed=0):
    """
    Utility function for multiprocessed env.
    
    :param env_id: (str) the environment ID
    :param num_env: (int) the number of environment you wish to have in subprocesses
    :param seed: (int) the inital seed for RNG
    :param rank: (int) index of the subprocess
    """
    def _init():
        env = gym.make(env_id)
        env.seed(seed + rank)
        return env
    set_global_seeds(seed)
    return _init


def load_model(algorithm, gym_env_id):
    model = None
    model_steps = 0
    
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
        env = DummyVecEnv([lambda: env]) # The algorithms require a vectorized environment to run

    existing_pickle_files = utils.get_files_with_pattern(pickle_dir, algorithm + r'_' + gym_env_id + r'_(.*)_steps.pkl')
    
    for file_name in existing_pickle_files:
        search = re.search(algorithm + r'_' + gym_env_id + r'_(.*)_steps.pkl', file_name)
         
        if search:
            if algorithm == 'deepq':
                model = DQN.load(file_name, env=env, verbose=0)
            elif algorithm == 'ppo2':
                model = PPO2.load(file_name, env=env, verbose=0)
            else:
                raise Exception("Algorithm not supported: {}".format(algorithm))

            model_steps = int(search.group(1))

            logger.info("Loading existing pickle file for environment {} with {} steps of training with algorithm {} and policy '{}'.".format(gym_env_id, model_steps, algorithm, model.policy))

            return model, model_steps
    
    logger.info("No pickle was found for environment {}. Creating new model with algorithm {} and policy 'MlpPolicy'...".format(gym_env_id, algorithm))

    if algorithm == 'deepq':
        model = DQN(policy='MlpPolicy', env=env, verbose=0)
    if algorithm == 'ppo2':
        model = PPO2(policy='MlpPolicy', env=env, verbose=0)

    return model, model_steps  

def train(algorithm, gym_env_id, total_timesteps, saving_interval=200):
    if algorithm != 'ppo2' and algorithm != 'deepq':
        logger.error("Given algorithm '{}' is not available. Only 'ppo2' and 'deepq'")
        return None
    
    model, model_steps = load_model(algorithm, gym_env_id)
    
    while model_steps < total_timesteps:
        # model.learn(saving_interval, callback=callback)
        model.learn(saving_interval)
        model_steps += saving_interval

        # Save new pickle before removing old ones
        model.save(pickle_dir + "{}_{}_{}_tmp".format(algorithm, gym_env_id, model_steps))
        # model.save_tf_session(pickle_dir + "{}_{}_{}_tf_model".format(algorithm, gym_env_id, model_steps))

        # Remove old pickle(s)
        utils.remove_files_with_pattern(pickle_dir, algorithm + r'.*steps.pkl')

        # Rename new pickle
        utils.rename_files(pickle_dir, algorithm + r'.*tmp.pkl', r'tmp', r'steps')
    
    return model

def callback(_locals, _globals):
    """
    Callback called at each step (for DQN an others) or after n steps (see ACER or PPO2)
    :param _locals: (dict)
    :param _globals: (dict)
    """
    global n_steps, best_mean_reward
    # Print stats every X calls
    if (n_steps + 1) % saving_interval == 0:
        # Evaluate policy training performance
        x, y = ts2xy(load_results(log_dir), 'timesteps')
        if len(x) > 0:
            mean_reward = np.mean(y[-100:])
            print(x[-1], 'timesteps')
            print("Best mean reward: {:.2f} - Last mean reward per episode: {:.2f}".format(best_mean_reward, mean_reward))

            # New best model, you could save the agent here
            if mean_reward > best_mean_reward:
                best_mean_reward = mean_reward
                # Example for saving best model
                print("Saving new best model")
                _locals['self'].save(current_pickle_dir + 'best_model.pkl')
    n_steps += 1
    # Returning False will stop training early
    return True

def evaluate(env, model, num_steps=1000):
    """
    Evaluate a RL agent
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
    mean_100ep_reward = round(np.mean(episode_rewards[-100:]), 1)
    print("Mean reward:", mean_100ep_reward, "Num episodes:", len(episode_rewards))
    
    return mean_100ep_reward

def moving_average(values, window):
    """
    Smooth values by doing a moving average
    :param values: (numpy array)
    :param window: (int)
    :return: (numpy array)
    """
    weights = np.repeat(1.0, window) / window
    return np.convolve(values, weights, 'valid')


def plot_results(log_folder, title='Learning Curve'):
    """
    plot the results

    :param log_folder: (str) the save location of the results to plot
    :param title: (str) the title of the task to plot
    """
    x, y = ts2xy(load_results(log_folder), 'timesteps')
    y = moving_average(y, window=1)
    # Truncate x
    x = x[len(x) - len(y):]

    fig = plt.figure(title)
    plt.plot(x, y)
    plt.xlabel('Number of Timesteps')
    plt.ylabel('Rewards')
    plt.title(title + " Smoothed")
    plt.show()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=None)
    parser.add_argument('env_id', nargs='?', default='Diplomacy-v0', help='Select the environment to run')
    args = parser.parse_args()
    main(args)