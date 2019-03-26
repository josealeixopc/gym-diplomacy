import logging
from gym.envs.registration import register

logger = logging.getLogger(__name__)

register(
    id='Diplomacy-v0',
    entry_point='gym_diplomacy.envs:DiplomacyEnv'
)