<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\Collection;

require dirname(__FILE__).'/../vendor/autoload.php';

class GroupSerializer extends AbstractSerializer {
    protected $type = 'groups';
    protected $withId=false;

    public function getAttributes($obj, array $fields = null) {
        $res =  [ ];
        $properties=[
            'name'  => 'name',
            'description'  => 'description',
        ];
        foreach($properties as $k=>$v){
            if( property_exists($obj,$k) ){
                $res[$k] = $obj->$v;
            }
        }
        return $res;
    }
    public function getId($post) {
        return $post->gid;
    }
    public function user($obj){
        if(array_key_exists('user',$obj)){
            $element = new Collection($obj->user, new UserSerializer);
            return new Relationship($element);
        }
    }
}
