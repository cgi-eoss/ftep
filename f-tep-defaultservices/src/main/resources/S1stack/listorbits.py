# listorbits.py
# Unique orbit numbers in a comma-separated list of Sentinel-1 input files.
# Author: Y. Rauste 2017-09-29

import sys
if(len(sys.argv) < 2):
    print("No image list given")
    sys.exit(1)
a=sys.argv[1]
al=a.split(sep=",")
orbits = []
for i in al:
    al_f=i.split(sep="_")
    orbit=int(al_f[6])
    orbits.append(orbit)
orbits=sorted(list(set(orbits)))
for i in orbits:
    print(i, end=" ")
print("")
