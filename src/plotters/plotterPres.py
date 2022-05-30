import matplotlib.pyplot as plt
import numpy as np
xs = np.array([i/100 for i in range(-300,300)])
ys = np.sin(3 * (xs+300)) + 0.5 * np.sin((xs+300) * 2) + np.sin(xs+300) + 0.2
#xs = [-3, -2,-1,0,1,2, 3]
#ys = [0.6, 0.5, 1, 0.2, 1, 0.5, 0.6]
for i in range(len(ys)):
    if i > len(ys)//2:
        print(i, len(ys)//2-i)
        #ys[i] = ys[i - 2 * (i-len(ys)//2)]
        ys[i] = np.cos(1 * (xs[i-50] + 1.85))
plt.plot(-xs,ys)
plt.show()