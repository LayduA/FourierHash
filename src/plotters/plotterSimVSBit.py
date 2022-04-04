from cmath import phase
import numpy as np
import matplotlib.pyplot as plt

path = 'code/data/FourierDModDPhase256_MANHATTAN_RHOMBUS_changing1bit.csv'
arr = np.genfromtxt(path, delimiter = ',')
print(arr)
arr = np.mean(arr, axis=0)

oneColor = np.array([])
oneColor = arr[0::6]
onePhase = arr[3::6]
dataPlot = np.zeros((13,7)) if '256' in path else np.zeros((8,5))

rhombus128 = np.array(
        [
            [ -1, 20, -1, -1, -1],
            [ -1, 19, 16, -1, -1],
            [ -1, 18, 15, 13, -1],
            [  0, 17, 14, 12, 11],
            [  1,  5,  8, 10, -1],
            [  2,  6, 59, -1, -1],
            [  3,  7, -1, -1, -1],
            [  4, -1, -1, -1, -1]
        ]
    )
rhombus256 = np.array([
    []
])
rhombus = rhombus128 if '128' in path else rhombus128
phasePlot = np.zeros(dataPlot.shape)
if 'RHOMBUS' in path:
    
    for ind, el in enumerate(oneColor[:21]):
        dataPlot[np.where(6 == ind)[0][0]][np.where(rhombus//6 == ind)[1][0]] = el
        phasePlot[np.where(6 == ind)[0][0]][np.where(rhombus//6 == ind)[1][0]] = onePhase[ind]
    
else:
    for ind, el in enumerate(oneColor):
        if ind == 0 :
            dataPlot[6][0] = el
        elif ind <= 42 :
            dataPlot[7 + ((ind - 1) % 6)][(ind-1) // 6] = el
        else :
            dataPlot[6 - ((ind - 1) % 7)][12 - ((ind - 1) // 7)] = el
    for i in range(6):
        dataPlot[i][0] = oneColor.min()
valMin = np.min([dataPlot.min(), phasePlot.min()])
valMax = np.max([dataPlot.max(), phasePlot.max()])
plt.subplot(1,2,1)
plt.title("Module")
plt.imshow(dataPlot, vmin = valMin, vmax = valMax)
plt.xticks(np.array([]))
plt.yticks([])

plt.subplot(1,2,2)
plt.title("Phase")
plt.xticks(np.array([]))
plt.yticks([])
plt.imshow(phasePlot, vmin = valMin, vmax = valMax) 

plt.colorbar()

plt.show()