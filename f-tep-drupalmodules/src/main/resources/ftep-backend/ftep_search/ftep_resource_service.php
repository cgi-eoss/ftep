<?php
require_once dirname(__FILE__)."/ftep_resource.php";


use Tobscure\JsonApi\Document;
use Tobscure\JsonApi\Collection;
use Tobscure\JsonApi\AbstractSerializer;
use Tobscure\JsonApi\Relationship;
use Tobscure\JsonApi\Resource;
use Tobscure\JsonApi\EmptyResource;
use Tobscure\JsonApi\EmptyCollection;
use Tobscure\JsonApi\Parameters;
require dirname(__FILE__).'/vendor/autoload.php';
require dirname(__FILE__).'/serializers/ServiceSerializer.php';



class FtepResourceService extends FtepResource {
    
    public function __construct(){
        parent::__construct();
    }
    public function getResourceName(){
        return "services";
    }
    public function getEntityName(){
        return "ftep_service";
    }

    public function read($id=null){
        $user = $this->getUser();
        $params=array(":sid"=> $id) ;
        $opts=array('fetch'=>PDO::FETCH_ASSOC);
        $opts=array();
        if(is_null($id)){
            $result = db_query( sprintf("SELECT *, '%s' as type FROM {%s}", $this->getResourceName(),$this->getEntityName()), $opts);
        } else {
            $result = db_query( sprintf("SELECT *, '%s' as type FROM {%s} WHERE (sid=:sid) ", $this->getResourceName(),$this->getEntityName()) , $params, $opts );
        }
        $res = $result->fetchAll();
        return $this->writeJsonApiResponse($res, new ServiceSerializer, $id);
    }

    function write(){
        $user = $this->getUser();
        $data = endpoint_request_data();
        $data = $this->getSection('service');
        $obj=array(
            'name' => $data->name,
            'description' => $data->description,
            'kind'=> $data->kind,
            'mode'=> $data->mode,
            'rating'=> $data->rating,
            'access_level'=> $data->accessLevel,
            'cpu'=> $data->cpu,
            'ram'=> $data->ram,
            'cost'=> $data->cost,
            'license'=> $data->license,
            'user_id' => $user->uid,
        );

        try{
            $nid = db_insert( $this->getEntityName() )
                ->fields( array_keys($obj) )
                ->values($obj)
                ->execute();
            $obj['sid'] =$nid;
        }
        catch( PDOException $Exception ) {
            // Note The Typecast To An Integer!
            $msg = $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
            return $this->writeResponse($msg, 409);
        }
        $resource = new Resource((object)$obj, new ServiceSerializer);
        return $this->writeJsonApiResponse( $resource, null, $nid, 201);
    }


    public function update($id){
        $user=$this->getUser();
        $data = endpoint_request_data();
        try{
            // Todo validare cambpi
            $query= db_update('ftep_service')
                ->fields( get_object_vars($data) )
                ->condition( 'uid' , $user->uid ,'=')
                ->condition( 'jid' , $id );
            $nid = $query->execute();
        }
        catch( PDOException $Exception ) {
            // Note The Typecast To An Integer!
            http_response_code(409);
            return  $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
        }
        $url=$this->getUrl();
        $res=array(
                'data'=>array(  
                    'type' => 'services',
                    'id' => $nid,
                    'attributes' => $data,
                    ),
                'link'=> array( 
                    'self' => $url
                    )
                );
        return $this->writeResponse($res,201);
    }

}
