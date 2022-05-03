orders = [3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53]
def orderMultx(x, n):
    ord = 1
    acc = x
    while acc % n != 1:
        acc *= x
        ord += 1
    return ord
def orderAddx(x, n):
    ord = 1
    acc = x
    while acc % n != 0:
        acc += x
        ord += 1
    return ord

print(orderMultx(2, 613))