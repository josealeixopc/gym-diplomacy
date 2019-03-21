import gym
from gym import error, spaces, utils
from gym.utils import seeding

import subprocess
import os
import signal
import socket
import atexit
import numpy as np

from gym_diplomacy.envs import proto_message_pb2
from gym_diplomacy.envs import comm

import logging

logging_level = 'DEBUG'
level = getattr(logging, logging_level)
logger = logging.getLogger(__name__)
logger.setLevel(level)

### LEVELS OF LOGGING (in increasing order of severity)
# DEBUG	    Detailed information, typically of interest only when diagnosing problems.
# INFO	    Confirmation that things are working as expected.
# WARNING	An indication that something unexpected happened, or indicative of some problem in the near future
# (e.g. ‘disk space low’). The software is still working as expected.
# ERROR	    Due to a more serious problem, the software has not been able to perform some function.
# CRITICAL	A serious error, indicating that the program itself may be unable to continue running.

### CONSTANTS

NUMBER_OF_OPPONENTS = 7
NUMBER_OF_PROVINCES = 75

fh = open("/home/jazz/Projects/FEUP/dip-q/java-modules/bandana/log/game_data.txt", "rb")

test_game = bytearray(fh.read())

fh.close()

test_game_2 = b'\n\x06\n\x02\x08\x06\x10\x01\n\x06\n\x02\x08\x06\x10\x01\n\x06\n\x02\x08\x06\x10\x01\n\x06\n\x02\x08' \
              b'\x06\x10\x01\n\x06\n\x02\x08\x01\x10\x00\n\x06\n\x02\x08\x01\x10\x01\n\x06\n\x02\x08\x01\x10\x01\n\x06' \
              b'\n\x02\x08\x04\x10\x00' \
              b'\n\x06\n\x02\x08\x04\x10\x01\n\x06\n\x02\x08\x04\x10\x01\n\x06\n\x02\x08\x07\x10\x00\n\x06\n\x02\x08' \
              b'\x07\x10\x01\n\x06\n\x02\x08\x07\x10\x01\n\x06\n\x02\x08\x03\x10\x00\n\x06\n\x02\x08\x03\x10\x01\n\x06' \
              b'\n\x02\x08\x03\x10\x00' \
              b'\n\x06\n\x02\x08\x02\x10\x00\n\x06\n\x02\x08\x02\x10\x00\n\x06\n\x02\x08\x02\x10\x00\n\x06\n\x02\x08' \
              b'\x05\x10\x01\n\x06\n\x02\x08\x05\x10\x01\n\x06\n\x02\x08\x05\x10\x00'


def observation_data_to_observation(observation_data: proto_message_pb2.ObservationData) -> np.array:
    number_of_provinces = len(observation_data.provinces)
    
    if number_of_provinces != NUMBER_OF_PROVINCES:
        raise ValueError("Number of provinces is not consistent. Constant variable is '{}' while received number of "
                         "provinces is '{}'.".format(NUMBER_OF_PROVINCES, number_of_provinces))

    observation = np.zeros((number_of_provinces, 2))

    for province in observation_data.provinces:
        # simply for type hint and auto-completion
        province: proto_message_pb2.ProvinceData = province

        # id - 1 because the ids begin at 1
        observation[province.id - 1] = [province.owner, province.sc]

    return observation


def action_to_deal_data(action: np.ndarray) -> proto_message_pb2.DealData:
    deal_data: proto_message_pb2.DealData = proto_message_pb2.DealData()

    if action.size != 3:
        raise ValueError("The array given does not have the correct number of elements.", action)

    deal_data.powerToPropose = action[0]
    deal_data.startProvince = action[1]
    deal_data.destinationProvince = action[2]

    return deal_data


class RequestHandler:
    def handle(self, request: bytearray) -> bytearray:
        observation_data: proto_message_pb2.ObservationData = proto_message_pb2.ObservationData()
        observation_data.ParseFromString(request)

        deal_data_bytes = self.get_action(observation_data)
        return deal_data_bytes

    def get_action(self, observation_data) -> bytearray:
        action = np.array([5, 21, 8])
        deal_data: proto_message_pb2.DealData = action_to_deal_data(action)
        return deal_data.SerializeToString()


