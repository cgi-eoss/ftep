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
require dirname(__FILE__).'/serializers/GroupSerializer.php';
require dirname(__FILE__).'/serializers/UserSerializer.php';



class FtepResourceGroup extends FtepResource {
    
    public function __construct(){
        parent::__construct();
    }
    public function getResourceName(){
        return "groups";
    }
    public function getEntityName(){
        return "ftep_group";
    }
    public function read($id=null,$relationship=null,$relName=null){
        $user = $this->getUser();
        $params=array(":uid"=> $user->uid) ;
        $parameters = new Parameters($_GET);

	$jid="";
	// changed is_null to NULL due to some
	// error in the dev environment
        if(!$id==NULL){
            $jid=" AND (gid=:gid) ";
            $params[ ':gid' ] = $id;
        }
        $fields = $this->getFields('group','{ftep_group}.*');

        $opts=array();
        $sql= sprintf(
                "SELECT gid,uid,%s  ".
                " FROM {ftep_group} ".
                "WHERE (uid=:uid) %s" ,$fields, $jid
            ); 
        $result = db_query( $sql, $params, $opts);
        $res = $result->fetchAll();
    
        $relationship = $relName;
        $include = $parameters->getInclude(['user']);
        if($include or $relationship){
            $fieldNames = $this->getFields('user','{ftep_group_member}.*}');
            foreach($res as $k=>$v){
                $sql=sprintf(
                        "SELECT {ftep_group_member}.uid,%s ".
                        " FROM {ftep_group} ".
                        " INNER JOIN {ftep_group_member} ON ( {ftep_group}.gid={ftep_group_member}.group_id )".
                        "WHERE ({ftep_group}.gid=:gid) ", $fieldNames);
                $resx=db_query($sql , array(':gid' => $res[$k]->gid ));
//              die("<pre>".var_export(array($res,$resx,$sql, $res[$k]->gid ),true));
                if(!is_null($relationship)){
                    $res[$k]=(object)array('uid'=>$res[$k]->uid);
                }
               //die("<pre>".var_export(array($res,$resx->fetchAll() ),true));
                $res[$k]->user=$resx->fetchAll();
            }

            // die("<pre>".var_export($res,true));
            if($relationship){
                $y=new UserSerializer('relation');
                //die("<pre>".var_export($res,true));
                if(count($res[0]->user)==1){
                    $x1 = (new Collection($res[0]->user, $y));
                } else {
                    $x1 = (new Collection($res[0]->user, $y));
                }
                $document = new Document($x1);
                return $document;
            } else {
                $relationship = $include;
            }
        }
        return $this->writeJsonApiResponse($res, new GroupSerializer, $id, 200, $relationship);
    }

    public function delete($id){
        $user = $this->getUser();
        $num_deleted = db_delete( 'ftep_group' )
            ->condition( 'gid', $id)
            ->condition( 'uid', $user->uid)
            ->execute();
        http_response_code(204);
    }

    //public function update($id){
    public function update($id=null,$relationship=null,$relName=null){
        $user=$this->getUser();
        // die("<pre>".var_export(array($id,$relationship, $relName),true));
        if(!is_null($relationship)){
            $relationships=array("users"=>array("table"=>"{ftep_user}", "update_query"=>"") );
            if( ! in_array($relName, array_keys($relationships) ) ){
                throw new Exception("Unsupported relationship: ".$relName);
            }
            $data=endpoint_request_data();
            foreach($data as $k=>$v){
                // update drupal_ftep_member SET 
                // INSERT INTO drupal_ftep_group_member 
                $query= db_update( "ftep_group" )
                    ->fields( array('uid'=>$v) )
                    ->condition( 'uid' , $user->uid ,'=') 
                    ->condition( 'gid' , $id ,'=') ;
            die("<pre>".var_export(array($query),true));
            }
            die("<pre>".var_export(array($data),true));

        
            return ;
        } 


        $data = $this->getSection('groups');

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
                ->condition( 'gid' , $id ,'=') ;

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
   public  function write(){
        $user = $this->getUser();
        $data = $this->getSection('groups');
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

            $obj['gid']=$gid;
        }
        catch( Exception $Exception ) {
            $msg = $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
            return $this->writeResponse($msg, 409);
        }
        $resource = new Resource((object)$obj, new GroupSerializer);
        return $this->writeJsonApiResponse( $resource, null, $gid, 201);

    }
}
