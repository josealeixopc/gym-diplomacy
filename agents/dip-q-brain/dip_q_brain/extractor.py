import codecs
import json
import os
import shutil

import tensorflow as tf
from stable_baselines.common import tf_util

from my_ppo2 import PPO2


def generate_checkpoint_from_model(model_path, checkpoint_name):
    model = PPO2.load(model_path)

    with model.graph.as_default():
        sess = tf_util.make_session(graph=model.graph)
        tf.global_variables_initializer().run(session=sess)

        if os.path.exists(checkpoint_name):
            shutil.rmtree(checkpoint_name)

        tf.saved_model.simple_save(sess, checkpoint_name, inputs={"obs": model.act_model.obs_ph},
                                   outputs={"action": model.action_ph})



def get_params_from_model(model_path):
    """
    Gets the neural network parameters from the model. From what I gather, given that it is a PPO model, it has two neural
    networks: one for the policy function (pi Tensors) and another for the value function (vf Tensors). These parameters
    include the weights and the bias of a given layer.

    From the code, it looks like the action is calculated with the value function. Therefore, we will use those in the Java side.
    One little problem is that the bias are all zero, don't know why. Therefore we'll only use weights.
    :param model_path:
    :return:
    """
    model = PPO2.load(model_path)

    with model.graph.as_default():
        sess = tf_util.make_session(graph=model.graph)
        tf.global_variables_initializer().run(session=sess)
        params = sess.run(model.params)

        params_savable = []
        for matrix in params:
            m = matrix.tolist()
            params_savable.append(m)  # nested lists with same data, indices

        file_path = "path.json"  # your path variable
        json.dump(params_savable, codecs.open(file_path, 'w', encoding='utf-8'), separators=(',', ':'), sort_keys=True,
                  indent=4)  # this saves the array in .json format


if __name__ == '__main__':
    # generate_checkpoint_from_model("pickles/ppo2-test-pickle.pkl", "checkpoint")
    get_params_from_model("/home/jazz/Documents/openai-results/dip-log/gym/pickles/2019-05-20-23-24-56-ppo2-best-model.pkl")
