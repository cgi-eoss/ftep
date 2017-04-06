<?php

use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\Collection;
use Tobscure\JsonApi\Resource;

require dirname(__FILE__).'/../vendor/autoload.php';

class JobSerializer extends AbstractSerializer {
    protected $type = 'jobs';
    protected $withId=false;

    /*
    public function __construct($withId=false){
        $this->withId=
    }
    */
    public function getAttributes($job, array $fields = null) {
        $res =  [ ];
        $properties=[
            'inputs'  => 'inputs',
            'outputs'  => 'outputs',
            'status'  => 'status',
            'step'  => 'step',
            'guiendpoint'  => 'guiEndpoint',
        ];
        foreach($properties as $k=>$v){
            if( property_exists($job,$k) ){
                if( $k=='guiendpoint' and strlen($job->$k)>0) {
                    $job->$k = str_replace( "192.168.3" ,"192.171.139", $job->$k );
                    $fragments = explode(":", $job->$k);
                    $fragments[0]="ftep-wps.eoss-cloud.it";

                    $job->$k = implode(":", $fragments);
                    $res[$v] = "http://".$job->$k;
                }
                else if($k=='inputs' ||  $k=='outputs'){
                    $tmp=json_decode( stripslashes($job->$k) );
                    if($tmp===false || is_null($tmp) ){
                        $tmp=$job->$k;
  //                  file_put_contents('/tmp/debug.txt', var_export( array($job, $k, $job->$k,  json_decode(stripslashes($job->$k) ), $tmp  ),true) , FILE_APPEND);
                    }
                    $res[$v] = $tmp;
                }
                else {
                    $res[$v] = $job->$k;
                }
            }
        }
        return $res;
    }
    public function getId($post) {
        return $post->id;
    }
    public function service($job) {
//        die("<pre>".var_export($job,true));
        if(array_key_exists('service',$job)){
        $element = new Collection($job->service, new ServiceSerializer);
        return new Relationship($element);
        }
    }
    /*
    public function getRelationship($aa,$name){
        $x=new Resource($aa->service,new ServiceSerializer);
        die("<pre>".var_export($x,true));
        return new Relationship($x);
    }
     */
    /*public function projects($job) {
        $element = new Collection($job->projects, new ProjectSerializer);
        return new Relationship($element);
    }*/
}
