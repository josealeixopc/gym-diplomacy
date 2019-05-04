from baselines.common import plot_util as pu
results = pu.load_results('~/Documents/openai-results', verbose=True)
# results = pu.load_results('/tmp')

import matplotlib.pyplot as plt
import numpy as np

lines = ["-", "--", ":", "-."]
markers= ['o', 'x', '+', '^']
colors=['#000000', '#222222', '#444444', '#666666']

def average_graph():
  min_number_episodes = 10000

  for i in range(len(results)):
    result = results[i]
    rewards = result.monitor.r
    number_episodes = len(rewards)
    print('Results {}: number of episodes = {}'.format(i, number_episodes))

    if(number_episodes < min_number_episodes):
      min_number_episodes = number_episodes

  total_reward_per_episode = [0] * min_number_episodes

  for i in range(len(results)):
    result = results[i]
    rewards = result.monitor.r

    for j in range(min_number_episodes):
      reward_episode = rewards[j]
      total_reward_per_episode[j] += reward_episode

  average_reward_per_episode = [x / len(results) for x in total_reward_per_episode]
  print(min_number_episodes)

  plt.plot(range(len(total_reward_per_episode)), pu.smooth(average_reward_per_episode, radius=1), linewidth=1, linestyle=lines[0], color=colors[0])
  # plt.plot(range(len(total_reward_per_episode)), average_reward_per_episode, linewidth=1, linestyle=lines[0], marker=markers[0], markersize=3, color=colors[0])
  plt.xlabel('Number of episodes')
  plt.ylabel('Average reward per episode')
  plt.show()

def individual_graph():
  for i in range(len(results)):
    # print(np.ones(3))
    # print(np.cumsum(r.monitor.l))
    # print(r.monitor.r)
    # print(r.monitor.r.mean())
    # print(pu.smooth(r.monitor.r, radius=8))
    # for l in np.cumsum(r.monitor.l):
    #   plt.axvline(l, linestyle=':', linewidth=1)
    # plt.plot(np.cumsum(r.monitor.l), r.monitor.r)

    # Understanding convolution with window size: https://stackoverflow.com/a/20036959/7308982
    plt.plot(np.cumsum(results[i].monitor.l), pu.smooth(results[i].monitor.r, radius=1), linewidth=1, linestyle=lines[i], color=colors[i])

  plt.xlim(right=500)
  plt.xlabel('Number of steps')
  plt.ylabel('Average reward per episode')

  plt.show()

def plot_tactical():
  tactical_data = np.genfromtxt('/home/jazz/Documents/openai-results/open-ai-tactical.csv', delimiter=',')
  plt.plot([i[0] for i in tactical_data],  pu.smooth([i[1] for i in tactical_data], radius=1), linewidth=1, linestyle=lines[0], color=colors[0])
  plt.xlabel('Number of steps')
  plt.ylabel('Reward per episode')
  plt.show()

average_graph()
plot_tactical()