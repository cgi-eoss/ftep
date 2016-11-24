<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\Collection;
use Tobscure\JsonApi\Resource;

require dirname(__FILE__).'/../vendor/autoload.php';

class JobOutputSerializer extends AbstractSerializer {
    protected $type = 'file';
    public function getAttributes($job, array $fields = null) {
        $res =  [ ];
        $properties=[
            'fname'  => 'fname',
            'link'  => 'link',
            'size'  => 'size',
        ];
        foreach($properties as $k=>$v){
            if( property_exists($job,$k) ){
                $res[$k] = $job->$v;
            }
        }
        return $res;
    }
    public function getId($obj) {
        return $obj->jobid;
    }
}
