import sys
from subprocess import call

from gdalconst import GA_ReadOnly
from osgeo import gdal, osr

jp2 = sys.argv[1]
cloud_msk = sys.argv[2]
output_file = sys.argv[3]

b02_metadata = gdal.Open(jp2, GA_ReadOnly)

# Extract corner coordinates of granule
ulx, xres, xskew, uly, yskew, yres = b02_metadata.GetGeoTransform()
lrx = ulx + (b02_metadata.RasterXSize * xres)
lry = uly + (b02_metadata.RasterYSize * yres)

# Extract EPSG code
srs = osr.SpatialReference(wkt=b02_metadata.GetProjection())
epsg = srs.GetAuthorityCode('PROJCS')

call(['gdal_rasterize',
      '-ot', 'Byte',
      '-a_nodata', '0',
      '-tr', str(abs(xres)), str(abs(yres)),
      '-burn', '255',
      '-te', str(ulx), str(lry), str(lrx), str(uly),
      '-a_srs', "EPSG:" + epsg,
      '-q',
      cloud_msk,
      output_file])

exit(0)
