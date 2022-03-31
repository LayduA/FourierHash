import numpy as np
import matplotlib.pyplot as plt
import json

json_file_path = "../code/data/paramsSearchFourierDModDPhase128.json"

with open(json_file_path, 'r') as j:
     contents = json.loads(j.read())

arr = np.array(contents['values'])

for dist in ['MANHATTAN', 'CUBIC', 'SIGMOID', 'SQUARE', 'XY', 'MULT', 'SQRTMIN', 'EUCLIDEAN', 'BELL']:
    simDict = {}
    compDict = {}
    for o in arr:
        if o['distance'] == dist:
            simDict.update({o['correction']:o['similarity']})#o['compressionRate']/
            compDict.update({o['correction']: o['compressionRate']})
    #plt.plot(compDict.keys(), compDict.values(), label = dist.lower() + " compression")
    plt.plot(simDict.keys(), simDict.values(), "--", label = dist.lower() + " similarity")
#thresh = np.zeros(len(compSimDict.keys())) + 0.5
#plt.plot(compSimDict.keys(), thresh, "r--", label = "Threshold value")

#plt.xticks([i for i in range(arrDD.shape[0]) if i % 2 == 0])
#plt.ylim(0, 0.05 + max(0.5, np.max(arrDD)))
plt.xlabel("Correction factor")
#plt.ylabel("Haar-psi similarity")
#plt.plot(thresh, "r--", label = "Threshold")

plt.legend()

plt.show()