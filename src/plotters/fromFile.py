from turtle import distance
import numpy as np
import csv
import time
import sys

sys.path.append("./src")
from haar_psi import haar_psi

distances = np.zeros(1280)

i = 0
with open('src/data/FourierCartesian128_DETMod_DET_Phase_MAN_RHOM_04_hashesPair.csv', newline='') as csvfile:
    csvReader = csv.reader(csvfile, delimiter=",")
    start = time.time()
    while i < 1280:
        row = next(csvReader)
        rowFiltered = np.array([int(el.replace('[', '')) for (ind, el) in enumerate(row) if ind > 0 and ']' not in el])
        image1 = np.array([((x >> 16) & 0b11111111, (x >> 8) & 0b11111111, x & 0b11111111) for x in rowFiltered[:256*256]]).reshape((256,256,3))
        image2 = np.array([((x >> 16) & 0b11111111, (x >> 8) & 0b11111111, x & 0b11111111) for x in rowFiltered[256*256:]]).reshape((256,256,3))

        assert(len(image1) == len(image2))
        
        dist = haar_psi(image1, image2)
        distances[i] += dist[0]
        
        if i % 100 == 0:
            print('i = ', i, " time elapsed = ", (time.time() - start), "s")
            start = time.time()
        
        i += 1

with open("src/data/distancesFC128DDMANRH035.csv", 'w') as csvoutputfile:
    csvWriter = csv.writer(csvoutputfile)
    csvWriter.writerow(distances)
    