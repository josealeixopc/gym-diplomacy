import atexit
import logging
import os
import signal
import socketserver
import subprocess
import threading
import typing
from abc import ABCMeta, abstractmethod

import gym
import numpy as np

FORMAT = "%(levelname)s -- [%(filename)s:%(lineno)s - %(funcName)s()] %(message)s"
logging.basicConfig(format=FORMAT)

logging_level = 'DEBUG'
level = getattr(logging, logging_level)
logger = logging.getLogger(__name__)
logger.setLevel(level)
logger.disabled = False


class DiplomacyEnv(gym.Env, metaclass=ABCMeta):
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

    ### CUSTOM ATTRIBUTES

    # BANDANA

    bandana_root_path: str = os.path.abspath(os.path.join(os.path.dirname(__file__),
                                                          "../../../../java-modules/bandana"))
    bandana_init_command: str = "./run-tournament.sh"

    bandana_subprocess = None

    # Communication

    # When a type hint contains names that have not been defined yet, that definition may be expressed as a string
    # literal, to be resolved later. A situation where this occurs commonly is the definition of a container class,
    # where the class being defined occurs in the signature of some of the methods.
    server: 'DiplomacyThreadedTCPServer' = None

    # Env

    current_step_number = 0

    received_first_observation: bool = False

    waiting_for_action: bool = False
    waiting_for_observation_to_be_processed: bool = True

    limit_action_time: int = 0

    observation: np.ndarray = None
    action: np.ndarray = None

    info: dict = {}
    done: bool = False
    reward: float = 0

    termination_complete = False
    terminate_socket = False

    enable_bandana_output = True

    def __init__(self):
        # Make sure the program calls clean up, even if it exits abruptly
        atexit.register(self.clean_up)

        self._init_observation_space()
        self._init_action_space()

    @abstractmethod
    def _init_observation_space(self):
        pass

    @abstractmethod
    def _init_action_space(self):
        pass

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

        self.current_step_number += 1

        logger.info("Executing 'step' function...")

        logger.debug("Waiting for 'waiting_for_action' to be set to true...")

        # When the agent calls step, make sure it does nothing until the agent can act
        while not self.waiting_for_action:
            pass

        logger.debug("'waiting_for_action' has been set to true. Setting global action to generated action...")

        self.action = action

        self.waiting_for_action = False
        # After setting 'waiting_for_action' to false, the 'handle' function should send the chosen action

        logger.debug("'waiting_for_action': {}".format(self.waiting_for_action))

        self.waiting_for_observation_to_be_processed = True

        logger.debug(
            "'waiting_for_observation_to_be_processed': {}".format(self.waiting_for_observation_to_be_processed))

        logger.debug("Waiting for new observation to be processed...")

        while self.waiting_for_observation_to_be_processed:
            pass

        logger.debug("New observation has been processed.")

        logger.info("Finished executing 'step' number {}: ".format(self.current_step_number))
        logger.info("\t-observation: {}".format(self.observation))
        logger.info("\t-reward: {}".format(self.reward))
        logger.info("\t-done: {}".format(self.done))
        return self.observation, self.reward, self.done, self.info

    def reset(self):
        """Resets the state of the environment and returns an initial observation.
        Returns: observation (object): the initial observation of the
            space.
        """
        try:
            # Set or reset current observation to None
            self.observation = None

            # In this case we simply restart Bandana
            if self.bandana_subprocess is not None:
                self._kill_bandana()
                self._init_bandana(self.enable_bandana_output)
            else:
                self._init_bandana(self.enable_bandana_output)

            # If server has not been initialized, create server
            if self.server is None:
                self._init_socket_server()

            # Wait until the observation field has been set, by receiving the observation from Bandana
            while self.observation is None:
                pass

            return self.observation

        except Exception as e:
            self.clean_up()
            raise e

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

        logger.info("Closing environment.")

        if self.server is not None:
            self._terminate_socket_server()

        if self.bandana_subprocess is not None:
            self._kill_bandana()

        self.termination_complete = True

    def clean_up(self):
        logger.info("Cleaning up environment.")

        if not self.termination_complete:
            self.close()

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

    def _init_bandana(self, enable_output: bool = False):

        logger.info("Starting BANDANA tournament...")
        logger.debug("Running '{}' command on directory '{}'."
                     .format(self.bandana_init_command, self.bandana_root_path))

        if not enable_output:
            logger.info("Suppressing BANDANA output to STDOUT and STDERR.")
            # use DEVNULL to ignore the output from the subprocess
            self.bandana_subprocess = subprocess.Popen(self.bandana_init_command,
                                                       cwd=self.bandana_root_path,
                                                       shell=True,
                                                       preexec_fn=os.setsid,
                                                       stdout=subprocess.DEVNULL,
                                                       stderr=subprocess.DEVNULL)
        else:
            self.bandana_subprocess = subprocess.Popen(self.bandana_init_command,
                                                       cwd=self.bandana_root_path,
                                                       shell=True,
                                                       preexec_fn=os.setsid)

        logger.info("Started BANDANA tournament.")

    def _kill_bandana(self):
        if self.bandana_subprocess is None:
            logger.info("No BANDANA process to terminate.")
        else:
            logger.info("Terminating BANDANA process...")

            # Killing the process group (pg) also kills the children, whereas killing the process would leave the
            # children as orphan processes
            os.killpg(os.getpgid(self.bandana_subprocess.pid), signal.SIGTERM)
            self.bandana_subprocess.wait()

            logger.info("BANDANA process terminated.")

            # Set current process to None
            self.bandana_subprocess = None

    def _init_socket_server(self):
        self.server = DiplomacyThreadedTCPServer(self, 5000, DiplomacyTCPHandler)

        # DON'T DO 'with self.server'!!!
        # This is used in the docs, but maybe because shutdown is executed inside the same 'with' context, it doesn't
        # work
        self.server_thread = threading.Thread(target=self.server.serve_forever)

        # Exit the server thread when the main thread terminates
        self.server_thread.daemon = True
        self.server_thread.start()
        logger.info("Started ThreadedTCPServer daemon thread.")

    def _terminate_socket_server(self):
        with self.server:
            self.server.shutdown()
            self.server.server_close()
            logger.info("Shut down ThreadedTCPServer.")

    @abstractmethod
    def handle_request(self, request: bytearray) -> bytes:
        pass


class DiplomacyThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    port: int
    host: str
    # typing.Type[A] means any subclass of A
    handler_request_class: typing.Type[socketserver.BaseRequestHandler]
    diplomacy_env: DiplomacyEnv

    def __init__(self,
                 diplomacy_env: DiplomacyEnv,
                 port: int,
                 request_handler_class: typing.Type[socketserver.BaseRequestHandler],
                 host='localhost'):
        self.diplomacy_env = diplomacy_env
        self.port = port
        self.host = host
        self.address = (self.host, self.port)
        self.handler_request_class = request_handler_class

        # bind_and_activate means that the constructor will bind and activate the server right away
        # Because we want to set up the reuse address flag, this should not be the case, and we bind and activate
        # manually
        super(DiplomacyThreadedTCPServer, self).__init__(self.address,
                                                         self.handler_request_class,
                                                         bind_and_activate=False)
        self.allow_reuse_address = True
        self.server_bind()
        self.server_activate()
        logger.info("Server bound and activated at address: {}".format(self.server_address))

    def shutdown(self):
        super().shutdown()
        logger.info("Shutting down server.")


class DiplomacyTCPHandler(socketserver.BaseRequestHandler):
    """
    The request handler class for our server.

    It is instantiated once per connection to the server, and must
    override the handle() method to implement communication to the
    client.
    """

    def recv_all(self, n):
        # Helper function to recv n bytes or return None if EOF is hit
        data = b''
        while len(data) < n:
            packet = self.request.recv(n - len(data))
            if not packet:
                return None
            data += packet
        return data

    def handle(self):
        while True:
            data_length_bytes: bytes = self.recv_all(4)

            # If recv read an empty request b'', then client has closed the connection
            if not data_length_bytes:
                break

            # DON'T DO strip() ON THE DATA_LENGTH PACKET. It might delete what Python thinks is whitespace but
            # it actually is a byte that makes part of the integer.
            data_length: int = int.from_bytes(data_length_bytes, byteorder='big')

            # Don't do strip() on data either (be sure to check if there is some error if you do use)
            data: bytes = self.recv_all(data_length)

            if self.server.diplomacy_env is not None:
                response: bytes = self.server.diplomacy_env.handle_request(bytearray(data))
            else:
                logger.warning("'diplomacy_env' is None in DiplomacyTCPHandler. Assuming it's for debug reasons. "
                               "Otherwise, fix.")
                response: bytes = data.upper()

            self.request.sendall(len(response).to_bytes(4, byteorder='big'))
            self.request.sendall(response)


if __name__ == '__main__':
    # Testing
    server = DiplomacyThreadedTCPServer(None, 5000, DiplomacyTCPHandler)

    # DON'T DO 'with self.server'!!!
    # This is used in the docs, but maybe because shutdown is executed inside the same 'with' context, it doesn't
    # work
    server_thread = threading.Thread(target=server.serve_forever)

    # Exit the server thread when the main thread terminates
    server_thread.daemon = True
    server_thread.start()
    logger.info("Started ThreadedTCPServer daemon thread.")

    while input("Terminate? (y/n) ") not in 'y':
        pass

    with server:
        server.shutdown()
        server.server_close()

    import sys

    sys.exit()
