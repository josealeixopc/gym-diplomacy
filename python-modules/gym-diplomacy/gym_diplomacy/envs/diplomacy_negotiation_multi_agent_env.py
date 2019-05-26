import logging
import time
import typing

from gym import spaces

from gym_diplomacy.envs import diplomacy_negotiation_env
from gym_diplomacy.envs.diplomacy_negotiation_env import MAXIMUM_NUMBER_OF_SC, NUMBER_OF_OPPONENTS, \
    NUMBER_OF_PHASES_AHEAD, NUMBER_OF_PLAYERS, NUMBER_OF_PROVINCES, action_to_deal_data, observation_data_to_observation

import numpy as np

from gym_diplomacy.envs.proto_message import proto_message_pb2

FORMAT = "%(asctime)s %(levelname)s -- [%(filename)s:%(lineno)s - %(funcName)s()] %(message)s"
logging.basicConfig(format=FORMAT)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
logger.disabled = False


# GLOBAL VARIABLES


class DiplomacyNegotiationMultiAgentEnv(diplomacy_negotiation_env.DiplomacyNegotiationEnv):
    # Set multi-agent flag to true
    multi_agent_env = True
    n_agents = NUMBER_OF_PLAYERS

    agents_power_id: typing.List[int] = []
    action_list: typing.List[np.ndarray] = [None] * n_agents
    observation_list: typing.List[np.ndarray] = [None] * n_agents

    info_list: typing.List[dict] = [{}] * n_agents
    done_list: typing.List[bool] = [False] * n_agents
    reward_list: typing.List[float] = [0] * n_agents

    waiting_for_action_list: typing.List[bool] = [False] * n_agents
    waiting_for_observation_to_be_processed_list: typing.List[bool] = [True] * n_agents

    enable_bandana_output = False

    def __init__(self):
        super().__init__()

    def _init_observation_space(self):
        # Observation space: [[province owner, province has supply center] * number of provinces, our_power_id]
        # Eg: If observation_space[0] is 5 and observation_space[1] is 0,
        # then the second province belongs to player 5 and does NOT have a SC.
        # The last integer denotes the ID of the power the current player is controlling.

        observation_space_description = []

        # Extend for each province
        for i in range(NUMBER_OF_PROVINCES):
            observation_space_description.extend([NUMBER_OF_PLAYERS + 1, 2])  # +1 because of the NONE power

        # Extend for the ID of the power we are controlling
        observation_space_description.extend([NUMBER_OF_PLAYERS])

        self.observation_space = spaces.MultiDiscrete(observation_space_description)

    def _init_action_space(self):
        # Action space:
        # [
        # defend_unit binary, unit to defend,
        # defend_sc binary, power to make deal with,
        # attack binary, region to attack,
        # support_attack binary, region to attack,
        # phases ahead
        # ]
        #
        # Eg: Action [0, 2, 1, 5, 1, 14, 0, 2, 4]
        # Proposes an order commitment:
        # - with NO defend_unit
        # - with defend_sc of province 5
        # - with attack region 14
        # - with NO support_attack
        # - 4 phases ahead from the current one (0 means the same phase)

        self.action_space = spaces.MultiDiscrete([2, MAXIMUM_NUMBER_OF_SC,
                                                  2, NUMBER_OF_OPPONENTS,
                                                  2, MAXIMUM_NUMBER_OF_SC,
                                                  2, MAXIMUM_NUMBER_OF_SC,
                                                  NUMBER_OF_PHASES_AHEAD])

    def step(self, actions: typing.List[np.ndarray]):
        """Run one timestep of the environment's dynamics. When end of
        episode is reached, you are responsible for calling `reset()`
        to reset this environment's state.
        Accepts an action and returns a tuple (observation, reward, done, info).
        Args:
            actions (object): an action provided by the environment
        Returns:
            observation (object): agent's observation of the current environment
            reward (float) : amount of reward returned after previous action
            done (boolean): whether the episode has ended, in which case further step() calls will return undefined results
            info (dict): contains auxiliary diagnostic information (helpful for debugging, and sometimes learning)
        """
        try:
            self.current_step_number += 1

            logger.info("Executing 'step' function...")

            logger.debug("Waiting for 'waiting_for_action' to be set to true...")

            # When the agent calls step, make sure it does nothing until the agent can act
            while not all(self.waiting_for_action_list):
                pass

            logger.debug("'waiting_for_action' has been set to true. Setting global action to generated action...")

            self.action_list = actions

            logger.info("Action to take: {}".format(actions))

            self.waiting_for_action_list = [False] * self.n_agents

            # After setting 'waiting_for_action' to false, the 'handle' function should send the chosen action

            logger.debug("'waiting_for_action': {}".format(self.waiting_for_action))

            self.waiting_for_observation_to_be_processed_list = [True] * self.n_agents

            logger.debug(
                "'waiting_for_observation_to_be_processed': {}".format(self.waiting_for_observation_to_be_processed))

            logger.debug("Waiting for new observation to be processed...")

            while any(self.waiting_for_observation_to_be_processed_list):
                pass

            if self.previous_step_end_time != 0:
                # If it's not the first step
                logger.info("Time between last and current step: {:.3f} ms.".format(time.time() -
                                                                                    self.previous_step_end_time))

            logger.info("Finished executing 'step' number {} for all agents.".format(self.current_step_number))

            self.previous_step_end_time = time.time()

            return self.observation_list, self.reward_list, self.done_list, self.info_list

        except Exception as e:
            self.clean_up()
            raise e

    def reset(self):
        """Resets the state of the environment and returns an initial observation.
        Returns: observation (object): the initial observation of the
            space.
        """
        try:
            logger.info("Executing 'reset' function...")

            # Set or reset current observation to None
            self.observation_list = [None] * self.n_agents

            # In this case we simply restart Bandana
            if self.bandana_subprocess is not None:
                if not all(self.waiting_for_action_list):
                    pass
                    # self._kill_bandana()
                    # self._init_bandana(self.enable_bandana_output)
                else:
                    # If agent tries to reset environment while we are waiting for action, then we need to send an
                    # invalid action back to the request that was left hanging and simply continue
                    self.action_list = [None] * self.n_agents
                    self.waiting_for_action_list = [False] * self.n_agents
            else:
                self._init_bandana(self.enable_bandana_output)

            # If server has not been initialized, create server
            if self.server is None:
                self._init_grpc_server()

            # Wait until the observation field has been set, by receiving the observation from Bandana
            while any(self.waiting_for_action_list):
                pass

            return self.observation_list

        except Exception as e:
            self.clean_up()
            raise e

    def render(self, mode='human'):
        raise NotImplementedError

    def handle_request(self, request: proto_message_pb2.BandanaRequest) -> proto_message_pb2.DiplomacyGymResponse:
        logger.info("Executing _handle of request...")

        if request.type is proto_message_pb2.BandanaRequest.INVALID:
            raise ValueError("Type of BandanaRequest is 'INVALID'. Something bad happened on BANDANA side.",
                             request)

        elif request.type is proto_message_pb2.BandanaRequest.GET_DEAL_REQUEST:
            response = self._handle_get_deal_request(request)

        elif request.type is proto_message_pb2.BandanaRequest.SEND_GAME_END:
            response = self._handle_send_game_end_request(request)

        else:
            raise NotImplementedError("There is no handle for request of type '{}'.".format(request.type))

        logger.info("Returning handler response.")

        response: proto_message_pb2.DiplomacyGymResponse = response

        return response

    def _handle_get_deal_request(self,
                                 request_data: proto_message_pb2.BandanaRequest) -> proto_message_pb2.DiplomacyGymResponse:
        observation_data: proto_message_pb2.ObservationData = request_data.observation

        power_id: int = request_data.observation.player

        if power_id not in self.agents_power_id:
            self.agents_power_id.append(power_id)

        observation, reward, done, info = observation_data_to_observation(observation_data)

        index: int = self.agents_power_id.index(power_id)

        self.observation_list[index] = observation
        self.reward_list[index] = reward
        self.done_list[index] = done
        self.info_list[index] = info

        response_data: proto_message_pb2.DiplomacyGymResponse = proto_message_pb2.DiplomacyGymResponse()
        response_data.type = proto_message_pb2.DiplomacyGymResponse.CONFIRM

        # No longer waiting for request from BANDANA to be processed
        self.waiting_for_observation_to_be_processed_list[index] = False

        # Waiting for action from the agent
        self.waiting_for_action_list[index] = True

        # This timeout is used when env.close() is not called.
        # For example, in the baselines agents, close() is not called.
        # Therefore, there must be a way for telling the cycle to stop.
        # This may disappear once we refactor.
        time_to_timeout = 10
        timeout = time.time() + time_to_timeout  # timeout after ten seconds

        # Wait for action to be taken. If env should terminate, then stop waiting.
        while any(self.waiting_for_action_list):
            if done:
                # Return empty deal because game is over and BANDANA needs a response
                logger.debug("Sending empty deal to finalize program.")
                return response_data

            if time.time() > timeout:
                logger.info("Timed out waiting for step function. Either step is taking too long or it hasn't been "
                            "called in '{}' seconds.".format(time_to_timeout))

                # Bandana needs to be killed, otherwise it'll ask for one more action when it shouldn't
                self._kill_bandana()

                return response_data

        # Once we have the action, send it as a deal
        deal_data: proto_message_pb2.DealData = action_to_deal_data(self.action_list[index])
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

        self.waiting_for_action[request_data.observation.player - 1] = False

        self.waiting_for_observation_to_be_processed = False

        return response_data
