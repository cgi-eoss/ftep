<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\Collection;

require dirname(__FILE__).'/../vendor/autoload.php';
// require dirname(__FILE__).'/serializers/FileSerializer.php';


class DatabasketSerializer extends AbstractSerializer {
    protected $type = 'databaskets';

    public function getAttributes($obj, array $fields = null) {
        $res = [];
        $properties = [
          'name' => 'name' ,
          'description' => 'description',
          'acecsslevel' => 'accesslevel'
      ];
        foreach($properties as $k=>$v){
            if( property_exists($obj, $k) ){
                $res[$k] = $obj->$v;
            }
        }
        return $res;
    }
    public function getId($post) {
        return $post->idb;
    }

    /**
     * @return \Tobscure\JsonApi\Relationship
     */
    public function files($obj){
        // if( array_key_exists('file', $obj) ||array_key_exists('files', $obj) ) {
        if( array_key_exists('files', $obj)  ) {
            $element = new Collection( $obj->files, new FileSerializer);
            // die("<pre>".var_export($files,true));
            return new Relationship($element);
        }
    }
}

