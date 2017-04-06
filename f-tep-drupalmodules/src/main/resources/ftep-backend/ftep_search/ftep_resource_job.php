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
require dirname(__FILE__).'/serializers/JobSerializer.php';
require dirname(__FILE__).'/serializers/JobOutputSerializer.php';


class FtepResourceJob extends FtepResource {
    
    public function __construct(){
        parent::__construct();
    }
    public function getResourceName(){
        return "jobs";
    }
    public function getEntityName(){
        return "ftep_job";
    }
    public function read($id=null,$relationship=null,$relName=null){
        $user = $this->getUser();
        $params=array(":uid"=> $user->uid) ;
        $parameters = new Parameters($_GET);
        $jid="";
        if( !empty( $id) ){
            $jid=" AND (id=:id) ";
            $params[':id' ] = $id;
        }
        $fields = $this->getFields('job','{ftep_job}.*');
        $opts=array();

        $sql= sprintf(
                "SELECT id,{ftep_job}.sid, {ftep_job}.guiendpoint , step, %s,".
                " CASE  WHEN (fstate= '' OR fstate ISNULL) THEN 'Running' ".
                " ELSE fstate END as status ".
                " FROM {ftep_job} ".
                " INNER JOIN services ON jid=uuid ".
                " INNER JOIN {users} ON {ftep_job}.uid={users}.uid  ".
                " WHERE ({ftep_job}.uid=:uid) %s ",$fields , $jid
            );
        $result = db_query( $sql, $params, $opts);
        $res = $result->fetchAll();

        // 
        // Go for the relationships now
        //
        $relationship=$relName;

        $include = $parameters->getInclude(['service']);
        if($include  or $relationship ){
            $fieldNames=$this->getFields('service');
            foreach($res as $k=>$v){
                $resx= db_query(
                    sprintf(
                        "SELECT sid,%s".
                        " FROM {ftep_service}  ".
                        " WHERE ({ftep_service}.sid=:id) ",  $fieldNames)
                        , array( ':id'=> $res[$k]->sid )
                    ); 
                if(!is_null($relationship)){
                    $res[$k]=(object)array('id'=>$res[$k]->id);
                }
                $res[$k]->service=$resx->fetchAll();
            }

            if($relationship){
                $y=new ServiceSerializer('relation');
                $x1 = (new Collection($res, $y  ));
                $document = new Document($x1);
                return $document;
            } else {
                $relationship=$include;
            }
        }
        return $this->writeJsonApiResponse($res, new JobSerializer, $id, 200, $relationship);
    }
    protected function resolveJob($id){
        $user = $this->getUser();
        $params[':id' ] = $id;
        $params[':uid' ] = $user->uid;
        $sql= "SELECT * ".
                " FROM {ftep_job} ".
                " WHERE ( {ftep_job}.id=:id ) AND ( uid=:uid )  " ;

        $result = db_query( $sql, $params );
        $res = $result->fetchObject();
        if(!$res){
            throw new Exception("No result found");
        }
        return $res;
    }


