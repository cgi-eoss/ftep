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
require dirname(__FILE__).'/serializers/ProjectSerializer.php';



class FtepResourceProject extends FtepResource {
    
    public function __construct(){
        parent::__construct();
    }
    public function getResourceName(){
        return "projects";
    }
    public function getEntityName(){
        return "ftep_project";
    }
    public function read($id=null){
        $user = $this->getUser();
        $params=array(":uid"=> $user->uid) ;

        $jid="";
        if( !empty( $id) ){
            $jid=" AND (pid=:pid) ";
            $params[ ':pid' ] = $id;
        }
        $opts=array();
        $fieldsAr=array('pid','name','description');
        $fields=join(",", $fieldsAr);
        $result = db_query( 
            sprintf("SELECT %s  ".
            " FROM {ftep_project} ".
            " WHERE (uid=:uid) %s" ,$fields, $jid), 
            $params, 
            $opts
        );
        $res = $result->fetchAll();
        return $this->writeJsonApiResponse($res, new ProjectSerializer, $id);
        
    }
    public function delete($id){
        $user = $this->getUser();
        $num_deleted = db_delete( 'ftep_project' )
            ->condition( 'pid', $id)
            ->condition( 'uid', $user->uid)
            ->execute();
        http_response_code(204);
    }


    public function write(){
        $user = $this->getUser();
        $data = $this->getSection('projects');
        $obj = array(
            'name' => $data->name,
            'description' => $data->description,
            'uid' => $user->uid,
        );

        try{ 
            $gid = db_insert($this->getEntityName())
                ->fields(array_keys($obj) )
                ->values( $obj )
                ->execute();

            $obj['pid']=$gid;
        }
        catch( Exception $Exception ) {
            $msg = $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
            return $this->writeResponse($msg, 409);
        }
        $resource = new Resource((object)$obj, new ProjectSerializer);
        return $this->writeJsonApiResponse( $resource, null, $gid, 201);

    }
    public function update($id){
        $user=$this->getUser();
        $data = $this->getSection('projects');

        $fields = get_object_vars($data);
        $body_id = null;


        if( !array_key_exists('id', endpoint_request_data()->data)){
                http_response_code(400);
                throw new Exception("Bad format. Expected 'id' in the body");
        } 
        $body_id=endpoint_request_data()->data->id;
        if( $body_id != $id ){
                http_response_code(400);
                throw new Exception("Bad format. Different values for 'id' ");
        } 
        try{
            // Todo validare cambpi
            $query= db_update($this->getEntityName())
                ->fields( $fields )
                ->condition( 'uid' , $user->uid ,'=') 
                ->condition( 'pid' , $id ,'=') ;

            $nid = $query->execute();

            if($nid==0){
                http_response_code(404);
                return;
            }
        }
        catch( PDOException $Exception ) {
            http_response_code(409);
            return  $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
        }
        return $this->read($body_id);
    }
}
