import numpy as np
import matplotlib.pyplot as plt
import json

json_file_path = "../code/data/paramsSearchFourierDModDPhase256.json"

with open(json_file_path, 'r') as j:
     contents = json.loads(j.read())

arr = np.array(contents['values'])

for dist in ['MANHATTAN', 'CUBIC', 'SIGMOID', 'SQUARE', 'XY', 'MULT', 'SQRTMIN', 'EUCLIDEAN', 'BELL']:
    compSimDict = {}
    for o in arr:
        if o['distance'] == dist:
            compSimDict.update({o['correction']: o['compressionRate']/o['similarity']})
    plt.plot(compSimDict.keys(), compSimDict.values(), label = dist.lower())
#thresh = np.zeros(len(compSimDict.keys())) + 0.5
#plt.plot(compSimDict.keys(), thresh, "r--", label = "Threshold value")

#plt.xticks([i for i in range(arrDD.shape[0]) if i % 2 == 0])
#plt.ylim(0, 0.05 + max(0.5, np.max(arrDD)))
plt.xlabel("Correction factor")
#plt.ylabel("Haar-psi similarity")
#plt.plot(thresh, "r--", label = "Threshold")

plt.legend()

plt.show()