    public function getJobGui($id){
        $job = $this->resolveJob($id);
        if(!$job->guiendpoint){
            throw new Exception("The job ".$id." does not have gui information ");
        }
        $resx[] =(object) array(
                    "jobid"=>"",
                    "fname"=>"",
                    "link" =>""
                );
        header("X-AUTH-TOKEN");
        die("HERE");
    }
    public function getJobOutput($id){
        $user = $this->getUser();

        $params[':id' ] = $id;
        $params[':uid' ] = $user->uid;

        $sql= "SELECT id, outputs ".
                " FROM {ftep_job} ".
                " WHERE ( {ftep_job}.id=:id ) AND ( uid=:uid )  " ;

        $result = db_query( $sql, $params );
        $res = $result->fetchObject();
        //file_put_contents('/tmp/test.log', var_export(array($res,$result,$sql, $params),true));
        if(!$res){
            throw new Exception("No result found");
        }
        $resx=array();
        $outputs=json_decode( stripslashes( trim($res->outputs,'"') ));
        if($outputs){
            foreach($outputs as $k=>$v){
                $a= preg_match_all('/.*\/Job_(.*)\/outDir\/(.*)/i', $v, $aresult);
                $resx[] =(object) array(
                    "jobid"=>$aresult[1][0],
                    "fname"=>$aresult[2][0],
		    "link" => "/download/Job_" . $aresult[1][0] . "/outDir/" . $aresult[2][0]
                );
            }
        }
        $y=new JobOutputSerializer();
        $x1 = (new Collection($resx, $y  ));
        $document = new Document($x1);
        $document->addMeta('total', count($resx));
        $document->addLink('self', $this->getUrl( $id ."/getOutputs"));
        return $document;


    }
    protected function getServiceRelationShip($res){
        $fields = $parameters->getFields();
        $fieldNames=$this->getFields('service', '{ftep_service}.*');
        foreach($res as $k=>$v){
            $resx= db_query(
                sprintf(
                    "SELECT sid,%s".
                    " FROM {ftep_service}  ".
                    " WHERE ({ftep_service}.sid=:id) ",  $fieldNames)
                    , array( ':id'=> $res[$k]->sid )
                ); 
            $res[$k]->service=$resx->fetchAll();
        }
        return $res;
    }
    public function resolveService($data){
        // @TODO check accessibility
        $status='approved';
        $sql= "SELECT sid, name, access_level, status" .
            " FROM {ftep_service}" .
            " WHERE (name=:name) AND (status=:status)";
        $serviceq  = db_query( $sql, array(':name' => $data->serviceName , ':status' => $status ));


        $service = $serviceq->fetchObject();
        //file_put_contents("/tmp/service.log", var_export(array($sql, $serviceq, $service), true));
        if(!$service){
            //die("<pre>".var_export(array($service, $data),true));
            $msg=sprintf("Cannot find service identified by " , $data->serviceName);
            throw new Exception($msg);
            //return $this->writeResponse($msg, 409);
        }
        return $service->sid;
    }
    private function resolveUser($data){
        $status=1;
        $service  = db_query("SELECT uid" .
            " FROM {users}" .
            " WHERE (name=:name) AND (status=:status)", 
            array(':name' => $data->userId , ':status' => $status )
        );
        $service = $service->fetchObject();
        if(!$service){
            //die("<pre>".var_export(array($service, $data),true));
            $msg=sprintf("Cannot find user " , $data->userId);
            throw new Exception($msg);
            //$msg=sprintf("Cannot find userid for '%s' " , $data->userId);
            //return $this->writeResponse($msg, 409);
        }
        return $service->uid;
    }
    public function write(){
        $this->_log->info(__METHOD__. ' - '. __LINE__. ' - '. var_export(array($_REQUEST),true) );
        $user = $this->getUser();
        $data = endpoint_request_data();
        $this->_log->info(__METHOD__. ' - '. __LINE__. ' - '. var_export($data,true) );

        $data = $this->getSection('jobs');
        $obj = $this->getJobPayload();
        try{ 
            if( ! array_key_exists('uid', $obj) ) {
                throw new Exception("Missing uid");
            }
            if( ! array_key_exists('sid', $obj) ) {
                throw new Exception("Missing sid");
            }

            $nid = db_insert('ftep_job')
                ->fields( array_keys($obj) )
                ->values($obj)
                ->execute();
            $obj['id'] =$nid;
            $obj['fstate']='Running';
        }
        //catch( PDOException $Exception ) {
        catch( Exception  $Exception ) {
            // Note The Typecast To An Integer!
            $msg = $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
            return $this->writeResponse($msg, 409, " Conflict");
        }
       // $queue = DrupalQueue::get('jobs_queue');
       // $queue->createQueue();
       // $queue->createItem($data);

        $resource = new Resource((object)$obj, new JobSerializer);
        return $this->writeJsonApiResponse( $resource, null, $nid, 201, null);
    }
    protected function getJobPayload(){
        $data = $this->getSection('jobs');
        $obj = array(
            'jid'   => $data->jobId,
            'inputs' => ($data->inputs=='NA' ? '{}' : $data->inputs ),
            'outputs' => ($data->outputs=='NA' ? '{}' : $data->outputs ),
            'step' => $data->step,
            'guiendpoint' => (array_key_exists('guiEndpoint',$data) ? ($data->guiEndpoint ? $data->guiEndpoint : "") : ""),
            'uid'=> null,
        );
        if( (isset($data->userId) || property_exists($data,'userId') )&& (strlen($data->userId)>0)){
            $obj['uid'] = $this->resolveUser( $data );
        }
        if( (isset($data->serviceName) || property_exists($data,'serviceName')) && (strlen($data->serviceName)>0)) {
            $obj['sid'] = $this->resolveService( $data );
        }
        return $obj;
    }
    public function delete($id){
        $user = $this->getUser();
        $num_deleted = db_delete( 'ftep_job' )
            ->condition( 'id', $id)
            ->condition( 'uid', $user->uid)
            ->execute();
        http_response_code(204);
    }

    public function update($id){
        $user=$this->getUser();
        $data = $this->getSection('jobs');
        $fields = get_object_vars($data);
        $this->_log->info(__METHOD__. ' - '. __LINE__. ' - '. var_export(array($id, $_REQUEST,$fields),true) );
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
            $obj = $this->getJobPayload();
            $jid=$obj['jid'];

            unset($obj['jid']);
            $query= db_update('ftep_job')
                ->fields( $obj )
                ->condition( 'id' , $id ,'=') ;
            $nid = $query->execute();
            if($nid==0){
                http_response_code(404);
                return;
            }

            $queue = DrupalQueue::get('jobs_queue');
            $queue->createQueue();
            $obj['id']=$id;
            $obj['jid']=$jid;
            $obj['user']=$user->name;

            $queue->createItem($obj);
        }
        catch( PDOException $Exception ) {
            // Note The Typecast To An Integer!
            http_response_code(409);
            return  $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
        }
        return $this->read($body_id);
    }

}
