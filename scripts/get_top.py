#!/usr/bin/python

import sys

NUM = int(sys.argv[1])

f = open("../data/bnc/all.num", "r")
output = open("../data/bnc/top"+str(NUM), "w")

wlist = []

f.readline()

while len(wlist) < NUM:
  line = f.readline()
  word = line.split(" ")[1]
  if word not in wlist:
    wlist.append(word)
    output.write(word+"\n")
  else:
    print "Dupe: " + word

output.close()
f.close()
