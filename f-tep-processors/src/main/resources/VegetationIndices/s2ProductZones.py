import xml.etree.ElementTree as ET
import sys

GRANULE_XML_FILES = sys.argv[1:]

epsgs = set([ET.parse(GRANULE).getroot().find('.//HORIZONTAL_CS_CODE').text for GRANULE in GRANULE_XML_FILES])

print(' '.join(epsgs))
