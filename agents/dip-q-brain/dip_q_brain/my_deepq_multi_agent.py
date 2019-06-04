import logging
import math
import random
import typing

import gym
# This import is needed to register the environment, even if it gives an "unused" warning
# noinspection PyUnresolvedReferences
import gym_diplomacy

import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt

FORMAT = "%(asctime)s %(levelname)-8s -- [%(filename)s:%(lineno)s - %(funcName)20s()] %(message)s"
logging.basicConfig(format=FORMAT)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
logger.disabled = False

MIN_EPSILON = 0
MAX_EPSILON = 1
LAMBDA = 0.1
GAMMA = 0
BATCH_SIZE = 128


class RandomAgent(object):
    """The world's simplest agent!"""

    def __init__(self, action_space):
        self.action_space = action_space

    def act(self, observation, reward, done):
        return self.action_space.sample()


class MyDeepQAgentModel:
    """
    My own implementation of a deep q agent
    """

    def __init__(self, num_states, num_actions, batch_size):
        self._num_states = num_states
        self._num_actions = num_actions
        self._batch_size = batch_size

        # Define the TF placeholders
        self._states = None
        self._actions = None

        # Define the TF output operations
        self._logits = None
        self._optimizer = None
        self._var_init = None

        # Setup the model
        self._define_model()

    def _define_model(self):
        self._states = tf.placeholder(dtype=tf.float32, shape=[None, self._num_states])
        self._q_s_a = tf.placeholder(dtype=tf.float32, shape=[None, self._num_actions])

        # create a couple of fully connected hidden layers
        # See this if you're wondering about multiple parameter brackets
        # https://stackoverflow.com/questions/42874825/python-functions-with-multiple-parameter-brackets
        fc1 = tf.keras.layers.Dense(64, activation=tf.nn.relu)(self._states)
        fc2 = tf.keras.layers.Dense(64, activation=tf.nn.relu)(fc1)

        self._logits = tf.keras.layers.Dense(self._num_actions)(fc2)

        loss = tf.losses.mean_squared_error(self._q_s_a, self._logits)

        self._optimizer = tf.train.AdamOptimizer().minimize(loss)
        self._var_init = tf.global_variables_initializer()

    def predict_one(self, state: np.ndarray, sess: tf.Session):
        """
        Returns an array with the Q(s,a) values for a given state. The array contains the logits, which represent the
        Q-value of an action for the given state s.

        :param state:
        :param sess:
        :return:
        """
        return sess.run(self._logits, feed_dict={self._states: state.reshape(1, self._num_states)})

    def predict_batch(self, states: np.ndarray, sess: tf.Session):
        return sess.run(self._logits, feed_dict={self._states: states})

    def train_batch(self, x_batch: np.ndarray, y_batch: np.ndarray, sess: tf.Session):
        sess.run(self._optimizer, feed_dict={self._states: x_batch, self._q_s_a: y_batch})

    @property
    def num_actions(self):
        return self._num_actions

    @property
    def num_states(self):
        return self._num_states

    @property
    def batch_size(self):
        return self._batch_size

    @property
    def var_init(self):
        return self._var_init


class Memory:
    """
    Handles the storage and retrieval of the results of the action of the model on the environment, which can be used to
    batch train the neural network.
    """

    def __init__(self, max_memory):
        self._max_memory = max_memory
        self._samples = []

    def add_sample(self, sample):
        self._samples.append(sample)

        if len(self._samples) > self._max_memory:
            self._samples.pop(0)

    def sample(self, num_samples):
        if num_samples > len(self._samples):
            return random.sample(self._samples, len(self._samples))
        else:
            return random.sample(self._samples, num_samples)


