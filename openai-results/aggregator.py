import re

def get_powers_info(results_file_list):

  power_to_times_played = {}
  power_to_sc = {}

  for results_file in results_file_list:
    file_object = open(results_file, 'r')
    lines = file_object.readlines()

    open_ai_lines = []
    open_ai_name = 'OpenAINegotiator'

    for line in lines:
      if re.search(open_ai_name, line) != None:
        open_ai_lines.append(line)

    for line in open_ai_lines:
      splits = line.split()
      power_name = splits[3]
      sc = int(splits[4])
      
      # Sc will represent the year of loss
      if sc > 1000:
        sc = 0

      if power_name not in power_to_times_played:
        power_to_times_played[power_name] = 0

      if power_name not in power_to_sc:
        power_to_sc[power_name] = 0
      
      power_to_times_played[power_name] += 1
      power_to_sc[power_name] += sc

  # Order alphabetically
  power_to_times_played_keys = sorted(power_to_times_played.keys(), key=lambda x:x.lower())

  number_of_games = 0
  number_of_sc = 0

  for key in power_to_times_played_keys:
    print("{} --> games: {}, sc: {}, sc_avg: {}".format(key, power_to_times_played[key], power_to_sc[key], power_to_sc[key]/power_to_times_played[key]))
    number_of_games += power_to_times_played[key]
    number_of_sc += power_to_sc[key]

  print ("Number of games: {}".format(number_of_games))
  print ("Number of SC: {}".format(number_of_sc))


def get_number_supply_centers(results_file_list):

  i = 0

  for results_file in results_file_list:
    file_object = open(results_file, 'r')
    lines = file_object.readlines()

    open_ai_lines = []
    open_ai_name = 'OpenAINegotiator'

    for line in lines:
      if re.search(open_ai_name, line) != None:
        open_ai_lines.append(line)

    for line in open_ai_lines:
      splits = line.split()
      power_name = splits[3]
      sc = int(splits[4])
      
      # Sc will represent the year of loss
      if sc > 1000:
        sc = 0
    
      print("{},{}".format(i, sc))
      i += 1

if __name__ == "__main__":
  # get_powers_info(['./dip-log-20-phases-ahead/bandana/gameResults.txt', './dip-log-20-phases-ahead/bandana/gameResults2.txt'])
  # get_powers_info(['./dip-log-0-phases-ahead/bandana/gameResults.txt'])

  # get_number_supply_centers(['./dip-log-0-phases-ahead/bandana/gameResults.txt'])
  get_number_supply_centers(['./dip-log-20-phases-ahead/bandana/gameResults.txt', './dip-log-20-phases-ahead/bandana/gameResults2.txt'])




