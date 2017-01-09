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


require dirname(__FILE__).'/serializers/DatabasketSerializer.php';
require dirname(__FILE__).'/serializers/FileSerializer.php';

class FtepResourceDataBasket extends FtepResource {
    
    public function __construct(){
        parent::__construct();
    }
    public function getResourceName(){
        return "databaskets";
    }
    public function getEntityName(){
        return "ftep_databasket";
    }

    public function read($id=null, $relationship=null, $relName=null  ){
        $parameters = new Parameters($_GET);
        $default_limit=5;
        $paged=$parameters->getLimit();
        $limit = $parameters->getLimit(100) ?  $parameters->getLimit(100) : $default_limit;
        $offset = $parameters->getOffset($limit) ; 

        $limit_clause="";
        $offset_clause="";
        if( !is_null($limit) ){
            if(! is_numeric($limit) ){
                $limit=$default_limit;
            }
            if($limit<=0){ 
                $limit = $default_limit;
            }
            $limit_clause = sprintf(" LIMIT %s ", $limit );
        }
        if( $offset ){
            $offset_clause = sprintf(" OFFSET %s ", $offset );
        }

        $user = $this->getUser();
        $params=array(":uid"=> $user->uid) ;
        if( array_key_exists('fake', $_GET)) { $params=array(":uid"=> 165) ; }

        $opts=array();

        $jid="";
        if(!is_null($id)){
            // Going for a single item
            $jid=" AND (idb=:idb) ";
            $params[':idb' ] = $id;
        }
        $sql= sprintf(
                "SELECT *  ".
                " FROM {ftep_databasket} ".
                " WHERE (uid=:uid) %s ".
                " ORDER BY name %s %s" , $jid, $limit_clause, $offset_clause ) ; 
        $result = db_query( $sql, $params, $opts );
        $res = $result->fetchAll();

        $sqltot= sprintf(
                "SELECT count(*)  ".
                " FROM {ftep_databasket} ".
                " WHERE (uid=:uid) %s ", $jid);
        $tot = db_query( $sqltot, $params, $opts )->fetchCol();

        $relationship=$relName;

        $includes = $parameters->getInclude(['files']); // ['author', 'comments']
        if($includes or $relationship){
            // Either I'm retrieving the attributes of a relationship
            // or I'm retrieving a related item itself
            //
            // For each Item obtained from the above query
            // get the related items 
            // and associate them to the corresponding item in the result arraa
            $fieldNames = $this->getFields('files'); //; ,'{ftep_files}.*' );
            foreach($res as $k=>$v){
                 $sql = sprintf(" SELECT {ftep_file2}.fid, {ftep_file2}.%s ". 
                     " FROM {ftep_file2} ".
                     " INNER JOIN {ftep_databasket} ON {ftep_file2}.databasket_id={ftep_databasket}.idb ".
                     " WHERE ( {ftep_databasket}.idb=:idb ) AND ( {ftep_databasket.uid}=:uid ) ORDER BY {ftep_file2}.name ", $fieldNames);

                $resx = db_query($sql, array( ':idb' => $res[$k]->idb, ':uid' => $params[':uid'] )  );
                if(!is_null($relationship)){
                    $res[$k] = (object)array('idb'=>$res[$k]->idb) ;
                }
                $res[$k]->files=$resx->fetchAll();
            }
            if($res){
                if($relationship){
                    return (object)$res[0];
                } else {
                    $relationship = $includes ;
                }
            }
             // return $this->writeJsonApiResponse($res, new DatabasketSerializer, $id, 200, $relationship);
            $dbs=new DatabasketSerializer();
            $dbsc= (new Collection($res, $dbs));
            $dbsc->with($includes);
            $document = new Document( $dbsc );
            $document->addMeta('total', $tot[0]);
            $document->addLink('self', $this->getUrl( $id ));
            return $document;
        }

        $dbs=new DatabasketSerializer();
        $dbsc= (new Collection($res, $dbs));
        $document = new Document( $dbsc );

/*
        $sqltot= sprintf(
                "SELECT count(*)  ".
                " FROM {ftep_databasket} ".
                " WHERE (uid=:uid) %s ", $jid);
        $tot = db_query( $sqltot, $params, $opts )->fetchCol();
 */

        if(! is_null($paged) ){
            $first=1;
            $cur=(($offset+$limit)/$limit);
            $next=(($offset+$limit)/$limit)+1;
            $prev= ($offset/$limit);

            $last= ceil($tot[0]/$limit);

            $document->addLink('first',$this->getUrl($id)."?page[number]=".$first."&page[size]=".$limit);
            if($prev>0) $document->addLink('prev', $this->getUrl($id)."?page[number]=".$prev."&page[size]=".$limit);
            $document->addLink('next', $this->getUrl($id)."?page[number]=".$next."&page[size]=".$limit);
            $document->addLink('last', $this->getUrl($id)."?page[number]=".$last."&page[size]=".$limit);
            $document->addLink('self', $this->getUrl($id)."?page[number]=".$cur."&page[size]=".$limit);
            $document->AddMeta('total' , $tot[0] );
        } else {
            // $document->AddMeta('total' , count($res));
            $document->AddMeta('total' , $tot[0] );
            $document->addLink('self',$this->getUrl($id));
        }

        return $document;
    }
    public function write($id=null,$relationship=null,$relName=null){
        $user = $this->getUser();
        $data = $this->getSection('databaskets');
        if( is_array($data) ){
            if( $relationship && $relName ){
                // THis is a POST on a relationship
                $result = $this->updateRelationship($data, $id);
                return $result;
                //die("here1".var_export($result,true));
            }
        } 
        $obj=array(
            'name' => $data->name,
            'description' => $data->description,
            'accesslevel'=> property_exists( $data, "accessLevel" ) ? $data->accessLevel :"" ,
            'uid' => $user->uid,
        );

        try{
            $nid = db_insert( $this->getEntityName() )
                ->fields(array('name', 'description', 'accesslevel','uid'))
                ->values($obj)
                ->execute();
            $obj['idb'] =$nid;
        }
        catch( PDOException $Exception ) {
            // Note The Typecast To An Integer!
            $msg = $Exception->getMessage( ) . (int)$Exception->getCode( ) ;
            return $this->writeResponse($msg, 409);
        }
        $resource = new Resource((object)$obj, new DatabasketSerializer);
        return $this->writeJsonApiResponse( $resource, null, $nid, 201);
    }
    public function delete($id,$relationship=null,$relName=null){
        $user = $this->getUser();
        if(!is_null($relationship) ){
            $data=json_decode(file_get_contents("php://input")); //     endpoint_request_data();
            
            if( empty($data->data)  ){
                return;
            }
                // This is actually a cleanup of all the elements of the relationship
            $sql=sprintf(" DELETE FROM {ftep_file2} f ".
                    " USING {ftep_databasket} d ".
                    // " WHERE (f.databasket_id=d.idb) AND (d.uid=%d) AND (d.idb=:idb) AND (f.fname=:fname) ", $user->uid);
                    " WHERE (f.databasket_id=d.idb) AND (d.uid=%d) AND (d.idb=:idb) AND f.name in (:fnames) ", $user->uid);
            if( !is_array($data->data)  ) {
                $data->data = array( $data->data );
            }
            $fnames = array_map(create_function('$o', 'return $o->name;'), $data->data);
            $query = db_query($sql,  array(':idb'=>$id, ':fnames'=>$fnames )) ;
            return $this->read($id,'relationship','files');
        } else {
            $num_deleted = db_delete( 'ftep_databasket' )
                ->condition( 'idb', $id)
                ->condition( 'uid', $user->uid)
                ->execute();
            http_response_code(204);
        }
    }

