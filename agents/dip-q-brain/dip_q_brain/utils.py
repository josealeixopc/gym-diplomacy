import os
import re

def remove_files_with_pattern(dir, pattern):
    for f in os.listdir(dir):
        if re.search(pattern, f):
            os.remove(os.path.join(dir, f))

def rename_files(dir, pattern_to_search, old_pattern, new_pattern):
    for f in os.listdir(dir):
        if re.search(pattern_to_search, f):
            new_name = re.sub(old_pattern, new_pattern, f)
            os.rename(os.path.join(dir, f), os.path.join(dir, new_name))

def get_files_with_pattern(dir, pattern):
    files = []
    for f in os.listdir(dir):
        if re.search(pattern, f):
            files.append(os.path.join(dir,f))
    return files