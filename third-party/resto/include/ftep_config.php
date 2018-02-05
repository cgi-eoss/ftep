<?php

return array(
    'custom'=>array(
        'header' => 'HTTP_EOSSO_PERSON_COMMONNAME',
        'acl_server'=>'http://localhost:8090/secure/api/v2.0/ftepFiles/search/findyType',
        'acl_server'=>'http://localhost:8000/resp.json',
        'path'=>'$._embedded.ftepFiles[*].restoId'
    )
);