    private function multicall($data, array $opts=array()  ){
        $chs=array();
        $results=array();

        $mh = curl_multi_init();
        $result=array();
        foreach($data as $id=>$d){
            $chs[$id] = curl_init();
            $url = ( is_array($d) && !empty($d['url']) ) ? $d['url'] : $d;
            curl_setopt($chs[$id], CURLOPT_URL, $url);
            curl_setopt($chs[$id], CURLOPT_HEADER,0);
            curl_setopt($chs[$id], CURLOPT_RETURNTRANSFER, true );
            // extra options?
            if (!empty($options)) {
                curl_setopt_array($curly[$id], $options);
            }
            curl_multi_add_handle($mh, $chs[$id]);
        }
        // execute the handles
        $running = null;
        do {
            curl_multi_exec($mh, $running);
        } while($running > 0);

        // get content and remove handles
        foreach($chs as $id => $c) {
            $result[$id] = curl_multi_getcontent($c);
            curl_multi_remove_handle($mh, $c);
        }
        // all done
        curl_multi_close($mh);
        return $result;
    }
    protected function updateRelationship($items, $id){
        // This is to add something new to the relationship
        $urls=array();
        try{
            // $query = db_insert( "ftep_file2" ) ->fields( array('name', 'url', 'datasource', 'databasket_id') );
            //
            $sql="INSERT INTO {ftep_file2}(name, url, datasource, databasket_id) VALUES( :name, :url, :datasource, :databasket_id ) ON CONFLICT DO NOTHING";
            foreach($items as $k=>$v){
                $urls[$v->name] = $v->url ;
                $items[ $v->name ] = array('name' =>  $v->name, 'databasket_id' =>  $id , 'url'=> $v->url );
                $query = db_query($sql, array(':name'=>$v->name, ':url'=>$v->url, ':datasource'=>'', ':databasket_id'=> $id) ) ;
                //file_put_contents('/tmp/log.txt',"Inserting file ".$v->url. " - ".var_export($items,true),FILE_APPEND);
            }
        }catch (PDOException $e) {
            switch($e->getCode()){
            case '23505': {
                http_response_code(409);
                return  $e->getMessage();
                break;
            }
            default: {
                throw $e;
            }
            }
        }
        // Now update the properties reading the corresponding datasource
        $xxx = $this->multicall( $urls );
        foreach($xxx as $k=>$v){
            $data=json_decode($v);
            if(!$data){ 
                watchdog('ftep_databasket',
                    'Cannot decode json message :'.check_plain($v).
                    ' - User '.$this->getUser()->uid." - URL: ".$k,
                    WATCHDOG_NOTICE);
                continue; 
            };
            $start = $data->temporal->start_time;
            $stop = $data->temporal->end_time;
            $size = $data->file->size;
            $fname=$data->file->filename;
            $entr =  array( 
                'identifier'=> $fname,
                'start'=> $start,
                'stop' => $stop,
                'size'=>$size,
                'geo'=> $data->spatial->geometries->display
            );

            $param_list=array('data_format', 'temporal', 'misc','file');
            $data->file->path="ftp://ftp.ceda.ac.uk".$data->file->path;
            foreach($param_list as $p ){
                $entr['details'][$p] =  $data->$p ;
            }
            //$result['entities'][] = $v;
            $result = db_query(  " UPDATE {ftep_file2} set properties_text=:a where (name=:b) and (databasket_id=:c) ",  array(":a"=>json_encode($entr) ,":b"=>$k, ":c"=> $items[ $k ]['databasket_id'] ) );
        }
        return $this->read($id,'relationship','files');
    }

