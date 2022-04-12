from itertools import permutations
import numpy as np
arr = np.array([(2 ** (12*m)) % 23 for m in range(10)])
poss3 = []
poss4 = []
types = {}
print(arr)
def concat(arr):
    s = ""
    for a in arr:
        s = s + str(a)
    return s
for p in permutations(arr,3):
    if (p[0] + p[1] + p[2]) % 23 == 0:
        poss3.append(np.array(p))
        types.update({concat(np.sort(p)):'+++'})
    if (p[0] + p[1] - p[2]) % 23 == 0:
        poss3.append(np.array(p))
        types.update({concat(np.sort(p)):'++-'})
for p in permutations(arr,4):
    if (p[0] + p[1] + p[2] + p[3]) % 23 == 0:
        poss4.append(np.array(p))
        types.update({concat(np.sort(p)):'++++'})
    if (p[0] + p[1] + p[2] - p[3]) % 23 == 0:
        poss4.append(np.array(p))
        types.update({concat(np.sort(p)):'+++-'})
    if (p[0] + p[1] - p[2] - p[3]) % 23 == 0:
        poss4.append(np.array(p))
        types.update({concat(np.sort(p)):'++--'})
vals = np.array(sorted(np.array(poss4), key = lambda p:(p[0],p[1],p[2])))
vals = np.unique(vals, axis = 0)
r = np.apply_along_axis(lambda perm: np.array([np.where(arr == i) for i in perm]), 0, vals)
r = r.reshape((r.shape[0], r.shape[-1]))
p = []
for i in r:
    elems = arr[i]
    if types.get(concat(np.sort(elems))) is not None:
        p.append((list(i), types.get(concat(np.sort(elems)))))
        types.pop(concat(np.sort(elems)))
l = sorted(p, key = lambda pair: (pair[1], pair[0]))
for el in l:
    print(el)

