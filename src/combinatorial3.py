from itertools import permutations
import numpy as np

def nChooseK(n, k):
    arr = [sorted(list(p)) for p in permutations(range(n),k)]
    return np.unique(arr, axis = 0)

vals =nChooseK(40,4)
combs = np.array([comb for comb in vals if (2 ** (comb % 22)).sum() % 23 == 0 and comb.sum() % 21 == 0])
print(combs, len(combs))