    public function update($id,$relationship=null,$relName=null){
        $user=$this->getUser();
        if(!is_null($relationship)){
            $relationships=array("files"=>array("table"=>"{ftep_files}", "update_query"=>"") );
            if( ! in_array($relName, array_keys($relationships) ) ){
                throw new Exception("Unsupported relationship: ".$relName);
            }
            $data=endpoint_request_data();
            if( empty($data->data)  ){
                // This is actually a cleanup of all the elements of the relationship
                $sql=sprintf(" DELETE FROM {ftep_file2} f ".
                    " USING {ftep_databasket} d ".
                    " WHERE (f.databasket_id=d.idb) AND (d.uid=%d) AND (d.idb=%d) ", $user->uid, $id);
                $result = db_query($sql)
                    ->execute();
                return $this->read($id,'relationship','files');
            } else {
                $this->updateRelationship($data->data, $id); 
                // This is to add something new to the relationship
                /*
                try{
                    $query = db_insert( "ftep_file2" )
                        ->fields( array('name', 'url', 'datasource', 'databasket_id') );
                    foreach($data->data as $k=>$v){
                        $urls[$v->name] = $v->url ;
                        $query->values(   array($v->name, $v->url, '', $id) ) ;
                        $items[ $v->name ] = array('name' =>  $v->name, 'databasket_id' =>  $id , 'url'=> $v->url );
                    }
                    $query->execute();
                }catch (PDOException $e) {
                    switch($e->getCode()){
                    case '23505': {
                        http_response_code(409);
                        return  $e->getMessage();
                        break;
                    }
                    }
                }
                // Now update the properties reading the corresponding datasource
                // 
                $xxx = $this->multicall( $urls );
                foreach($xxx as $k=>$v){
                    $data=json_decode($v);
                    if(!$data){ continue; };
                    $start = $data->temporal->start_time;
                    $stop = $data->temporal->end_time;
                    $size = $data->file->size;
                    $fname=$data->file->filename;
                    $entr =  array( 
                        'identifier'=> $fname,
                        'start'=> $start,
                        'stop' => $stop,
                        'size'=>$size,
                        'geo'=> $data->spatial->geometries->display
                    );

                    $param_list=array('data_format', 'temporal', 'misc','file');
                    $data->file->path="ftp://ftp.ceda.ac.uk".$data->file->path;
                    foreach($param_list as $p ){
                        $entr['details'][$p] =  $data->$p ;
                    }
                    $result['entities'][] = $v;
                    $result = db_query(  " UPDATE {ftep_file2} set properties_text=:a where (name=:b) and (databasket_id=:c) ",  array(":a"=>json_encode($entr) ,":b"=>$k, ":c"=> $items[ $k ]['databasket_id'] ) );
                }
                return $this->read($id,'relationship','files');
            } // relationship
                 */
                return $this->read($id,'relationship','files');
            } // relationship
        } 
        $data = $this->getSection('databaskets');
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
