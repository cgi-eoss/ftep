import re
import sys
from collections import namedtuple
from decimal import *

POLYGON = sys.argv[1]

m = re.search('POLYGON\\s*\\(\\((.*)\\)\\)', POLYGON)

if m is None:
    raise RuntimeError("Could not parse POLYGON string from '%s'" % POLYGON)

Point = namedtuple('Point', ['longitude', 'latitude'])

# WKT POLYGON strings are always in "long lat" format so Point._make can be used directly
points = [Point._make([Decimal(n) for n in p.strip().split()]) for p in m.group(1).split(',')]

# Simply sort all points and pull the first/last list elements for the bounds
longSorted = sorted(points, key=lambda x:x.longitude)
latSorted = sorted(points, key=lambda x:x.latitude)

northBound = latSorted[-1].latitude
southBound = latSorted[0].latitude
eastBound = longSorted[-1].longitude
westBound = longSorted[0].longitude

print("%s %s %s %s" % (northBound, southBound, eastBound, westBound))
