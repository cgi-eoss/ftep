<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\Collection;
use Tobscure\JsonApi\Resource;

require dirname(__FILE__).'/../vendor/autoload.php';

class UserSerializer extends AbstractSerializer {
    protected $type = 'user';
    protected $kind;

    public function __construct($kind=null){
        $this->kind = $kind;
    }
    public function getAttributes($user, array $fields = null) {
        if($this->kind==='relation'){
            return null;
        }
        $res =  [ ];
        $properties=[
            'userid'  => 'userid',
            'mail'  => 'mail',
        ];
        foreach($properties as $k=>$v){
            if( property_exists($user,$k) ){
                $res[$k] = $user->$v;
            }
        }
        return $res;
    }
    public function getId($obj) {
 //       if($this->kind=='relation'){ // die("<pre>".var_export($obj,true)); //$obj=$obj->user[0]; }
        return $obj->uid;
    }
}
