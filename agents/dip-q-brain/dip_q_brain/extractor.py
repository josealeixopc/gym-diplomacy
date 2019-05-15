import tensorflow as tf
import cloudpickle
import os
from pprint import pprint

from my_ppo2 import PPO2

def _load_from_file(load_path):
  if isinstance(load_path, str):
      if not os.path.exists(load_path):
          if os.path.exists(load_path + ".pkl"):
              load_path += ".pkl"
          else:
              raise ValueError("Error: the file {} could not be found".format(load_path))

      with open(load_path, "rb") as file:
          data, params = cloudpickle.load(file)
  else:
      # Here load_path is a file-like object, not a path
      data, params = cloudpickle.load(load_path)

  return data, params

model = _load_from_file("pickles/ppo2-test-pickle.pkl")


pprint(model)

# Add ops to save and restore all the variables.
# saver = tf.train.Saver()

# Later, launch the model, initialize the variables, do some work, and save the
# variables to disk.
# with tf.Session() as sess:
#   # Save the variables to disk.
#   save_path = saver.save(sess, "tmp/model.ckpt")
#   print("Model saved in path: %s" % save_path)
