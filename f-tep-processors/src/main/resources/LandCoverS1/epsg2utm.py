import csv
import re
import sys

EPSG_DB = '/opt/OTB-5.8.0-Linux64/share/epsg_csv/pcs.csv'

EPSG = sys.argv[1]


def wanted_epsg(row):
    return row['COORD_REF_SYS_CODE'] == EPSG


with open(EPSG_DB) as csvfile:
    rows = filter(wanted_epsg, csv.DictReader(csvfile))

    if len(rows) == 0:
        raise RuntimeError("No row in EPSG database matching '%s'" % EPSG)
    elif len(rows) > 1:
        raise RuntimeError("Multiple rows in EPSG database matching '%s'" % EPSG)

    epsg_name = rows[0]['COORD_REF_SYS_NAME']
    m = re.search('UTM zone (\\d+[NS])\\b', epsg_name)

    if m is None:
        raise RuntimeError("Could not match 'UTM zone ###' from '%s'" % epsg_name)

    utm_zone = m.group(1)
    print(utm_zone)
