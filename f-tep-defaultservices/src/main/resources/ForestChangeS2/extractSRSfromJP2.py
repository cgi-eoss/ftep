# extractSRSfromJP2.py
import sys
from subprocess import call
from gdalconst import GA_ReadOnly
from osgeo import gdal, osr

jp2 = sys.argv[1]
metadata = gdal.Open(jp2, GA_ReadOnly)
srs = osr.SpatialReference(wkt=metadata.GetProjection()).GetAuthorityCode('PROJCS')

print(srs)
exit(0)