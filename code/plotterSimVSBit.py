import numpy as np
import matplotlib.pyplot as plt

arr = np.genfromtxt('code/FourierDModRPhase256.csv')
oneColor = np.array([])
for i in range(arr.shape[0]):
    if i % 3 == 2:
        oneColor = np.append(oneColor, arr[i])
dataPlot = np.zeros((7,13))
for ind, el in enumerate(oneColor):
    if ind == 0 :
        dataPlot[0][6] = el
    elif ind <= 42 :
        dataPlot[(ind-1) // 6][7 + ((ind - 1) % 6)] = el
    else :
        dataPlot[12 - ((ind - 1) // 7)][6 - ((ind - 1) % 7)] = el
plt.subplot(211)
plt.imshow(dataPlot, origin='lower')

plt.subplots_adjust(bottom=0.1, right=0.8, top=0.9)
plt.colorbar()

plt.show()