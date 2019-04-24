import gym
from gym import spaces

import subprocess
import os
import signal
import atexit
import threading
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
NUMBER_OF_ACTIONS = 3
NUMBER_OF_PLAYERS = 2#7
NUMBER_OF_PROVINCES = 8#75


def observation_data_to_observation(observation_data: proto_message_pb2.ObservationData) -> np.array:
    """
    This function takes a Protobuf ObservationData and generates the necessary information for the agent to act.

    :param observation_data: A Protobug ObservationData object.
    :return: A list with the structure [observation, reward, done, info]. Observation is an np array, reward is a float,
    done is a boolean and info is a string.
    """
    number_of_provinces = len(observation_data.provinces)

    if number_of_provinces != NUMBER_OF_PROVINCES:
        raise ValueError("Number of provinces is not consistent. Constant variable is '{}' while received number of "
                         "provinces is '{}'.".format(NUMBER_OF_PROVINCES, number_of_provinces))

    observation = np.zeros(number_of_provinces * 4 + 1)

    for province in observation_data.provinces:
        # simply for type hint and auto-completion
        province: proto_message_pb2.ProvinceData = province

        # id - 1 because the ids begin at 1
        observation[(province.id - 1) * 4] = province.id
        observation[(province.id - 1) * 4 + 1] = province.owner
        observation[(province.id - 1) * 4 + 2] = province.sc
        observation[(province.id - 1) * 4 + 3] = province.unit
    observation[len(observation) - 1] = observation_data.player

    reward = observation_data.previousActionReward
    done = observation_data.done
    info = {}

    return observation, reward, done, info


def action_to_orders_data(action) -> proto_message_pb2.OrdersData:
    """
    Transforms the action list generated by the model into a OrdersData object that will be sent to Bandana.
    :param action: The list of the agent action.
    :return: OrdersData object with the representation of the set of orders.
    """
    orders_data: proto_message_pb2.OrdersData = proto_message_pb2.OrdersData()
    for order in action:
        new_order = orders_data.orders.add()
        new_order.start = int(order[0])
        new_order.action = int(order[1])
        new_order.destination = int(order[2])
    return orders_data


class DiplomacyStrategyEnv(gym.Env):
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
    response_available = threading.Event()

    limit_action_time: int = 0

    observation: np.ndarray = None
    action: np.ndarray = None

    info: dict = {}
    done: bool = False
    reward: float = 0

    current_agent = None

    terminate = False
    termination_complete = False


    def __init__(self):
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
        # When the agent calls step, make sure it does nothing until the agent can act
        while not self.waiting_for_action:
            pass

        self.action = action
        self.waiting_for_action = False

        # After setting 'waiting_for_action' to false, the 'handle' function should send the chosen action
        self.response_available.wait()

        return self.observation, self.reward, self.done, self.info


    def reset(self):
        """Resets the state of the environment and returns an initial observation.
        Returns: observation (object): the initial observation of the
            space.
        """
        # Set or reset current observation to None
        self.observation = None

        # In this case we simply restart Bandana
        if self.bandana_subprocess is not None:
            self._kill_bandana()
            self._init_bandana()
        else:
            self._init_bandana()

        if self.socket_server is None:
            self._init_socket_server()

        self.socket_server.terminate = False
        self.socket_server.threaded_listen()

        # Wait until the observation field has been set, by receiving the observation from Bandana
        while self.observation is None:
            pass

        return self.observation


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
        logger.debug("CLOSING ENV")

        self.terminate = True

        if self.socket_server is not None:
            self.socket_server.close()

        if self.bandana_subprocess is not None:
            self._kill_bandana()

        self.termination_complete = True


    def clean_up(self):
        logger.debug("CLEANING UP ENV")

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


    def _init_bandana(self):
        logger.info("Starting BANDANA tournament...")
        logger.debug("Running '{}' command on directory '{}'."
                     .format(self.bandana_init_command, self.bandana_root_path))

        self.bandana_subprocess = subprocess.Popen(self.bandana_init_command, cwd=self.bandana_root_path,
                                                   shell=True, preexec_fn=os.setsid)
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
        '''
        Observation space: [[province_id, owner, is supply center, has unit] * number of provinces]
        The last 2 values represent the player id and the province to pick the order.
        Eg: If observation_space[2] is [2, 5, 0, 0], then the second province belongs to player 5, is NOT a SC, and does NOT have a unit.
        '''
        observation_space_description = []

        for i in range(NUMBER_OF_PROVINCES):
            observation_space_description.extend([NUMBER_OF_PROVINCES, NUMBER_OF_PLAYERS, 2, 2])
        
        observation_space_description.extend([NUMBER_OF_PLAYERS])

        self.observation_space = spaces.MultiDiscrete([observation_space_description])


    def _init_action_space(self):
        '''
        An action represents an order for a unit.
        Action space: [Order type for the unit, Destination province]
        Eg: Action [2, 5] proposes an order of type 2 related to the province with id 5.
        '''
        self.action_space = spaces.MultiDiscrete([NUMBER_OF_ACTIONS, NUMBER_OF_PROVINCES])


    def _init_socket_server(self):
        self.socket_server = comm.LocalSocketServer(5000, self._handle)


    def _handle(self, request: bytearray) -> None:
        request_data: proto_message_pb2.BandanaRequest = proto_message_pb2.BandanaRequest()
        request_data.ParseFromString(request)

        if request_data.type is proto_message_pb2.BandanaRequest.INVALID:
            raise ValueError("Type of BandanaRequest is INVALID.", request_data)

        observation_data: proto_message_pb2.ObservationData = request_data.observation
        self.observation, self.reward, self.done, self.info = observation_data_to_observation(observation_data)

        response_data: proto_message_pb2.DiplomacyGymOrdersResponse = proto_message_pb2.DiplomacyGymOrdersResponse()
        response_data.type = proto_message_pb2.DiplomacyGymOrdersResponse.VALID

        self.waiting_for_action = True
        while self.waiting_for_action:
            if self.done or self.terminate:
                # Return empty deal just to finalize program
                logger.debug("Sending empty deal to finalize program.")
                # TODO: Terminate should not be here. Refactor all of this!
                self.socket_server.terminate = True
                return response_data.SerializeToString()

        self.received_first_observation = True
        self.response_available.set()

        orders_data: proto_message_pb2.OrdersData = action_to_orders_data(self.action)
        response_data.orders.CopyFrom(orders_data)

        return response_data.SerializeToString()


def main():
    gym = DiplomacyEnv()


if __name__ == "__main__":
    main()
