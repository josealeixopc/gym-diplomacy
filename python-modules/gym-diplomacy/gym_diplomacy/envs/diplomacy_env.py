import threading

import gym
from gym import spaces

import subprocess
import os
import time
import signal
import atexit
import numpy as np

from gym_diplomacy.envs import proto_message_pb2
from gym_diplomacy.envs import comm

import logging

FORMAT = "%(levelname)-8s -- [%(filename)s:%(lineno)s - %(funcName)20s()] %(message)s"
logging.basicConfig(format=FORMAT)

logging_level = 'DEBUG'
level = getattr(logging, logging_level)
logger = logging.getLogger(__name__)
logger.setLevel(level)

### LEVELS OF LOGGING (in increasing order of severity)
# DEBUG	    Detailed information, typically of interest only when diagnosing problems.
# INFO	    Confirmation that things are working as expected.
# WARNING	An indication that something unexpected happened, or indicative of some problem in the near future
# (e.g. 'disk space low'). The software is still working as expected.
# ERROR	    Due to a more serious problem, the software has not been able to perform some function.
# CRITICAL	A serious error, indicating that the program itself may be unable to continue running.

### CONSTANTS

NUMBER_OF_OPPONENTS = 7
NUMBER_OF_PROVINCES = 75


def observation_data_to_observation(observation_data: proto_message_pb2.ObservationData) -> np.array:
    """
    This function takes a Protobuf ObservationData and generates the necessary information for the agent to act.

    :param observation_data: A Protobuf ObservationData object.
    :return: A list with the structure [observation, reward, done, info]. Observation is an np array, reward is a float,
    done is a boolean and info is a dictionary.
    """
    number_of_provinces = len(observation_data.provinces)

    if number_of_provinces != NUMBER_OF_PROVINCES:
        raise ValueError("Number of provinces is not consistent. Constant variable is '{}' while received number of "
                         "provinces is '{}'.".format(NUMBER_OF_PROVINCES, number_of_provinces))

    observation = np.zeros(number_of_provinces * 2)

    for province in observation_data.provinces:
        # simply for type hint and auto-completion
        province: proto_message_pb2.ProvinceData = province

        # id - 1 because the ids begin at 1
        observation[(province.id - 1) * 2] = province.owner
        observation[(province.id - 1) * 2 + 1] = province.sc

    reward = observation_data.previousActionReward
    done = observation_data.done
    info = {"info_string": observation_data.info}

    return observation, reward, done, info


