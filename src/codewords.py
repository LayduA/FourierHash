import numpy as np

words = open('src/data/codes/codes/8-4-2688.txt', 'r').readlines()[1:]
words = [[int(digit) for digit in w.split()[:-2]] for w in words]
print(words.__repr__().replace('[', '').replace('], ', '\n')) 