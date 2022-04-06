import numpy as np
import matplotlib.pyplot as plt

arr = np.genfromtxt('src/data/FourierCartesian128_DETMod_DET_Phase_MAN_RHOM_04_changing1bit.csv', delimiter=',')
arr = arr[:, :-4]
print(arr.shape)
print(np.argmax(np.mean(arr, axis=0)), np.max(np.mean(arr, axis=0)),np.var(arr, axis=0)[np.argmax(np.mean(arr,axis=0))])
print(np.argmax(np.var(arr, axis=0)), np.max(np.var(arr, axis=0)))
print(len(arr[arr > 0.5]) / len(arr), np.mean(arr), np.var(arr))