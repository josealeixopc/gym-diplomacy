import unittest
import os

import gym
import gym_diplomacy


class TestInit(unittest.TestCase):

    def test_init(self):

        env = gym.make('Diplomacy-v0')

