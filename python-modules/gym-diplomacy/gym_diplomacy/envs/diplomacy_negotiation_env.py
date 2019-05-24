import logging
import pprint
import time

import numpy as np
from gym import spaces

from gym_diplomacy.envs import diplomacy_env
from gym_diplomacy.envs.proto_message import proto_message_pb2

FORMAT = "%(asctime)s %(levelname)s -- [%(filename)s:%(lineno)s - %(funcName)s()] %(message)s"
logging.basicConfig(format=FORMAT)

logger = logging.getLogger(__name__)
logger.setLevel(logging.CRITICAL)
logger.disabled = False

### LEVELS OF LOGGING (in increasing order of severity)
# DEBUG	    Detailed information, typically of interest only when diagnosing problems.
# INFO	    Confirmation that things are working as expected.
# WARNING	An indication that something unexpected happened, or indicative of some problem in the near future
# (e.g. 'disk space low'). The software is still working as expected.
# ERROR	    Due to a more serious problem, the software has not been able to perform some function.
# CRITICAL	A serious error, indicating that the program itself may be unable to continue running.

### CONSTANTS
NUMBER_OF_PLAYERS = 7
NUMBER_OF_OPPONENTS = NUMBER_OF_PLAYERS - 1
NUMBER_OF_PROVINCES = 75
NUMBER_OF_PHASES_AHEAD = 20
MAXIMUM_NUMBER_OF_SC = 18

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

    size_of_observation: int = (number_of_provinces * 2) + 1

    observation = np.zeros(size_of_observation)

    for province in observation_data.provinces:
        # simply for type hint and auto-completion
        province: proto_message_pb2.ProvinceData = province

        # id - 1 because the ids begin at 1
        observation[(province.id - 1) * 2] = province.owner
        observation[(province.id - 1) * 2 + 1] = province.sc

    observation[size_of_observation - 1] = observation_data.player

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

    if action is None:
        # If action is none, return a default deal data
        return deal_data

    defend_unit: proto_message_pb2.DealData.DefendUnitData = proto_message_pb2.DealData.DefendUnitData()
    defend_sc: proto_message_pb2.DealData.DefendSCData = proto_message_pb2.DealData.DefendSCData()
    attack_region: proto_message_pb2.DealData.AttackRegionData = proto_message_pb2.DealData.AttackRegionData()
    support_attack_region: proto_message_pb2.DealData.SupportAttackRegionData = proto_message_pb2.DealData.SupportAttackRegionData()

    defend_unit.execute = bool(action[0])
    defend_unit.region = action[1]

    defend_sc.execute = bool(action[2])
    defend_sc.allyPower = action[3]

    attack_region.execute = bool(action[4])
    attack_region.region = action[5]

    support_attack_region.execute = bool(action[6])
    support_attack_region.region = action[7]

    deal_data.defendUnit.CopyFrom(defend_unit)
    deal_data.defendSC.CopyFrom(defend_sc)
    deal_data.attackRegion.CopyFrom(attack_region)
    deal_data.supportAttackRegion.CopyFrom(support_attack_region)

    deal_data.phasesFromNow = action[8]

    return deal_data


class DiplomacyNegotiationEnv(diplomacy_env.DiplomacyEnv):
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

    def render(self, mode='human'):
        raise NotImplementedError

    def _init_observation_space(self):
        # Observation space: [[province owner, province has supply center] * number of provinces, our_power_id]
        # Eg: If observation_space[0] is 5 and observation_space[1] is 0,
        # then the second province belongs to player 5 and does NOT have a SC.
        # The last integer denotes the ID of the power the current player is controlling.

        observation_space_description = []

        # Extend for each province
        for i in range(NUMBER_OF_PROVINCES):
            observation_space_description.extend([NUMBER_OF_PLAYERS + 1, 2])    # +1 because of the NONE power

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
                return response_data

            if time.time() > timeout:
                logger.info("Timed out waiting for step function. Either step is taking too long or it hasn't been "
                            "called in '{}' seconds.".format(time_to_timeout))

                # Bandana needs to be killed, otherwise it'll ask for one more action when it shouldn't
                self._kill_bandana()

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

        self.waiting_for_action = False
        logger.debug("'waiting_for_action': {}".format(self.waiting_for_action))

        self.waiting_for_observation_to_be_processed = False

        return response_data


if __name__ == "__main__":
    gym = DiplomacyNegotiationEnv()
