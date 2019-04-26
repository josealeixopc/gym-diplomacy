from baselines.common import plot_util as pu
results = pu.load_results('~/Documents')
# results = pu.load_results('/tmp')

import matplotlib.pyplot as plt
import numpy as np
r = results[0]
plt.plot(np.cumsum(r.monitor.l), r.monitor.r)
#plt.plot(np.cumsum(r.monitor.l), pu.smooth(r.monitor.r, radius=10))
plt.show()