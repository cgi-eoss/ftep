<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\Collection;
use Tobscure\JsonApi\Resource;

require dirname(__FILE__).'/../vendor/autoload.php';

class FileSerializer extends AbstractSerializer {
    protected $type = 'files';
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
            'name'  => 'name',
            'url'  => 'url',
            'datasource'  => 'datasource',
            'properties' => 'properties_text',
        ];
        foreach($properties as $k=>$v){
            if( property_exists($user,$k) ){
                if($v=='properties_text'){
                    $res[$k] = json_decode($user->$v);
                } else{
                    $res[$k] = $user->$v;
                }
            }
        }
        return $res;
    }
    public function getId($obj) {
        // die("<pre>".var_export($obj,true));
 //       die("<pre>".var_export($obj,true));
        if($this->kind=='relation'){
            if( property_exists($obj, 'files') && is_array($obj->files) && count($obj->files)>0 ){
                // $obj = $obj->files[0];
  //               $obj = $obj->files;
            }
        }

        if(! property_exists($obj, 'fid') ){
            return;
        }
        return $obj->fid;
        // return $obj->fid;
    }
}