class MultiAgentRunner:
    def __init__(self, sess, model, env, memories, max_eps, min_eps, decay, num_agents):
        self._sess = sess
        self._model: MyDeepQAgentModel = model
        self._env = env
        self._memories: typing.List[Memory] = memories
        self._max_eps = max_eps
        self._min_eps = min_eps
        self._decay = decay
        self._num_agents = num_states

        self._epsilon = self._max_eps
        self._steps = 0
        self._reward_store = []

    def run(self):
        state_list: typing.List[np.ndarray] = self._env.reset()
        episode_reward = 0

        while True:
            actions_list = []

            for i in range(self._num_agents):
                actions_list.append(self._choose_action(state_list[i]))

            next_state_list, reward_list, done_list, info_list = self._env.step(actions_list)

            for i in range(self._num_agents):
                if done_list[i]:
                    next_state_list[i] = None

                self._memories[i].add_sample((state_list[i], actions_list[i], reward_list[i], next_state_list[i]))
                self._replay(self._memories[i])

            self._steps += 1
            self._epsilon = MIN_EPSILON + (MAX_EPSILON - MIN_EPSILON) * math.exp(-LAMBDA * self._steps)

            state_list = next_state_list
            episode_reward += reward_list[0]

            if done_list[0]:
                self._reward_store.append(episode_reward)
                break

        print("Step {}, Total reward: {}, Epsilon: {}".format(self._steps, episode_reward, self._epsilon))

    def _choose_action(self, state):
        # Using e-greedy policy
        if random.random() < self._epsilon:
            # With probability epsilon, choose a random action
            return random.randint(0, self._model.num_actions)
        else:
            # Else, choose the best action
            return np.argmax(self._model.predict_one(state, self._sess))

    def _replay(self, memory):
        batch = memory.sample(self._model.batch_size)
        initial_states = np.array([val[0] for val in batch])
        next_states = np.array([(np.zeros(self._model.num_states) if val[3] is None else val[3]) for val in batch])

        # predict Q(s,a) given the batch of states
        q_s_a = self._model.predict_batch(initial_states, self._sess)

        # predict Q(s',a') - so that we can do gamma * max(Q(s'a')) below
        q_s_a_d = self._model.predict_batch(next_states, self._sess)

        # setup training arrays
        x = np.zeros((len(batch), self._model.num_states))
        y = np.zeros((len(batch), self._model.num_actions))

        for i, b in enumerate(batch):
            # For each element in batch, try to reduce the difference between the current Q(s,a) value and teh Q(s', a')
            state, action, reward, next_state = b[0], b[1], b[2], b[3]

            current_q = q_s_a[i]

            # update the q value for action
            if next_state is None:
                # in this case, the game completed after action, so there is no max Q(s',a')
                # prediction possible
                current_q[action] = reward
            else:
                current_q[action] = reward + GAMMA * np.amax(q_s_a_d[i])
            x[i] = state
            y[i] = current_q

        self._model.train_batch(x, y, self._sess)

    @property
    def reward_store(self):
        return self._reward_store


if __name__ == '__main__':

    # You can set the level to logger.DEBUG or logger.WARN if you
    # want to change the amount of output.

    gym_env = gym.make('Diplomacy_Negotiation_MA-v0')

    # You provide the directory to write to (can be an existing
    # directory, including one with existing data -- all monitor files
    # will be namespaced). You can also dump to a tempdir if you'd
    # like: tempfile.mkdtemp().

    num_states = gym_env.observation_space.shape[0]
    num_actions = gym_env.action_space.n

    model = MyDeepQAgentModel(num_states, num_actions, BATCH_SIZE)
    mem = Memory(50000)

    with tf.Session() as sess:
        writer = tf.summary.FileWriter("/tmp/my_deepq_multi_agent", sess.graph)
        sess.run(model.var_init)
        runner = MultiAgentRunner(sess, model, gym_env, mem, MAX_EPSILON, MIN_EPSILON,
                                  LAMBDA, 7)
        num_episodes = 300
        cnt = 0
        while cnt < num_episodes:
            if cnt % 10 == 0:
                print('Episode {} of {}'.format(cnt + 1, num_episodes))
            runner.run()
            cnt += 1
        plt.plot(runner.reward_store)
        plt.show()
        writer.close()

    gym_env.close()
