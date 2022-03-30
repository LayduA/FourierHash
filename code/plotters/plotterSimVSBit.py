import numpy as np
import matplotlib.pyplot as plt

arr = np.genfromtxt('code/FourierDModRPhase256.csv')
oneColor = np.array([])
for i in range(arr.shape[0]):
    if i % 3 == 0:
        oneColor = np.append(oneColor, arr[i])
dataPlot = np.zeros((13,7))
for ind, el in enumerate(oneColor):
    if ind == 0 :
        dataPlot[6][0] = el
    elif ind <= 42 :
        dataPlot[7 + ((ind - 1) % 6)][(ind-1) // 6] = el
    else :
        dataPlot[6 - ((ind - 1) % 7)][12 - ((ind - 1) // 7)] = el
for i in range(6):
    dataPlot[i][0] = oneColor.min()

plt.imshow(dataPlot)
plt.colorbar()

plt.show()