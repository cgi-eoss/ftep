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
require_once dirname(__FILE__).'/vendor/autoload.php';


require_once dirname(__FILE__).'/serializers/UserSerializer.php';


class FtepResourceUser extends FtepResource {
    
    public function __construct(){
        parent::__construct();
    }
    public function getResourceName(){
        return "users";
    }
    public function getEntityName(){
        return "ftep_group_member";
    }

    public function read($id=null){
        $parameters = new Parameters($_GET);
        $user = $this->getUser();
        $params=array();
        $params[':uid' ] = $user->uid;

        $jid="";
        if(!is_null($id)){
            // Going for a single item
            $jid=" AND (uid=:uid) ";
            $params[':uid' ] = $id;
        }
        $sql= sprintf(
                "SELECT *  ".
                " FROM {ftep_group_member} ".
                " INNER JOIN {ftep_group} ON {ftep_group_member}.group_id = {ftep_group}.gid "  .
                // " WHERE ({ftep_group}.uid=:uid) %s ", $jid ) ;
                " WHERE (1=1) %s ", $jid ) ;
        $result = db_query( $sql, $params );
        $res = $result->fetchAll();
        return $this->writeJsonApiResponse($res, new UserSerializer, $id);
    }

    function write(){
        $user = $this->getUser();
        $data = endpoint_request_data();

        $data = $this->getSection('user');
        $obj=array(
            'userid' => $data->userid,
            'mail' => $data->mail,
        );

        try{
            $nid = db_insert( $this->getEntityName() )
                ->fields(array('userid', 'mail'))
                ->values($obj)
                ->execute();
            $obj['uid'] =$nid;
        }
        catch( PDOException $Exception ) {
            // Note The Typecast To An Integer!
            $msg = $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
            return $this->writeResponse($msg, 409);
        }
            $resource = new Resource((object)$obj, new DatabasketSerializer);
            return $this->writeJsonApiResponse( $resource, null, $nid, 201);
    }

    public function update($id){
        $user=$this->getUser();
        $data = $this->getSection('user');

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
                ->condition( 'idb' , $id ,'=') ;

            $nid = $query->execute();

            if($nid==0){
                http_response_code(404);
                return;
            }
        }
        catch( PDOException $Exception ) {
            // Note The Typecast To An Integer!
/*
403 Forbidden

A server MUST return 403 Forbidden in response to an unsupported request to update a resource or relationship.

404 Not Found

A server MUST return 404 Not Found when processing a request to modify a resource that does not exist.

409 Conflict

A server MAY return 409 Conflict when processing a PATCH request to update a resource if that update would violate other server-enforced constraints (such as a uniqueness constraint on a property other than id).
*/
            http_response_code(409);
            return  $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
        }
        return $this->read($body_id);
    }
}
