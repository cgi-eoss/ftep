# yrtap.py
# Target-aligned pixels
# Y. Rauste 2017-10-12
import sys
n = len(sys.argv)
if(n < 3):
    print("Not enough arguments:", n)
    sys.exit(1)
res= float(sys.argv[1])
narg = n - 1

for i in range(narg - 1):
    coord=float(sys.argv[2+i])
    coord2=int(coord/res)*res
    if(i == (narg - 2)):
        print(coord2)
    else:
        print(coord2, end=" ")
