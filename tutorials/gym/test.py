import gym
import numpy
import time
import matplotlib.pyplot as plt

# Winter is here. You and your friends were tossing around a frisbee at the park when you made a wild throw that left the frisbee out in the middle of the lake. The water is mostly frozen, but there are a few holes where the ice has melted. If you step into one of those holes, you'll fall into the freezing water. At this time, there's an international frisbee shortage, so it's absolutely imperative that you navigate across the lake and retrieve the disc. However, the ice is slippery, so you won't always move in the direction you intend.

## ACTIONS
# LEFT = 0
# DOWN = 1
# RIGHT = 2
# UP = 3

# S: start
# F: frozen surface (safe)
# H: hole (unsafe)
# G: goal

env = gym.make('FrozenLake-v0')

# Q-Table

Q = numpy.zeros([env.observation_space.n, env.action_space.n])

# Q-Learning parameters

learning_rate = 0.628
discount_factor = 0.9
num_of_episodes = 10000
limit_num_of_steps = 100

initial_temperature = 1.5

repetition = 0

while True:

  repetition += 1

  reward_list = []
  exploration_list = []
  exploitation_list = []

  for episode in range(num_of_episodes):
    state = env.reset()
    total_reward = 0

    exploration_actions = 0
    exploitation_actions = 0

    for t in range(limit_num_of_steps):
      # env.render()

      if numpy.random.uniform(0, 1) < numpy.exp(-1 / (initial_temperature / numpy.log(episode * repetition))):
        # Explore action space
        action = env.action_space.sample()
        exploration_actions += 1

      else:
        # Exploit learned values

        # argmax returns the position of the largest value. max returns the largest value.

        maximum_value = numpy.max(Q[state,:])

        if maximum_value == 0:
          action = env.action_space.sample()
        else:
          action = numpy.argmax(Q[state,:])

        exploitation_actions += 1

      # action = numpy.argmax(Q[state,:] + numpy.random.randn(1,env.action_space.n)*(1./(episode+1)))

      next_state, reward, done, info = env.step(action)

      Q[state, action] = Q[state, action] + learning_rate * (reward + discount_factor * numpy.max(Q[next_state, :]) - Q[state, action])

      total_reward += reward

      state = next_state

      ## Episode ends if I fall down a hole or reach the goal
      if done == True:
        break
    
    reward_list.append(total_reward)
    exploitation_list.append(exploitation_actions)
    exploration_list.append(exploration_actions)


  print ("Average reward per episode", str(sum(reward_list)/num_of_episodes))
  print ("Average number of exploitation actions per episode", str(sum(exploitation_list)/num_of_episodes))
  print ("Average number of exploration actions per episode", str(sum(exploration_list)/num_of_episodes))

  # print ("Q-table: {}".format(Q))

  env.close()

  input("Click Enter to continue...")