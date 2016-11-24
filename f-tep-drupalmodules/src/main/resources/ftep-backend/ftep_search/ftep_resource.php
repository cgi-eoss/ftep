<?php
require_once 'ftep_exception.php';

use Tobscure\JsonApi\Document;
use Tobscure\JsonApi\Collection;
use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\Resource;
use Tobscure\JsonApi\EmptyResource;
use Tobscure\JsonApi\EmptyCollection;
use Tobscure\JsonApi\Parameters;

use JsonSchema\RefResolver;
use JsonSchema\Uri\UriResolver;
use JsonSchema\Uri\UriRetriever;
use Prophecy\Argument;

require dirname(__FILE__).'/vendor/autoload.php';

use Monolog\Logger;
use Monolog\Handler\StreamHandler;
use Monolog\Handler\RotatingFileHandler;
use Monolog\Processor\WebProcessor;
use Monolog\Processor\MemoryUsageProcessor;
use Monolog\Formatter\LineFormatter;

class FtepResource extends Resource{ 
    protected $_version;
    protected $_endpoint;
    protected $_log;

    public function __construct(){
        $this->_version="1.0";
        $this->_endpoint="api";

        $this->_log = new Logger( basename(__FILE__) );
        $this->_log->pushHandler( new StreamHandler('/var/log/ftep.log', Logger::DEBUG));
        // processor, adding URI, IP address etc. to the log
        $this->_log->pushProcessor(new WebProcessor);
        $this->_log->pushProcessor(new MemoryUsageProcessor);
        
        // $handler = new RotatingFileHandler(storage_path().'/logs/useractivity.log', 0, Logger::INFO);
        /*
         
            // add handler to the logger
            $log->pushHandler($handler);
        // processor, memory usage
        $log->pushProcessor(new MemoryUsageProcessor);
      // custom: I add user id and permission group (by Sentry) to log
      $user = Sentry::getUser();
      $group = $user->getGroups();
      $log->addInfo('USER ID: '.$user->id.' | USER GROUP: '.$group[0]->name.' |');  
         */

        $this->_log->pushProcessor(
            function ($record) {
                $user = $this->getUser();
                $record['extra']['uid'] = $user->uid;
                $record['extra']['name'] = $user->name;
                return $record;
            }
        );

        // $this->_log->debug(__METHOD__." - ".__LINE__);

    }
    public function toArray()
    {
        $array = $this->toIdentifier();

        $array['attributes'] = $this->getAttributes();
        if($array['attributes']==null){
            unset($array['attributes']);
        }

        $relationships = $this->getRelationshipsAsArray();

        if (count($relationships)) {
            $array['relationships'] = $relationships;
        }

        $links = [];
        if (! empty($this->links)) {
            $links = $this->links;
        }
        $serializerLinks = $this->serializer->getLinks($this->data);
        if (! empty($serializerLinks)) {
            $links = array_merge($serializerLinks, $links);
        }
        if (! empty($links)) {
            $array['links'] = $links;
        }

        $meta = [];
        if (! empty($this->meta)) {
            $meta = $this->meta;
        }
        $serializerMeta = $this->serializer->getMeta($this->data);
        if (! empty($serializerMeta)) {
            $meta = array_merge($serializerMeta, $meta);
        }
        if (! empty($meta)) {
            $array['meta'] = $meta;
        }

        return $array;
    }

    protected function getFields($entity,$default="*"){
        $parameters = new Parameters($_GET);
        $fields = $parameters->getFields();
        $fieldNames=$default;
        if(array_key_exists($entity, $fields)){
            if($fields[$entity][0]==="*"){
                $fieldNames=$default;
            }else{
                $fieldNames=join(",", array_values( $fields[$entity] ));
            }
        }
        return $fieldNames;
    }


    protected function dump($data){
        die("<pre>".var_export(array($data),true));
    }

    protected function getUser(){
        global $user;
        return $user;
    }
	  
    protected function getResourceName(){
        return get_class($this);
    }
    protected function getUserCondition(){
        $user = $this->getUser();
        $params=array(":uid"=> $user->uid) ;
        return $params;
    }
    protected function getUrl($data=null){
        global   $base_url;
        //return get_class($this);
        $url = sprintf("%s/%s/v%s/%s%s",$base_url,$this->_endpoint, $this->_version, $this->getResourceName(),is_null($data)?"":"/$data");
        return $url;
        
    }
    public function options($id=null){
        header("Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE, HEAD");         
        // header("Access-Control-Allow-Headers: *");
        header('Access-Control-Allow-Credentials: true');
        header('Access-Control-Allow-Headers: content-type');
        header('Access-Control-Allow-Origin: '.$_SERVER['HTTP_ORIGIN']);
        exit(0);
    }
    protected function writeResponse($data,$code=200,$msg=null){
        http_response_code($code);
        //header("HTTP/1.1 $code $msg");
        header("Content-Type: application/vnd.api+json");
        //$result=array('links'=>array('self'=>$this->getUrl()), $data);
        $result=array('links'=>array('self'=>$this->getUrl()), 'data'=>$data);
        return $result;
    }
    protected function writeJsonApiResponse($data, $serializer, $id=null, $code=200, $relationships=null, $fields=null){
        // Create a new collection of posts, and specify relationships to be included.
        if( ! $data instanceOf Resource) {
            $collection = (new Collection($data, $serializer));
            if(!is_null($relationships)){
                // $collection->with($data[0]->service);
                //$collection->with('service.name12');
                //$collection->with('service');
                $collection->with($relationships);
                $collection->fields($fields);
            }
        } else{
            $collection = $data;
        }
        // Create a new JSON-API document with that collection as the data.
        $document = new Document($collection);

        http_response_code($code);

        // Add metadata and links.
        $document->addMeta('total', count($data));
        $document->addLink('self', $this->getUrl( $id ));
        //$document->addPaginationLinks(
        //    'url', // The base URL for the links
        //    [],    // The query params provided in the request
        //    40,    // The current offset
        //    20,    // The current limit
        //    100    // The total number of results
        //);
//  die("<pre>".var_export(array($data,$serializer,$id,$code,$relationships, $document),true));
        return $document;
    }
    protected function getSection($type){
        $data = endpoint_request_data();
        if(! array_key_exists("data", $data) ){
            throw new Exception("Invalid payload. Missing 'data' element");
        }
        $data=$data->data;
        if(!$data) {
            throw new Exception("Invalid payload");
        }
        if(!is_array($data)){
            if($data->type != $type  ){
                throw new Exception("Endpoint '".$this->getResourceName() ."' - Cannot find section ".$data->type." in payload.");
                //Operation not supported for ".$data->type." - ".$this->getResourceName(). " - $type");
            }
            if(! array_key_exists("attributes", $data) ){
                throw new Exception("Invalid payload");
            }
            $data = $data->attributes;
        } 
        // data is an array, i.e. a POST of multiple objects on a relationshp
        return $data;
    }
}
