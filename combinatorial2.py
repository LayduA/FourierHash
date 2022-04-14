from itertools import permutations
import numpy as np
import matplotlib.pyplot as plt
def nChooseK(n, k):
    count = 0
    arr = [sorted(list(p)) for p in permutations(range(n),k)]
    return np.unique(arr, axis = 0)

def nChooseKModMTimesRPlusP(n, k, m, r=1, p=0):
    return np.array([(i, (i * r + p) % m) for i in nChooseK(n, k)])

def intToPlusMinus(n, size):
    return np.array([-2 * bool((n & (1 << i))) for i in range(size)])

def intToBool(n, size):
    return np.array([bool((n & (1 << i))) for i in range(size)])

primes = [3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97,101,103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181,191,193,197]

def order2toTheKModP(p):
    exp = 1
    while (2 ** (12 * exp)) % p != 1:
        exp += 1
    return exp
def indices(n, k, m, r=1, p=0):
    j = nChooseKModMTimesRPlusP(n,k,m,r,p)
    sols = np.array([])
    for x in j:
        for p in range(r):
            for i in range(1 << (k-1)):
                if np.sum((x[1] + p) * intToPlusMinus(i, k) ) % m == 0  and np.sum((intToPlusMinus(i, k) + 1)/2) % 2 == 0:
                    sols = np.append(sols,np.array([x[0], p,  (x[0]*r + p), (intToPlusMinus(i, k) + 1)/2]))
    return sols
#print(indices(10,4,31,12,0).shape)
#print(nChooseKModMTimesRPlusP(10, 4, 31, 12))
# orders = [order2toTheKModP(p) for p in primes]
# kmodP = [2**(12 * k) % 53 for k in range(order2toTheKModP(53))]
# print(kmodP)
# for i in range(3,32):
#     if len([p for p in kmodP if p + i in kmodP]) == 0:
#         print(i)
# print([k % 28 for k in kmodP])
# print(orders)
def f(x):
    temp = x % 23
    return temp

# x = 0xabcdeffff
# for m in range(50):
#     for k in range(12):
#         if f(x + 2**k - 2**m) == f(x) and k != m:
#             print(x, k, m)
def mod2(arrX):
    return np.sum(arrX) % 2

#combinations = every assignment of nVar variables
nVar = 10
combinations = np.zeros((1 << nVar, nVar))
for i in range(combinations.shape[0]):
    combinations[i] = intToBool(i, nVar)

combinations = combinations[(np.sum(combinations, axis = 1) % 2) == 0]

def powerShifted(i):
    return ((2 ** ((np.arange(nVar) * 12 + i) % 11)) % 23)

powershifted = np.zeros((12, nVar))
for i in range(12):
    powershifted[i] = powerShifted(i)

def getSum(x, i):
    return (combinations[x] * powershifted[i]).sum() % 23
count = 0
corr = np.zeros(nVar)
corr[0] = 120
for s in range(12):
    for c in range(combinations.shape[0]):
        if getSum(c, s) == 0 and combinations[c].sum() != 0:
            t = (12 * (np.where(combinations[c] == 1)[0]))
            tRepl = np.where(t == 0, 3, t)
            if tRepl.sum() % 13 == 0:
                print(combinations[c], powershifted[s], tRepl)
                count += 1
print(count)
