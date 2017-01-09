<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;

require dirname(__FILE__).'/../vendor/autoload.php';

class ProjectSerializer extends AbstractSerializer {
    protected $type = 'projects';
    protected $withId=false;

    public function getAttributes($obj, array $fields = null) {
        $rs = [
            'name'  => $obj->name,
            'description'  => $obj->description,
        ];
        return $rs;
    }
    public function getId($post) {
        return $post->pid;
    }
}
