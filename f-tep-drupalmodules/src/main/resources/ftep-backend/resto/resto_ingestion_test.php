<?php

//  curl -k "https://localhost/geoserver/34556f6a-9790-11e6-a108-005056011b7b/wms?service=WMS&version=1.1.0&request=GetMap&layers=34556f6a-9790-11e6-a108-005056011b7b:FTEP_S2_NDVI_B4_B8_20161021_131424Z&styles=&bbox=-180,-90,90,180&width=640&height=480&format=application%2Fatom%20xml" | xmllint --format - >
//
$jobid="34556f6a-9790-11e6-a108-005056011b7b";
$image="FTEP_S2_NDVI_B4_B8_20161021_131424Z";
$RESTO_USER="admin";
$RESTO_PWD="77137394cabe5c2d09c6f2eabd8f9136";
$RESTO_COLLECTION="ftep";
$RESTO_ENDPOINT="https://192.168.3.83/resto/collections";

define('ENCODE_SECRET','enigma');

require __DIR__ . '/vendor/autoload.php';

use \Curl\Curl;
use pastuhov\Command\Command;

function encode($jid, $fname ){
    // $mysecretkey = "secret";
    $jid="e1c1031e-a04e-11e6-ab70-005056011b7b";
    $fname="FTEP_S2_NDVI_B4_B8_20161101_161937Z.tif";
    //$secret="enigma";
    //$path = $jid."/".$fname."/".$secret;
    $path_url = $jid."/".$fname;
    $path = $path_url."/".ENCODE_SECRET;

    echo "--> ".$path." <--\n";

    // $b64 = base64_encode(md5($jid."/".$fname."/".$secret,true) ); // $mysecretkey.$path.$expiry,true));
    $b64 = base64_encode(md5($path,true) ); // $mysecretkey.$path.$expiry,true));
    $b64u = rtrim(str_replace(array('+','/'),array('-','_'),$b64),'=');
  
    //$url =  sprintf("https://localhost/files2/%s/%s", $b64u, $path_url);
    $url =  sprintf("/files2/%s/%s", $b64u, $path_url);
    echo "=>$url<=\n";

    //return $b64u;
    return $url;

    //$expiry = strtotime("+1 hour");
    //$b64 = base64_encode(md5($mysecretkey.$path.$expiry,true));
    //$b64u = rtrim(str_replace(array('+','/'),array('-','_'),$b64),'=');
    //echo "rtmp://sample.com/$path?e=$expiry&st=$b64u\n";
}




$curl = new Curl();
$url_params=array();
$url_params['service']='WMS';
$url_params['version']='1.1.0';
$url_params['request']='GetMap';
$url_params['layers']=sprintf("%s:%s", $jobid,$image);
$url_params['bbox']='-180,-90,90,180';
$url_params['width']=640;
$url_params['height']=480;
$url_params['format']='application/atom xml';

$query_string = http_build_query($url_params);

$url=sprintf("https://localhost/geoserver/%s/wms?%s", $jobid, $query_string);

$curl = new Curl();
$curl->setOpt(CURLOPT_SSL_VERIFYHOST, false);
$curl->setOpt(CURLOPT_SSL_VERIFYPEER, false);

$curl->get($url);
if ($curl->error) {
    echo 'Error: ' . $curl->errorCode . ': ' . $curl->errorMessage;
    die;
} 
// echo 'Response:' . "\n"; var_dump($curl->response);
// echo 'Response:' . "\n". $curl->response->asXML();

$tmpfile = tempnam(sys_get_temp_dir(), $jobid );
// echo $tmpfile;
$handle = fopen($tmpfile, "w");
fwrite($handle, $curl->response->saveXML());
fclose($handle);

$geojson =  sprintf("%s.json",$tmpfile);

try{
    $output = Command::exec(
        'ogr2ogr -f GeoJSON {output} {input} ',
        [
            'output' => $geojson,
                'input' => $tmpfile
            ]
        );

    $result = json_decode(file_get_contents($geojson));
    $feature = $result->features[0];
    $feature->properties->id=$image;
    $feature->id=$image;

    $fname_parts=explode("_",$image);

    switch($fname_parts[1]){
        case 'S2': {
            $satellite = "SENTINEL-2";
            break;
        }
        default: {
            $satellite = "UNKOWN";
        }
    }

    $id=$feature->properties->id;

    $feature->properties->title = "";
    //$feature->properties->title = "FTEP Product";
    $feature->properties->author_name = "FTEP";
    //$feature->properties->link_href = "https://192.171.139.83/secure/api/v1.0/download?j=$jid&f=$id.tif&xxx=123";
    $feature->properties->content = "XXX";
    $feature->properties->content_type = "image/tiff";

    $feature->properties->satellite ="satellite";
    $feature->properties->sensorMode = "sensorMode";
    $feature->properties->quicklook ="quicklook";
    $feature->properties->wms ="wms";
    //$feature->properties->resource =  "https://192.171.139.83/secure/api/v1.0/download?j=$jid&f=$id.tif";
    $feature->properties->resource =  "https://192.171.139.83/".encode($jid, $id );
    $feature->properties->resourceMimeType = "image/tiff";
    $feature->properties->resourceSize = "333";
    $feature->properties->jobid = "232323";


    # a    ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/L1C_MSI/2016/01/14/S2A_OPER_PRD_MSIL1C_PDMC_20160114T225642_R083_V20160114T164908_20160114T164908.zip
    # #https://192.171.139.83/secure/api/v1.0/download?j=507&f=FTEP_S2_NDVI_B4_B8_20161031_150901Z.tif

    // These params are defined in the FTEP_MODEL class
    $feature->properties->satellite = $satellite;
    $feature->properties->productId = $image;
    // $feature->properties->imageUrl  = "http://repubblica.com";

 //   die("<pre>".var_export($feature,true));

} catch(\Exception $e){
    die($e->getMessage());
} finally {
    unlink($tmpfile);
 //   unlink($tmpfile.".json");
}

echo "\n$tmpfile.json\n";



# curl -i  -k -X POST  -d @resource_FTEP_S2_NDVI_B4_B8_20160630_101018Z.json  https://admin@192.168.3.83/resto/collections/ftep -u admin:77137394cabe5c2d09c6f2eabd8f9136
$payload = json_encode($feature);

// die("<pre>".var_export($payload));

$curl = new Curl();
$curl->setOpt(CURLOPT_SSL_VERIFYHOST, false);
$curl->setOpt(CURLOPT_SSL_VERIFYPEER, false);
$curl->setHeader('Content-Type', 'application/json');
$url=$RESTO_ENDPOINT."/".$RESTO_COLLECTION;

echo "CALLING : ".$url."\n";
$credentials=array("username"=> $RESTO_USER, "password"=> $RESTO_PWD);

// $curl->buildPostData($payload);
$curl->setBasicAuthentication($RESTO_USER, $RESTO_PWD);
$curl->verbose();
$response = $curl->post($url, $payload );
if ($curl->error) {
    echo 'Error: ' . $curl->errorCode . ': ' . $curl->errorMessage;
    print_r($response);
    die;
} else {
    print_r($response);
}


