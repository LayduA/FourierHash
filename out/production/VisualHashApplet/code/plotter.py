import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

# arr = np.genfromtxt('code/similarities05.csv', delimiter = ',')
# arr2 = np.genfromtxt('code/similarities611.csv', delimiter = ",")
# arr3 = np.genfromtxt('code/similarities12125.csv', delimiter = ",")
# arr4 = np.genfromtxt('code/similarities1823.csv', delimiter = ",")
# arr = np.vstack((arr, arr2, arr3, arr4))
arr = np.genfromtxt('code/similaritiesNOTrandom05.csv', delimiter = ',')
print(arr.shape, arr[0])
df = pd.DataFrame(arr, columns=["dist","bit","corr","value"])

grouped = df.groupby(['dist', 'corr']).value.mean()

bestCorr = {}
for dist in df.dist.unique():
    jaj = [(key[1], grouped[key]) for key in grouped.keys() if key[0] == dist and key[0] == dist]
    jaj.sort(key=lambda t: t[1])
    bestCorr.update({dist:jaj[0][0]})

def getBestCorr(row):
    return bestCorr.get(row["dist"])

df["bestCorr"] = df.apply(lambda x: getBestCorr(x), axis = 1)
print(df[df["dist"] == 3]['bestCorr'])
df = df[df["corr"] == df["bestCorr"]][["dist","bit","value"]]

def plotBits(dist):
    arr_ = df[df['dist']==dist]
    vals = {}
    for (i, e) in enumerate(pd.Series(arr_['value'])):
        #print(e)
        true_dist = i #((i % 7)**3) + ((i//7)**3)
        if not true_dist in vals.keys():
            vals.update({true_dist:(e, 1)})
        else:
            vals.update({true_dist: (vals[true_dist][0] + e, vals[true_dist][1] + 1)})
    values = [vals[k][0] / vals[k][1] for k in vals.keys()]
    print(values)
    keysar = list(zip(vals.keys(), values))
    keysar.sort(key = lambda p: p[0])
    print(keysar)
    plt.plot([k[0] for k in keysar], [k[1] for k in keysar], label = "similarity")
    plt.legend()
    plt.show()
plotBits(3)