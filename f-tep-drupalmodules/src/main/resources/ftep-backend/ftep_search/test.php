<?php
$arrContextOptions=array(
        "ssl"=>array(
                    "verify_peer"=>false,
                            "verify_peer_name"=>false,
                                ),
                            );  
$data=file_get_contents('https://localhost/api/v1.0/datasources/getAuthentication/openserch-test.ceda.ac.uk', false, stream_context_create($arrContextOptions));

$j = json_decode($data);
die("<Pre>".var_export($j,true));
//file_put_contents('/tmp/xxx.tar.gz', convert_uudecode($j->
