import numpy as np
import matplotlib.pyplot as plt

arr = np.genfromtxt('src/data/FourierCartesian128_MANRHOM_035_changing1bit.csv', delimiter=',')
arr = arr[:,:-8]
print(np.argmax(np.mean(arr, axis=0)), np.min(arr.mean(axis=0)), np.max(np.mean(arr, axis=0)))
print(np.sum(arr > 0.5) / np.sum(arr > 0) * 100, '%', np.mean(arr), np.var(arr))
#print(np.argsort(arr.mean(axis=0))//12)
avgsPerGroup = arr.mean(axis=0).reshape((10,12))
avgsPerFrequence = arr.mean(axis=0).reshape((30,4))
print("std per freq", avgsPerFrequence.std(axis=1))
print(np.argsort(avgsPerGroup.mean(axis=1)))
print(avgsPerGroup.mean(axis=1), avgsPerGroup.std(axis=1))
print(avgsPerGroup.mean(axis=1).mean(), avgsPerGroup.mean(axis=1).std())
plt.bar(range(avgsPerGroup.shape[0]), avgsPerGroup.mean(axis=1))
plt.show()
