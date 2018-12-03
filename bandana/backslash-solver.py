import os, errno
import glob

dir = "Bandana Framework 1.3.1"

files_with_backslash = glob.glob(dir + "/*\*")

files_with_slash = []

# Get name of files in POSIX syntax (replace '\' by '/')
for f in files_with_backslash:
  new_f = f.replace('\\', '/')
  files_with_slash.append(new_f)

# For each file, create the necessary directories
for f in files_with_slash:
  directory = os.path.dirname(f)
  try:
    os.makedirs(directory)
  except OSError as e:
    if e.errno != errno.EEXIST:
        raise

# Finally change each file's path
for f in files_with_backslash:
  new_f = f.replace('\\', '/')
  os.rename(f, new_f)