<?php
require_once dirname(__FILE__)."/ftep_resource.php";

require __DIR__ . '/vendor/autoload.php';
use Monolog\Logger;
use Monolog\Handler\StreamHandler;

class FtepResourceDataSource extends FtepResource {
    
    public function __construct(){
        parent::__construct();
    }
    public function getResourceName(){
        return "datasources";
    }
    public function getEntityName(){
        return "ftep_datasource";
    }
    public function getAuthentication($data){
        $data = join('.',  array_slice( explode(".", $data ) ,1  ));
        $sql=sprintf("SELECT id,name,policy,credentials_data,download_endpoint from {%s} WHERE (download_domain=:data) AND (enabled='t') ", $this->getEntityName());
        $result = db_query( $sql,array(":data"=>$data)) ;
        $res="";
        foreach ($result as $record) {
            $res['data'][] = $record;
        }
        return   $this->writeResponse($res,200);
    }

    public function read($id=null){
        $user = $this->getUser();
	$params=array(":id"=> $id) ;
	// changed is_null to NULL due to some
	// error in the dev environment
        if($id==NULL){
            $result = db_query( sprintf("SELECT *, '%s' as type FROM {%s}", $this->getResourceName(),$this->getEntityName()));
        } else {
            $result = db_query( sprintf("SELECT *, '%s' as type FROM {%s} WHERE (id=:id) ", $this->getResourceName(),$this->getEntityName()) , $params );
        }

        $res=array('data'=>array());

        foreach ($result as $record) {
            $res['data'][] = $record;
        }
        return   $this->writeResponse($res,200);
    }

    function write(){
        $user = $this->getUser();
        $data = endpoint_request_data();

        try{
            $nid = db_insert( $this->getEntityName() )
                ->fields(array('name', 'description', 'policy'))
                ->values(array(
                            'name' => $data->name,
                            'description' => $data->description,
                            'policy'=> $data->policy
                            //'created' => REQUEST_TIME,
                            ))
                ->execute();
        }
        catch( PDOException $Exception ) {
            // Note The Typecast To An Integer!
            $msg = $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
            return $this->writeResponse($msg, 409);
        }

        //global   $base_url;
        //$url = $base_url."/api/v1.0/jobs/".$nid;
        $url = $this->getUrl("/$nid");
        http_response_code(201);

        //header(sprintf('Location: %s/%s/%s',$base_url,"/api/v1.0/jobs/",$nid));
        header(sprintf('Location: %s/%s/%s',$url,"/api/v1.0/datasources/",$nid));
        header("Content-Type: application/vnd.api+json");
        $res=array(
                'data'=>array(  
                    'type' => 'datasource',
                    'id' => $nid,
                    'attributes' => $data,
                    ),
                'link'=> array( 
                    'self' => $url
                    )
                );
        return $res;

    }


    public function update($id){
        $user=$this->getUser();
        $data = endpoint_request_data();
        try{
            // Todo validare cambpi
            $query= db_update($this->getEntityName())
                ->fields( get_object_vars($data) )
                ->condition( 'id' , $id );
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
                    'type' => $this->getResourceName(),
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