class DiplomacyEnv(gym.Env):
    """
    The main OpenAI Gym class. It encapsulates an environment with
    arbitrary behind-the-scenes dynamics. An environment can be
    partially or fully observed.
    The main API methods that users of this class need to know are:
        step
        reset
        render
        close
        seed
    And set the following attributes:
        action_space: The Space object corresponding to valid actions
        observation_space: The Space object corresponding to valid observations
        reward_range: A tuple corresponding to the min and max possible rewards
    Note: a default reward range set to [-inf,+inf] already exists. Set it if you want a narrower range.
    The methods are accessed publicly as "step", "reset", etc.. The
    non-underscored versions are wrapper methods to which we may add
    functionality over time.
    """

    # Set this in SOME subclasses
    metadata = {'render.modes': []}
    reward_range = (-float('inf'), float('inf'))
    spec = None

    # Set these in ALL subclasses
    action_space = None
    observation_space = None

    metadata = {'render.modes': ['human']}

    ### CUSTOM ATTRIBUTES

    # BANDANA

    init_bandana: bool = True

    bandana_root_path: str = os.path.abspath(os.path.join(os.path.dirname(__file__),
                                                          "../../../../java-modules/bandana"))
    bandana_init_command: str = "./run-tournament.sh"

    bandana_subprocess = None

    # Communication

    handler = RequestHandler()
    socketServer = None

    # Env

    waiting_for_action: bool = False
    limit_action_time: int = 0

    def __init__(self):
        if self.init_bandana:
            self._init_bandana()

        atexit.register(self.close)

        self._init_socket_server()
        self._init_observation_space()
        self._init_action_space()

        self.socketServer.listen()

    def _init_bandana(self):
        logger.info("Starting BANDANA tournament...")
        logger.debug("Running '{}' command on directory '{}'."
                     .format(self.bandana_init_command, self.bandana_root_path))

        self.bandana_subprocess = subprocess.Popen(self.bandana_init_command, cwd=self.bandana_root_path,
                                                   shell=True, preexec_fn=os.setsid)
        logger.info("Started BANDANA tournament.")

    def _init_observation_space(self):
        # Observation space: [[province owner, province has supply center] * number of provinces]
        # Eg: If observation_space[2] isz [5, 0], then the second province belongs to player 5 and does NOT have a SC

        observation_space_description = []

        for i in range(NUMBER_OF_PROVINCES):
            observation_space_description.append([NUMBER_OF_OPPONENTS, 2])

        self.observation_space = spaces.MultiDiscrete(observation_space_description)

    def _init_action_space(self):
        # Action space: [opponent to propose OC to, province of unit to move, destination province]
        # Eg: Action [2, 5, 6] proposes an order commitment to player 2 for moving a unit from province 5 to 6

        self.action_space = spaces.MultiDiscrete([NUMBER_OF_OPPONENTS, NUMBER_OF_PROVINCES, NUMBER_OF_PROVINCES])

    def _init_socket_server(self):
        self.socketServer = comm.LocalSocketServer(5000, self.handler)

    def require_step(self):
        raise NotImplementedError

    def step(self, action):
        """Run one timestep of the environment's dynamics. When end of
        episode is reached, you are responsible for calling `reset()`
        to reset this environment's state.
        Accepts an action and returns a tuple (observation, reward, done, info).
        Args:
            action (object): an action provided by the environment
        Returns:
            observation (object): agent's observation of the current environment
            reward (float) : amount of reward returned after previous action
            done (boolean): whether the episode has ended, in which case further step() calls will return undefined results
            info (dict): contains auxiliary diagnostic information (helpful for debugging, and sometimes learning)
        """
        if not self.waiting_for_action:
            raise Exception('Environment is not waiting for action')
        else:
            raise NotImplementedError

    def reset(self):
        """Resets the state of the environment and returns an initial observation.
        Returns: observation (object): the initial observation of the
            space.
        """
        raise NotImplementedError

    def render(self, mode='human'):
        """Renders the environment.
        The set of supported modes varies per environment. (And some
        environments do not support rendering at all.) By convention,
        if mode is:
        - human: render to the current display or terminal and
          return nothing. Usually for human consumption.
        - rgb_array: Return an numpy.ndarray with shape (x, y, 3),
          representing RGB values for an x-by-y pixel image, suitable
          for turning into a video.
        - ansi: Return a string (str) or StringIO.StringIO containing a
          terminal-style text representation. The text can include newlines
          and ANSI escape sequences (e.g. for colors).
        Note:
            Make sure that your class's metadata 'render.modes' key includes
              the list of supported modes. It's recommended to call super()
              in implementations to use the functionality of this method.
        Args:
            mode (str): the mode to render with
        Example:
        class MyEnv(Env):
            metadata = {'render.modes': ['human', 'rgb_array']}
            def render(self, mode='human'):
                if mode == 'rgb_array':
                    return np.array(...) # return RGB frame suitable for video
                elif mode is 'human':
                    ... # pop up a window and render
                else:
                    super(MyEnv, self).render(mode=mode) # just raise an exception
        """
        raise NotImplementedError

    def close(self):
        """Override _close in your subclass to perform any necessary cleanup.
        Environments will automatically close() themselves when
        garbage collected or when the program exits.
        """

        if self.bandana_subprocess is None:
            logger.info("No BANDANA process to terminate.")
        else:
            logger.info("Terminating BANDANA process...")

            # Killing the process group (pg) also kills the children, whereas killing the process would leave the children as orphan processes
            os.killpg(os.getpgid(self.bandana_subprocess.pid), signal.SIGTERM)
            self.bandana_subprocess.wait()

            logger.info("BANDANA process terminated.")

    def seed(self, seed=None):
        """Sets the seed for this env's random number generator(s).
        Note:
            Some environments use multiple pseudorandom number generators.
            We want to capture all such seeds used in order to ensure that
            there aren't accidental correlations between multiple generators.
        Returns:
            list<bigint>: Returns the list of seeds used in this env's random
              number generators. The first value in the list should be the
              "main" seed, or the value which a reproducer should pass to
              'seed'. Often, the main seed equals the provided 'seed', but
              this won't be true if seed=None, for example.
        """
        logger.warning("Could not seed environment %s", self)
        return


def main_f():
    # game: proto_message_pb2.GameData = proto_message_pb2.GameData()
    # game.ParseFromString(test_game)
    # print(game)
    # print(game_data_to_observation(game))

    # handler = RequestHandler()
    # deal_data_bytes = handler.handle(test_game)
    # deal_data: proto_message_pb2.DealData = proto_message_pb2.DealData()
    # deal_data.ParseFromString(deal_data_bytes)

    # action = np.array([5, 7, 8])
    # action_to_deal_data(action, game)

    # print(deal_data)

    gym = DiplomacyEnv()


if __name__ == "__main__":
    main_f()
