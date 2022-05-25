from itertools import permutations
from operator import mod
import numpy as np
import matplotlib.pyplot as plt
def nChooseK(n, k):
    arr = [sorted(list(p)) for p in permutations(range(n),k)]
    return np.unique(arr, axis = 0)

def nChooseKModMTimesRPlusP(n, k, m, r=1, p=0):
    return np.array([(i * r + p) % m for i in nChooseK(n, k)])

def intToPlusMinus(n, size):
    return np.array([-2 * bool((n & (1 << i))) + 1 for i in range(size)])

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
                if np.sum((x[1] + p) * intToPlusMinus(i, k) ) % m == 0:
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
# print([k % (pShift -1) for k in kmodP])
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
pSym = 23
pShift = 31
nVar = 10
combinations = np.zeros((1 << nVar, nVar))
for i in range(combinations.shape[0]):
    combinations[i] = intToBool(i, nVar)

combinations = combinations[(np.sum(combinations, axis = 1) % 2) == 0]

def powerShifted(i):
    return ((2 ** ((np.arange(nVar) * 12 + i) % (pSym-1))) % pSym)

powershifted = np.zeros((12, nVar))
for i in range(12):
    powershifted[i] = powerShifted(i)

def getSumModP(x, i, p):
    return (combinations[x] * powershifted[i]).sum() % p

count = 0
for s in range(12):
    for c in range(combinations.shape[0]):
        if getSumModP(c, s, pSym) == 0 and getSumModP(c, s, pShift)==0 and combinations[c].sum() != 0:
            print(combinations[c], powershifted[s])
            count += 1
if count == 0:
    print('oui')
zeroVal = 3


count = 0

modSym = (2**((np.arange(10) * 12) % (pSym-1)) % pSym)
modpShift = (2**((np.arange(10) * 12) % (pShift -1)) % pShift)

pPerm = 613
print(modSym, modpShift)
for s in range(12):

    for n in range(4,11,1):
        print("n = ", n)
        plusMinusBits = np.array([])
        for i in range(1 << n):
            plusMinusBits = np.append(plusMinusBits, intToPlusMinus(i, n))
        plusMinusBits = plusMinusBits.reshape((2**n, n))

        for comb in nChooseK(10, n):
            if len(np.unique(comb)) == len(comb):
            #print(comb, 2**(12*comb % 22) % 23)
                indices = (12 * comb) + s
                for signs in plusMinusBits:
                    modSym = (2**(indices % (pSym-1)) % pSym * signs)
                    modpShift = (2**(indices % (pShift -1)) % pShift) * signs
                    modPerm= [pow(2, int(i), pPerm) for i in indices] * signs
                    if np.sum(modSym) % pSym == 0 and np.sum(modpShift) % pShift == 0 and np.sum(modPerm) % pPerm == 0:
                        #if signs[0] > 0:
                        print(indices * signs, modPerm.sum())#, comb, s, mod23, modpShift, np.sum(modpShift)) #print(comb, 2**(indices % 22) % 23, signs, ((indices) * signs))
                        count += 1
print(count)
pairs = []
perms = []
for perm in permutations(range(10), 2):
    perms.append(perm)
for d in range(4):
    perms.append([120 + d, 124 + d])

for p1 in perms:
    for p2 in perms:
        pairs.append(([sorted(p1),sorted(p2)]))
pairs = np.unique(pairs, axis=0)

#pairs = pairs.reshape((len(pairs)//2, 2))
print(pairs.shape)
if True:
    plusMinusBits = np.array([])
    for i in range(1 << 4):
        plusMinusBits = np.append(plusMinusBits, intToPlusMinus(i, 4))
    plusMinusBits = plusMinusBits.reshape((2**4, 4))
    for s1 in range(12):
        for s2 in [number for number in range(0,12) if number != s1]:
            for comb in pairs:
                indices = [0,0,0,0]
                indices[0], indices [1] = (comb[0] * (1 if comb[0][0] >= 120 else 12)) + s1
                indices[2], indices [3] = (comb[1] * (1 if comb[0][0] >= 120 else 12)) + s2
                if indices[0] == indices[2] or indices[1] == indices[3]:
                    continue
                indices = np.array(indices)
                for signs in plusMinusBits:
                    modSym = (2**(indices % (pSym-1)) % pSym * signs)
                    modpShift = (2**(indices % (pShift -1)) % pShift) * signs
                    modPerm= [pow(2, int(i), pPerm) for i in indices] * signs
                    if np.sum(modSym) % pSym == 0 and np.sum(modpShift) % pShift == 0 and np.sum(modPerm) % pPerm == 0:
                        #if signs[0] > 0:
                        print(indices * signs, modPerm.sum())#, comb, s, mod23, modpShift, np.sum(modpShift)) #print(comb, 2**(indices % 22) % 23, signs, ((indices) * signs))
