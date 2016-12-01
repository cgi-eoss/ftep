<?php
define('GEOSERVER_URL',"http://192.168.3.85:8080/geoserver/");
define('GEOSERVER_CREDENTIALS',"admin:geoserver");

function createWorkspace($workspaceName){
    // Open log file
    $logfh = fopen("GeoserverPHP.log", 'w') or die("can't open log file");

    // Initiate cURL session
    // $service = "http://192.168.3.85:8080/geoserver/";
    $service = GEOSERVER_URL;
    $request = "rest/workspaces"; // to add a new workspace
    $url = $service . $request;
    $ch = curl_init($url);

    // Optional settings for debugging
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true); //option to return string
    curl_setopt($ch, CURLOPT_VERBOSE, true);
    curl_setopt($ch, CURLOPT_STDERR, $logfh); // logs curl messages

    //Required POST request settings
    curl_setopt($ch, CURLOPT_POST, True);
    curl_setopt($ch, CURLOPT_USERPWD, GEOSERVER_CREDENTIALS);

    //POST data
    curl_setopt($ch, CURLOPT_HTTPHEADER, array("Content-type: application/xml"));

    $xmlStr = sprintf("<workspace><name>%s</name></workspace>",$workspaceName);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $xmlStr);

    //POST return code
    $successCode = 201;

    $buffer = curl_exec($ch); // Execute the curl request

    // Check for errors and process results
    $info = curl_getinfo($ch);
    if ($info['http_code'] != $successCode) {
        $msgStr = "# Unsuccessful cURL request to ";
        $msgStr .= $url." [". $info['http_code']. "]\n";
        fwrite($logfh, $msgStr);
    } else {
        $msgStr = "# Successful cURL request to ".$url."\n";
        fwrite($logfh, $msgStr);
    }
    fwrite($logfh, $buffer."\n");

    curl_close($ch); // free resources if curl handle will not be reused
    fclose($logfh);  // close logfile

}


function createCoverage($workspaceName,$filename){
    // Open log file
    $logfh = fopen("GeoserverPHP.log", 'w') or die("can't open log file");

    // Initiate cURL session
    // $service = "http://192.168.3.85:8080/geoserver/";
    $service = GEOSERVER_URL;
    $request = "rest/workspaces"; // to add a new workspace
    $url = $service . $request."/".$workspaceName."/coveragestores/FTEP_S2_NDVI_B4_B8_20160929_152102Z-2/external.geotiff";
    $ch = curl_init($url);

    // Optional settings for debugging
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true); //option to return string
    curl_setopt($ch, CURLOPT_VERBOSE, true);
    curl_setopt($ch, CURLOPT_STDERR, $logfh); // logs curl messages
    curl_setopt($ch, CURLOPT_USERPWD, GEOSERVER_CREDENTIALS);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array("Content-type: text/plain"));

    // Using a PUT method i.e. -XPUT
    curl_setopt($ch, CURLOPT_PUT, true);
    $putData = "file:///data/cache/Job_ded9fec8-8657-11e6-b994-005056011b7b/outDir/FTEP_S2_NDVI_B4_B8_20160929_152102Z.tif";
    $fh = fopen("php://memory", "rw");
    fwrite($fh, $putData);
    rewind($fh);
    // fseek($fh,0);

    // Instead of POST fields use these settings
    curl_setopt($ch, CURLOPT_INFILE, $fh);
    curl_setopt($ch, CURLOPT_INFILESIZE, strlen($putData));

    //POST return code
    $successCode = 201;

    $buffer = curl_exec($ch); // Execute the curl request

    // Check for errors and process results
    $info = curl_getinfo($ch);
    if ($info['http_code'] != $successCode) {
        $msgStr = "# Unsuccessful cURL request to ";
        $msgStr .= $url." [". $info['http_code']. "]\n";
        fwrite($logfh, $msgStr);
    } else {
        $msgStr = "# Successful cURL request to ".$url."\n";
        fwrite($logfh, $msgStr);
    }
    fwrite($logfh, $buffer."\n");

    curl_close($ch); // free resources if curl handle will not be reused
    fclose($logfh);  // close logfile

}

$jobid="4ee89d9c-8675-11e6-bd18-005056011b7b";
createWorkspace($jobid);
createCoverage($jobid,'xx');

/*
^Crl -u admin:geoserver -v -XPUT -H 'Content-type: text/plain'  -d file:///data/cache/Job_ded9fec8-8657-11e6-b994-005056011b7b/outDir/FTEP_S2_NDVI_B4_B8_20160929_152102Z.tif   "http://192.168.3.85:8080/geoserver/rest/workspaces/ftepApiUser/coveragestores/FTEP_S2_NDVI_B4_B8_20160929_152102Z-2/external.geotiff"
 */
#!/usr/bin/php
?>
