# Orbit of a Sentinel-1 image (from manifest.safe)
# Y. Rauste 2018-01-15
import sys
import xml.etree.ElementTree as ET
ns = { 'safe': 'http://www.esa.int/safe/sentinel-1.0' }
tree=ET.parse(sys.argv[1])
root=tree.getroot()
e=root.findall("metadataSection/metadataObject/metadataWrap/xmlData/safe:orbitReference/safe:orbitNumber", \
namespaces=ns)
orbit=int(e[0].text)
print(orbit)