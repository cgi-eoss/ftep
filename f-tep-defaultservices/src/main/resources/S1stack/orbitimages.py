# orbitimages.py
# Images of orbit in a comma-separated list of Sentinel-1 input files.
# Author: Y. Rauste 2017-09-29

import sys
if(len(sys.argv) < 2):
    print("No orbit number given")
    sys.exit(1)
if(len(sys.argv) < 3):
    print("No image list given")
    sys.exit(1)
a=sys.argv[2]
orbit_r = int(sys.argv[1])
al=a.split(sep=",")
for i in al:
    al_f=i.split(sep="_")
    orbit=int(al_f[6])
    if(orbit == orbit_r):
        print(i, end=" ")
print("")
