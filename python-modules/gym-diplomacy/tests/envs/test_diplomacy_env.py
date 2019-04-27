# Import general packages for testing
import unittest
import typing

# Import specific dependencies for test setup
import socketserver
import socket
import threading

# Import packages to be tested
from gym_diplomacy.envs import diplomacy_env


class DiplomacyEnvTestCase(unittest.TestCase):
    diplomacy_env: diplomacy_env.DiplomacyEnv

    def setUp(self) -> None:
        self.diplomacy_env = diplomacy_env.DiplomacyEnv()

    def tearDown(self) -> None:
        self.diplomacy_env.close()
