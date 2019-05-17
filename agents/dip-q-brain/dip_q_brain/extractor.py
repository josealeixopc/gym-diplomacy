import tensorflow as tf
import cloudpickle
import os

from stable_baselines.common.tf_util import save_state
import tensorflow as tf
from tensorflow.python.framework import graph_util

from my_ppo2 import PPO2

models_dir = "models/"


def save_model_from_pickle(load_path):
    model = PPO2.load(load_path, env=None)

    os.makedirs(models_dir, exist_ok=True)
    model_file = models_dir + "model"

    save_state(model_file, sess=model.sess)


def load_model_from_tf(load_path):
    tf.reset_default_graph()

    # Add ops to save and restore all the variables.
    saver = tf.train.Saver()

    # Later, launch the model, use the saver to restore variables from disk, and
    # do some work with the model.
    with tf.Session() as sess:
        # Restore variables from disk.
        saver.restore(sess, load_path)


# Add ops to save and restore all the variables.
# saver = tf.train.Saver()

# Later, launch the model, initialize the variables, do some work, and save the
# variables to disk.
# with tf.Session() as sess:
#   # Save the variables to disk.
#   save_path = saver.save(sess, "tmp/model.ckpt")
#   print("Model saved in path: %s" % save_path)

if __name__ == '__main__':
    load_model_from_tf("pickles/ppo2-test-pickle.pkl")
