import numpy as np
import matplotlib.pyplot as plt

arrDD = np.genfromtxt('../code/data/FourierDModDPhase256size.csv')
arrDD = arrDD[np.nonzero(arrDD)][1:] #First term is when we change 0 bits -> useless

arrDD2bits = np.genfromtxt('../code\data\FourierDModDPhase2562bitsPhaseSimVSSize.csv')
arrDD2bits = arrDD2bits[np.nonzero(arrDD2bits)][1:]

thresh = np.zeros(arrDD.shape) + 0.5

plt.xticks([i for i in range(arrDD.shape[0]) if i % 2 == 0])
plt.ylim(0, 0.05 + max(0.5, np.max(arrDD)))
plt.xlabel("Number of bits changed (random indices)")
plt.ylabel("Haar-psi similarity")
plt.plot(thresh, "r--", label = "Threshold")
plt.plot(arrDD, label="DModDPhase")
plt.plot(arrDD2bits, label="DModDPhase2bits")

plt.legend()

plt.show()