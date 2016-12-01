<?php
include_once('geoPHP.inc');
$geom = geoPHP::load(file_get_contents("test1.xml"), "georss");
print $geom->out('geojson');
