<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\SerializerInterface;

require dirname(__FILE__).'/../vendor/autoload.php';

class ServiceSerializer extends AbstractSerializer {
    protected $type = 'services';
    protected $kind;

    public function __construct($kind=null){
        $this->kind=$kind;
    }

    public function getAttributes($job, array $fields = null) {
        if($this->kind==='relation'){
            return null;
        }
        $res =  [ ];
        $properties=[
            'name'  => 'name',
            'description'  => 'description',
            'kind'  => 'kind',
            'mode'  => 'mode',
            'rating'  => 'rating',
            'accessLevel'  => 'access_level',
            'cpu'  => 'cpu',
            'ram'  => 'ram',
            'cost'  => 'cost',
            'license'  => 'license',
            'cost'  => 'cost',
            'status'  => 'status',
        ];
        foreach($properties as $k=>$v){
            if( property_exists($job,$k) ){
                $res[$k] = $job->$v;
            }
        }
        return $res;
    }
    public function getId($post) {
        if($this->kind=='relation'){ 
            //die("<pre>".var_export($post,true));
            $post=$post->service[0]; }
        return $post->sid;
    }
//    public function getLinks($aa){
//        return array("XX" => "YY");
//    }
}

