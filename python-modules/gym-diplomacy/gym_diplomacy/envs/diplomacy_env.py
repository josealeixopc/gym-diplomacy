import gym
from gym import error, spaces, utils
from gym.utils import seeding

from .comm import LocalSocketServer

import subprocess
import os
import signal
import json
import atexit

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

NUMBER_OF_PROVINCES = 75
NUMBER_OF_OPPONENTS = 7


class RequestHandler:
    def handle(self, request: str):
        request_json = json.loads(request)

        command_type: str = request_json['command']

        logger.info("Received request with command '{}'.".format(command_type))

        if command_type.lower() == 'GET_ACTION'.lower():
            self.get_action(request_json['data'])
        else:
            logger.warning("Command '{}' is not valid.".format(command_type))

    def get_action(self, game_state):
        pass


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
        self.socketServer = LocalSocketServer(5000, self.handler)

    def _to_observation(self, game):
        raise NotImplementedError

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
