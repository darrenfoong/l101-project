#!/usr/bin/python

import sys
import matplotlib.pyplot as plt
import numpy as np

fname = sys.argv[1]
f = open(fname, "r")
lines = f.readlines()

x = []
y = []

for line in lines[1:]:
  linesplit = line.split("\n")[0].split(" ")
  x.append(float(linesplit[0]))
  y.append(float(linesplit[1]))

x.append(0)
y.append(0)

plt.plot(x,y)
plt.show()

x.append(1)
y.append(0)

# https://stackoverflow.com/a/451482

def area(p):
    return 0.5 * abs(sum(x0*y1 - x1*y0
                         for ((x0, y0), (x1, y1)) in segments(p)))

def segments(p):
    return zip(p, p[1:] + [p[0]])

print area(zip(x,y))
