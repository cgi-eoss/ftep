# Unique orbit numbers in a comma-separated list of orbit numbers.
# Author: Y. Rauste 2018-01-15

import sys
if(len(sys.argv) < 2):
    print("No orbit list given")
    sys.exit(1)
a=sys.argv[1]
al=a.split(sep=",")
orbits=sorted(list(set(al)))
for i in orbits:
    print(i, end=" ")
print("")