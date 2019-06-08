import argparse

from stable_baselines.results_plotter import ts2xy, load_results
import matplotlib.pyplot as plt
import numpy as np

lines = ["-", "--", ":", "-."]
markers = ['o', 'x', '+', '^']
colors = ['#000000', '#222222', '#444444', '#666666']


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
    num_episodes, rewards = ts2xy(load_results(log_folder), 'episodes')
    rewards = moving_average(rewards, window=100)
    # Truncate x
    num_episodes = num_episodes[len(num_episodes) - len(rewards):]

    plt.figure(title)
    plt.plot(num_episodes, rewards, linewidth=1, linestyle=lines[0], color=colors[0])
    plt.xlabel('Number of Episodes')
    plt.ylabel('Rewards')
    plt.title(title + " Smoothed")
    plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=None)
    parser.add_argument('log_dir', nargs='?', help='Select the directory with the logs.')
    args = parser.parse_args()
    plot_results(args.log_dir)
