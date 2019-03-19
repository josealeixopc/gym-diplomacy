import unittest
import os
import sys
import logging

import gym
# noinspection PyUnresolvedReferences
import gym_diplomacy

logger = logging.getLogger()
logger.level = logging.DEBUG
stream_handler = logging.StreamHandler(sys.stdout)
logger.addHandler(stream_handler)


class TestInit(unittest.TestCase):

    def test_socket_comm(self):
        pass

    def test_init(self):
        stream_handler.stream = sys.stdout
        env = gym.make('Diplomacy-v0')

