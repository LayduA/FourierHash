import numpy as np

words = open('src/data/8-5-code.txt', 'r').readlines()[1:]
words = [[int(digit) for digit in w.split()[:-2]] for w in words]
print(words.__repr__().replace('[', '{').replace('],', '},\n')) 