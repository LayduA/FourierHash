orders = [3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53]
def order2(n):
    ord = 1
    acc = 2
    while acc % n != 1:
        acc *= 2
        ord += 1
    return ord
def order12(n):
    ord = 1
    acc = 12
    while acc % n != 0:
        acc += 12
        ord += 1
    return ord

print([order12(i) for i in [order2(n) for n in orders]], [order2(n) for n in orders])