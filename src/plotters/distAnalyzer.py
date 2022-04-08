import csv
import numpy as np


path = "src/data/distancesFC128DD"
for pathEnd in ["MANRH04r.csv", "SIGRH04.csv", "BELRH04.csv"]:
    print(pathEnd)
    with open(path + pathEnd, newline='') as csvfile:
        csvReader = csv.reader(csvfile, delimiter=",")
        distances = np.array(next(csvReader), dtype=np.float64)

    print("Avg dist : ", np.mean(distances), ", standard dev:", np.std(distances), 
            ", percentage > 0.5:", 100 * np.sum(distances > 0.5) / len(distances),
            "%,  > 0.4:", 100 * np.sum(distances > 0.4) / len(distances),
            "%,  > 0.3:", 100 * np.sum(distances > 0.3) / len(distances), "%")
    avgBits = np.zeros(128)
    mins = np.zeros(128)
    for i in range(128):
        avgBits[i] += np.mean(distances[i::128])
        mins[i] += np.min(distances[i::128])

    print("Highest mean bit : ", np.max(avgBits), ", at index ", np.argmax(avgBits))
    print("Highest min bit : ", np.max(mins), "at index ", np.argmax(mins))