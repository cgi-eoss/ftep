<?php
define(DRUPAL_ROOT,'/var/www/ftep/');
require_once DRUPAL_ROOT . '/includes/bootstrap.inc';
require_once DRUPAL_ROOT . '/includes/common.inc';

$bootstrap = drupal_bootstrap(DRUPAL_BOOTSTRAP_DATABASE);
  /*
  foreach( file('/tmp/queue_item.json') as $line){
      echo $line;
      echo json_decode($line);
  }
   */
$jsons[]=<<<EOF
  {"inputs":"{}","outputs":"{}","step":"Step 1of3:Data-Fetch","guiendpoint":"","uid":"164","sid":"17","id":"510","jid":"ee0a2c00-9f8a-11e6-a477-005056011b7b","user":"ftepApiUser"}
EOF;
$jsons[]=<<<EOF
{"inputs":"{inputfile:[ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/L1C_MSI/2016/01/14/S2A_OPER_PRD_MSIL1C_PDMC_20160114T225642_R083_V20160114T164908_20160114T164908.zip],sourceBand2:[B8],sourceBand1:[B4],zone:[15N],resolution:[20]}","outputs":"{}","step":"Step 2of3:Processing","guiendpoint":"","uid":"164","sid":"17","id":"510","jid":"ee0a2c00-9f8a-11e6-a477-005056011b7b","user":"ftepApiUser"}
EOF;
$jsons=<<<EOF
{
"inputs": { "inputfile":[ "ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/dataL1C_MSI/2016/01/14/S2A_OPER_PRD_MSIL1C_PDMC_20160114T225642_R083_V20160114T164908_20160114T164908.zip" ] ,"sourceBand2":["B8"],"sourceBand1":["B4"],"zone":["15N"],"resolution":["20"]},
"outputs": "{ \"Result\": \"/data/cache/Job_ee0a2c00-9f8a-11e6-a477-005056011b7b/outDir/FTEP_S2_NDVI_B4_B8_20161031_165655Z.tif\" }",
"step": "Step 3of3:Output-List",
"guiendpoint": "",
"uid": "164",
"sid": "17",
"id": "510",
"jid": "ee0a2c00-9f8a-11e6-a477-005056011b7b",
"user": "ftepApiUser"
}
EOF;

$x=array(
    'inputs'=> '{ "inputfile":[ "ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/dataL1C_MSI/2016/01/14/S2A_OPER_PRD_MSIL1C_PDMC_20160114T225642_R083_V20160114T164908_20160114T164908.zip" ] ,"sourceBand2":["B8"],"sourceBand1":["B4"],"zone":["15N"],"resolution":["20"]}',
    'outputs'=> '{ "Result": "/data/cache/Job_ee0a2c00-9f8a-11e6-a477-005056011b7b/outDir/FTEP_S2_NDVI_B4_B8_20161031_165655Z.tif" }',
"step"=> "Step 3of3:Output-List",
"guiendpoint"=> "",
"uid"=> "164",
"sid"=> "17",
"id"=> "510",
"jid"=> "ee0a2c00-9f8a-11e6-a477-005056011b7b",
"user"=> "ftepApiUser",
"jobid"=>"510"
);
$queue = DrupalQueue::get('jobs_queue');
$queue->createQueue();
//$x= json_decode($jsons);
$queue->createItem($x);
die;
foreach($jsons as $k=>$v){
    $x= json_decode($v);
    print var_export($x,true);
    $queue->createItem($x);
}

