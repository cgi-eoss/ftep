<?php
require_once dirname(__FILE__)."/ftep_resource.php";
require_once dirname(__FILE__)."/ftep_opensearch_parser.php";
require_once dirname(__FILE__)."/ftep_opensearch_parser_resto.php";

require __DIR__ . '/vendor/autoload.php';
use Monolog\Logger;
use Monolog\Handler\StreamHandler;
use \Curl\Curl;
use \Curl\MultiCurl;



class FtepResourceSearch extends FtepResource {

    public function __construct(){
        parent::__construct();
    }

    /**
    * @TODO: this should comoe from the drupal db
    */
    protected function getDataSources($kind=null){
        $datasources=array();
        drupal_bootstrap(DRUPAL_BOOTSTRAP_FULL);
        $query = new EntityFieldQuery;

        $query = $query->entityCondition('entity_type', 'ftep_datasource')
            ->propertyCondition('enabled', 't') ;

        if( $kind['internal']!=='true' || $kind['external'] !== 'true' ) {
            if($kind['internal'] ==='true' ){
                $this->_log->debug(__METHOD__." - ".__LINE__. " - Searching internal data ");
                $query->propertyCondition('source_type','internal','=');
            }
            if($kind['external'] ==='true'){
                $this->_log->debug(__METHOD__." - ".__LINE__. " - Searching external data ") ;
                $query->propertyCondition('source_type','external' ,'=');
            }
        }
        $results = $query->execute();
        if (isset($results['ftep_datasource'])) {
            $nodes = entity_load('ftep_datasource',array_keys($results['ftep_datasource']));

            foreach ($nodes as $nid => $node) {
                // Do something with the node object
                $datasources[ $node->name ] = array( 
                   'name' => $node->name,
                   'endpoint' => $node->endpoint,
                   'template' => $node->template,
                   'parserClass' => $node->parser,
                );
            }
        }
        return $datasources;
    }
    /**
     * Submit a search to all registered and active datasources.
     * Parameters are aliased as declared by the datasource plugin itself.
     * Results are parsed by the plugin itself.
     */
    public function search(){
        $this->_log->debug(__METHOD__." - ".__LINE__. " - Search : ".var_export($_REQUEST,true) );

        $parser = new OpenSearchParser();
        $params = $parser->parseRequest($_REQUEST);
        $this->_log->debug(__METHOD__." - ".__LINE__. " - Search : ".var_export($params,true));
        //$kind = array_key_exixts('ref', $_REQUEST) && $_REQUEST['ref']==true 
        $ds_params=array('internal' => $params['tep'] , 'reference'=> $params['ref'] , 'external' => $params['sat'] );
        $datasources = $this->getDataSources($ds_params);
        $results=array();

        $headers=array();
        $data=$params;

        $multi_curl = new MultiCurl();
        $multi_curl->setOpt(CURLOPT_SSL_VERIFYHOST, false);
        $multi_curl->setOpt(CURLOPT_SSL_VERIFYPEER, false);
        $multi_curl->setOpt(CURLOPT_CONNECTTIMEOUT,2);
        $multi_curl->setOpt(CURLOPT_FOLLOWLOCATION, true);
        $multi_curl->setOpt(CURLOPT_VERBOSE, true);

        foreach($datasources as $n=>$datasource){
            if($n=='scihub' or $n=='test2' ){ continue; }
            $dataset=array_key_exists("dataset", $data) ? "/".$data['dataset'] : "";
            $url=$datasource['endpoint']."$dataset".(array_key_exists('format',$datasource) ? $datasource['format']:"");

            $plugin = $this->getDataSourcePlugin($n,$datasource);

            if( method_exists($plugin, 'aliasSearchParameters') ) {
                $data = $plugin->aliasSearchParameters($data);
            }

            $this->_log->debug(__METHOD__." - ".__LINE__. " - Submitting search to : ".$datasource['name']. " - ".var_export($data,true)  );

            $c=$multi_curl->addGet($url, $data); 
            $c->complete( function($instance) use($plugin,&$results,$n) {
                $this->_log->debug(__METHOD__." - ".__LINE__. " - successfully called ".$instance->url);//." - ".var_export($instance->response,true) );
                $res = $plugin->parse($instance->response);
                if($res['totalResults']>0){
                    $results['data'][]=array('datasource'=>$n,'results'=>$res);
                } else{
                    $this->_log->debug(__METHOD__." - ".__LINE__. " - no results from $n");
                }
            });
            $c->error( function($instance) use($plugin) {
                $this->_log->debug(__METHOD__." - ".__LINE__. " - error calling ".$instance->url." - (".$instance->errorCode."):".$instance->errorMessage);
            });
        }
        $multi_curl->start();
        return $results;
    }

    /**
     * Instantiate the class specified in the database for this datasource
     * @param $n string name of the resource
     * @param $datasource string name of the calss
     *
     */
    protected function getDataSourcePlugin($n, $datasource){
        require_once dirname(__FILE__)."/ftep_opensearch_parser_".strtolower($n).".php";
        if(array_key_exists('parserClass', $datasource)) {

            if(! class_exists($datasource['parserClass']) ){
                $this->_log->error(__METHOD__." - ".__LINE__. " - parser class : ".$datasource['parserClass']. " does not exists." );
                http_response_code(500);
                throw new FtepClassNotFoundException(sprintf("Class '%s' not found when instantiating parser plugin", $datasource['parserClass']));
            }

            $this->_log->debug(__METHOD__." - ".__LINE__. " - Launching datasource parser class : ".$datasource['parserClass'] );
            $parser=new $datasource['parserClass']();
        }
        return $parser;
    }
}

