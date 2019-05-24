import codecs
import json
import os
import shutil

import tensorflow as tf
import numpy as np

from my_ppo2 import PPO2


def generate_checkpoint_from_model(model_path, checkpoint_name):
    """
    Saves a TF session and graph checkpoint from an OpenAI Gym model. This function allows the model's graph and
    session to be loaded using the TensorFlow API. While the mehtod used has been deprecated, it's the simples choice
    to load in Java.

    :param model_path:
    :param checkpoint_name:
    :return:
    """
    model = PPO2.load(model_path)

    with model.graph.as_default():
        if os.path.exists(checkpoint_name):
            shutil.rmtree(checkpoint_name)

        tf.saved_model.simple_save(model.sess, checkpoint_name, inputs={"obs": model.act_model.obs_ph},
                                   outputs={"action": model.action_ph})


def get_action_from_model(model, obs: np.ndarray):
    """
    Gets the action for a environment observation. This function takes the model's graph and session and calculates
    its output for a given observation. The TF operation is given by the `deterministic_action`. The shape of the
    observation is given by the attribute `obs_ph`.

    :param model:
    :param obs:
    :return:
    """

    with model.graph.as_default():

        action = model.sess.run([model.act_model.deterministic_action],
                                {model.act_model.obs_ph: obs})

        return action


def generate_random_observation(model):
    # TODO: Make it truly random
    obs = np.ones(((1,) + model.act_model.ob_space.shape))
    obs[0][12] = 1
    obs[0][11] = 1
    obs[0][10] = 1

    return obs


def save_params_from_model(model_path, destination_path):
    """
    Gets the neural network parameters from the model. From what I gather, given that it is a PPO model, it has two neural
    networks: one for the policy function (pi Tensors) and another for the value function (vf Tensors). These parameters
    include the weights and the bias of a given layer.

    From the code, it looks like the action is calculated with the value function. Therefore, we will use those in the Java side.
    One little problem is that the bias are all zero, don't know why. Therefore we'll only use weights.
    :param destination_path:
    :param model_path:
    :return:
    """
    model = PPO2.load(model_path)

    with model.graph.as_default():
        params = model.sess.run(model.params)

        params_savable = []
        for matrix in params:
            m = matrix.tolist()
            params_savable.append(m)  # nested lists with same data, indices

        file_path = destination_path  # your path variable
        json.dump(params_savable, codecs.open(file_path, 'w', encoding='utf-8'), separators=(',', ':'), sort_keys=True,
                  indent=4)  # this saves the array in .json format


if __name__ == '__main__':
    pass
