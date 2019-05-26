import logging
from gym.envs.registration import register

logger = logging.getLogger(__name__)

# This is what registers the "Diplomacy-v0" environment to be used by agents
# For a successful registration, simply use "import gym_diplomacy" when initializing agents
register(
    id='Diplomacy_Negotiation-v0',
    entry_point='gym_diplomacy.envs:DiplomacyNegotiationMultiAgentEnv'
)

register(
    id='Diplomacy_Strategy_MA-v0',
    entry_point='gym_diplomacy.envs:DiplomacyNegotiationMultiAgentEnv'
)


register(
    id='Diplomacy_Strategy-v0',
    entry_point='gym_diplomacy.envs:DiplomacyStrategyEnv'
)

