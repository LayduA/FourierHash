from itertools import permutations
import numpy as np

def nChooseK(n, k):
    arr = [sorted(list(p)) for p in permutations(range(n),k)]
    return list(np.unique(arr, axis = 0))

vals = nChooseK(11,4)

indices = nChooseK(4,2)
signs = []
for ind in indices:
    signs.append([1 if i in ind else -1 for i in range(4)])

# def pow11_4():
#     arr = []
#     for a in range(11):
#         for b in range(11):
#             for c in range(11):
#                 for d in range(11):
#                     arr.append([a,b,c,d])
#     return np.array(arr)
# vals = pow11_4()
# #print(vals)
# cs = []
# count = 0
# for v in vals:
#     for s in signs:
#         if (s * (2 ** (np.array(v) % 22))).sum() % 23 == 0 and (s * (2 ** (np.array(v) % 268))).sum() % 269 == 0:
#             if len(np.unique(v)) == len(v):
#                 count += 1
#             cs.append(v * s)
#             #print(v * s, v, s, (v * s).sum())
# print(np.array(cs), len(cs), count)
# print(cs[20:28])

jaj = nChooseK(128,2)

pSym = 19

jej = []
for j in jaj:
    jej.append([int(j[0]), int(j[1])])
#print(jej, len(jej))
jouj = []
for j in jej:
    #if (2**j)[0] == 0 == (2**j)[1]:
        #print("yopla ", j)
    jouj.append([j[0], j[1], pow(2,j[0],pSym), pow(2, j[1], pSym)])
#print(jouj)
sums = [(j[0], j[1], (j[2] + j[3])) for j in jouj]
sums.sort(key= lambda t:t[2])
colls = {}
for s in sums:
    if s[2] % (pSym) in colls.keys():
        colls.get(s[2] % (pSym)).append((s[0], s[1], s[2]))
    else :
        colls.update({(s[2] % (pSym)): [(s[0], s[1], s[2])]})
#print(len(sums), len(np.unique(sums)))
dangers = [ind for ind in colls.keys() if len(colls.get(ind)) > 1]
intermediate = {}
for d in dangers:
    intermediate.update({d:colls[d]})
#print(intermediate[3])
final = {}
for el in intermediate[3]:
    if (el[0] + el[1]) % 613 in final.keys():
        final.get((el[0] + el[1]) % 613).append((el[0], el[1]))
    else:
        final.update({((el[0] + el[1]) % 613): [(el[0], el[1])]})

keys = {k:v for (k,v) in final.items() if len(v) > 1}
print(keys)