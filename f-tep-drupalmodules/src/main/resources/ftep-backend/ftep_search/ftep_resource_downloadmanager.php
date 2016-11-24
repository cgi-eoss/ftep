<?php
require_once dirname(__FILE__)."/ftep_resource.php";


//require __DIR__ . '/vendor/autoload.php';
//use Monolog\Logger;
//use Monolog\Handler\StreamHandler;

class FtepResourceDownloadManager extends FtepResource {
    
    public function __construct(){
        parent::__construct();
    }
    public function download2($jobid,$fname){
        $this->_log->debug(__METHOD__." - ".__LINE__. " - Request to download ".$jobid." - ".$fname);

        $user = $this->getUser();

        $sql="SELECT jid FROM {ftep_job} j ".
            " WHERE (id=:id) AND (uid=:uid) ";

        $params = array( ':id' => $jobid, ':uid' => $user->uid );
        $query = db_query( $sql, $params);
        $res=$query->fetchObject();

        if(!$res){
            http_response_code(404);
            $this->_log->error(__METHOD__." - ".__LINE__. " - Invalid request ".var_export(array($sql,$params),true));
            return;
        }
        $file = '/ftep-output/Job_'.$res->jid."/outDir/".$fname;

        $this->_log->debug(__METHOD__." - ".__LINE__. " - Request to download ".$jobid." - ".$fname." - File :".$file );

        header('Cache-Control: public, must-revalidate');
        header('Pragma: no-cache');
        // header('Content-Length: ' .(string)(filesize($file)) );
        header('Content-Disposition: attachment; filename='.$fname.'');
        header('Content-Transfer-Encoding: binary');
        header('X-Accel-Redirect: '. $file);
}

    public function download($id=null){
        $j = filter_input(INPUT_GET,"j",FILTER_SANITIZE_NUMBER_INT); ;
        $f = filter_input(INPUT_GET,"f",FILTER_SANITIZE_STRING); ;

        $this->_log->debug(__METHOD__." - ".__LINE__. " - Request to download ".$j." - ".$f." - ".var_export($id,true) );

        $user = $this->getUser();

        $sql="SELECT jid FROM {ftep_job} j ".
            " WHERE (id=:id) AND (uid=:uid) ";

        $params = array( ':id' => $j, ':uid' => $user->uid );
        $query = db_query( $sql, $params);
        $res=$query->fetchObject();

        if(!$res){
            http_response_code(404);
            $this->_log->error(__METHOD__." - ".__LINE__. " - Invalid request ".var_export(array($sql,$params),true));
            return;
        }
        $file = '/ftep-output/Job_'.$res->jid."/outDir/".$f;

        $this->_log->debug(__METHOD__." - ".__LINE__. " - Request to download ".$j." - ".$f." - ".var_export($id,true). " File :".$file );

        header('Cache-Control: public, must-revalidate');
        header('Pragma: no-cache');
        // header('Content-Length: ' .(string)(filesize($file)) );
        header('Content-Disposition: attachment; filename='.$f.'');
        header('Content-Transfer-Encoding: binary');
        header('X-Accel-Redirect: '. $file);
    }
}
