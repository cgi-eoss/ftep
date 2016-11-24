<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;

require dirname(__FILE__).'/../vendor/autoload.php';

class LoginSerializer extends AbstractSerializer {
    protected $type = 'login';

    public function getAttributes($job, array $fields = null) {
        $rs = [
            'sessionId'  => $job->sessid,
            'sessionName'  => $job->session_name,
            'token'  => $job->token,
        ];
        return $rs;
    }
    public function getId($post) {
        //return $post->sessid;
        return 0;
    }
}
