import numpy as np
import matplotlib.pyplot as plt

arr = np.genfromtxt('../code/data/FourierDModDPhase128_MANHATTAN_RHOMBUS_changing1bit.csv', delimiter=',')
arr = arr[:, :-2]
print(arr.shape)
print(np.argmax(np.mean(arr, axis=0)), np.max(np.mean(arr, axis=0)),np.std(arr, axis=0)[np.argmax(np.mean(arr,axis=0))])
print(np.argmax(np.std(arr, axis=0)), np.max(np.std(arr, axis=0)))
print(len(arr[arr > 0.5]) / len(arr), np.mean(arr), np.std(arr))