def action_to_deal_data(action: np.ndarray) -> proto_message_pb2.DealData:
    """
    This function takes a NumPy array generated by the DRL model and turns it into a DealData object that can be sent to Bandana.
    :param action: The NumPy array corresponding to the agent's action.
    :return: A DealData object with the translation of the NumPy array to a deal.
    """
    deal_data: proto_message_pb2.DealData = proto_message_pb2.DealData()

    if action.size != 5:
        raise ValueError("The array given does not have the correct number of elements.", action)

    our_move: proto_message_pb2.DealData.MTOOrderData = proto_message_pb2.DealData.MTOOrderData()
    their_move: proto_message_pb2.DealData.MTOOrderData = proto_message_pb2.DealData.MTOOrderData()

    our_move.startProvince = action[0]
    our_move.destinationProvince = action[1]

    their_move.startProvince = action[3]
    their_move.destinationProvince = action[4]

    deal_data.powerToPropose = action[2]
    deal_data.ourMove.CopyFrom(our_move)
    deal_data.theirMove.CopyFrom(their_move)

    return deal_data


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

    bandana_root_path: str = os.path.abspath(os.path.join(os.path.dirname(__file__),
                                                          "../../../../java-modules/bandana"))
    bandana_init_command: str = "./run-tournament.sh"

    bandana_subprocess = None

    # Communication

    socket_server = None

    # Env

    received_first_observation: bool = False

    waiting_for_action: bool = False
    waiting_for_observation_to_be_processed: bool = True

    limit_action_time: int = 0

    observation: np.ndarray = None
    action: np.ndarray = None

    info: dict = {}
    done: bool = False
    reward: float = 0

    current_agent = None

    terminate = False
    termination_complete = False

    cheat = 0

    enable_bandana_output = True

    def __init__(self):
        # Make sure the program calls clean up, even if it exits abruptly
        atexit.register(self.clean_up)

        self._init_observation_space()
        self._init_action_space()

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

        logger.info("Finished executing 'step': ")
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

            if self.socket_server is None:
                self._init_socket_server()

            self.socket_server.terminate = False
            self.socket_server.threaded_listen()

            # Wait until the observation field has been set, by receiving the observation from Bandana
            while self.observation is None:
                pass

            return self.observation

        except Exception as e:
            logger.error(e)
            self.clean_up()

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

        self.terminate = True

        if self.socket_server is not None:
            self.socket_server.close()

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

    def _init_observation_space(self):
        # Observation space: [[province owner, province has supply center] * number of provinces]
        # Eg: If observation_space[2] is [5, 0], then the second province belongs to player 5 and does NOT have a SC

        observation_space_description = []

        for i in range(NUMBER_OF_PROVINCES):
            observation_space_description.extend([NUMBER_OF_OPPONENTS, 2])

        self.observation_space = spaces.MultiDiscrete(observation_space_description)

    def _init_action_space(self):
        # Action space:
        # [
        # province to move OUR from,
        # province to move OUR to,
        # opponent to propose OC to,
        # province for THEIR unit to move,
        # destination province of THEIR unit
        # ]
        #
        # Eg: Action [1, 2, 2, 5, 6] proposes an order
        # commitment for us to move from province 1 to 2 and an order commitment to player 2 for moving a unit from
        # province 5 to 6

        self.action_space = spaces.MultiDiscrete([NUMBER_OF_PROVINCES, NUMBER_OF_PROVINCES,
                                                  NUMBER_OF_OPPONENTS, NUMBER_OF_PROVINCES, NUMBER_OF_PROVINCES])

    def _init_socket_server(self):
        self.socket_server = comm.LocalSocketServer(5000, self._handle)

    def _handle(self, request: bytearray) -> bytes:
        request_data: proto_message_pb2.BandanaRequest = proto_message_pb2.BandanaRequest()
        request_data.ParseFromString(request)

        logger.info("Executing _handle of request...")

        if request_data.type is proto_message_pb2.BandanaRequest.INVALID:
            raise ValueError("Type of BandanaRequest is 'INVALID'. Something bad happened on BANDANA side.",
                             request_data)

        elif request_data.type is proto_message_pb2.BandanaRequest.GET_DEAL_REQUEST:
            response_data = self._handle_get_deal_request(request_data)

        elif request_data.type is proto_message_pb2.BandanaRequest.SEND_GAME_END:
            response_data = self._handle_send_game_end_request(request_data)

        else:
            raise NotImplementedError("There is no handle for request of type '{}'.".format(request_data.type))

        logger.info("Returning handler response.")

        # Using this just for IDE autocompletion
        response_data: proto_message_pb2.DiplomacyGymResponse = response_data

        return response_data.SerializeToString()

    def _handle_get_deal_request(self,
                                 request_data: proto_message_pb2.BandanaRequest) -> proto_message_pb2.DiplomacyGymResponse:
        observation_data: proto_message_pb2.ObservationData = request_data.observation
        self.observation, self.reward, self.done, self.info = observation_data_to_observation(observation_data)

        response_data: proto_message_pb2.DiplomacyGymResponse = proto_message_pb2.DiplomacyGymResponse()
        response_data.type = proto_message_pb2.DiplomacyGymResponse.CONFIRM

        # No longer waiting for request from BANDANA to be processed
        self.waiting_for_observation_to_be_processed = False

        # Waiting for action from the agent
        self.waiting_for_action = True

        # This timeout is used when env.close() is not called.
        # For example, in the baselines agents, close() is not called.
        # Therefore, there must be a way for telling the cycle to stop.
        # This may disappear once we refactor.
        time_to_timeout = 10
        timeout = time.time() + time_to_timeout  # timeout after ten seconds

        # Wait for action to be taken. If env should terminate, then stop waiting.
        while self.waiting_for_action:
            if self.done:
                # Return empty deal because game is over and BANDANA needs a response
                logger.debug("Sending empty deal to finalize program.")
                # TODO: Terminate should not be here. Refactor all of this!
                self.socket_server.terminate = True
                return response_data
            if time.time() > timeout:
                logger.info("Timed out waiting for step function. Either step is taking too long or it hasn't been "
                            "called in '{}' seconds.".format(time_to_timeout))

                # The socket needs to be terminated, otherwise it'll hang
                self.socket_server.terminate = True

                # Bandana needs to be killed, otherwise it'll ask for one more action when it shouldn't
                self._kill_bandana()

                return response_data

            if self.terminate:
                logger.debug("Close has been called, so we are terminating the waiting for action loop.")
                self.socket_server.terminate = True
                # self.clean_up()
                return response_data

        # Once we have the action, send it as a deal
        deal_data: proto_message_pb2.DealData = action_to_deal_data(self.action)
        response_data.deal.CopyFrom(deal_data)

        return response_data

    def _handle_send_game_end_request(self,
                                      request_data: proto_message_pb2.BandanaRequest) -> proto_message_pb2.DiplomacyGymResponse:
        logger.debug("Handling 'SEND_GAME_END'.")

        observation_data: proto_message_pb2.ObservationData = request_data.observation
        self.observation, self.reward, self.done, self.info = observation_data_to_observation(observation_data)

        if not self.done:
            raise ValueError("Received game end notification, but value of 'done' is not 'True'.")

        response_data: proto_message_pb2.DiplomacyGymResponse = proto_message_pb2.DiplomacyGymResponse()
        response_data.type = proto_message_pb2.DiplomacyGymResponse.CONFIRM

        # TODO: put these lines in a function
        self.socket_server.terminate = True

        # self.terminate = True

        self.waiting_for_action = False
        logger.debug("'waiting_for_action': {}".format(self.waiting_for_action))

        self.waiting_for_observation_to_be_processed = False

        return response_data


def main():
    gym = DiplomacyEnv()


if __name__ == "__main__":
    main()
