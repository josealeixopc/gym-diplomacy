import os
from stable_baselines.common import tf_util
import tensorflow as tf
import shutil

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


if __name__ == '__main__':
    generate_checkpoint_from_model("pickles/ppo2-test-pickle.pkl", "checkpoint")
