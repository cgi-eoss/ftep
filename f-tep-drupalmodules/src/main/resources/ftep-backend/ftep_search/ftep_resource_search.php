<?php
use Psr\Http\Message\ServerRequestInterface;
use GuzzleHttp\Pool;
use GuzzleHttp\Client;
use GuzzleHttp\Psr7\Request;
use GuzzleHttp\Psr7;
use DI\Annotation\Inject;


use Monolog\Handler\StreamHandler;
use Monolog\Logger;

use Monolog\Processor\WebProcessor;
use Monolog\Processor\MemoryUsageProcessor;
//use Monolog\Formatter\LineFormatter;
//use Monolog\Handler\RotatingFileHandler;

//use Kevinrob\GuzzleCache\CacheMiddleware;
//use Kevinrob\GuzzleCache\Storage\FlysystemStorage;
//use Kevinrob\GuzzleCache\Strategy\PrivateCacheStrategy;
//use League\Flysystem\Adapter\Local;

require __DIR__ . '/vendor/autoload.php';

spl_autoload_register(function ($class) {
    $base_dir = __DIR__.'/parsers/';
    $file=$base_dir.'ftep_opensearch_parser_'.strtolower(substr($class,0,strpos($class,'ResponseHandler'))).'.php';
    if(file_exists($file)){
        require $file;
    }
});

class FtepResourceSearch  {
    /**
     * @Inject("Psr\Log\LoggerInterface")
     */
    protected $_log;

    public function __construct()
    {
        $this->_log = new Logger( basename(__FILE__) );
        $this->_log->pushHandler( new StreamHandler('/var/log/ftep.log', Logger::DEBUG));
        // processor, adding URI, IP address etc. to the log
        $this->_log->pushProcessor(new WebProcessor);
        $this->_log->pushProcessor(new MemoryUsageProcessor);
        // $handler = new RotatingFileHandler(storage_path().'/logs/useractivity.log', 0, Logger::INFO);
        // $log->pushHandler($handler);
        $this->_log->pushProcessor(
            function ($record) {
                $user = $this->getUser();
                $record['extra']['uid'] = $user->uid;
                $record['extra']['name'] = $user->name;
                return $record;
            }
        );
        $this->_conf=[
          'debug'=>false, 'useCache'=>false,
        ];
    }
    protected function getUser(){
        global $user;
        return $user;
    }
    public function getDatasources(array $datasourceParameters){
        $this->_log->debug( __METHOD__.' - '.__LINE__.' Called with parameters : '.var_export($datasourceParameters,true) );

        drupal_bootstrap(DRUPAL_BOOTSTRAP_FULL);
        $query = new EntityFieldQuery;

        // Load datasources entities defined and enabled
        $query = $query->entityCondition('entity_type', 'ftep_datasource')->propertyCondition('enabled', 't') ;

        $source_type='_undefined_';

        if( $datasourceParameters['internal'] ==='true' ){
            // This is a search for a product
            $source_type='internal';
        }
        if($datasourceParameters['external'] ==='true'){
            // This is a search for an external data (e.g. CEMS/IPT)
            $source_type='external';
        }
        if($datasourceParameters['reference'] ==='true'){
            // This is a search for an reference data
            $source_type='reference';
        }
        $this->_log->info(__METHOD__.' - ' .__LINE__. ' - Searching '.$source_type.' data');
        $query->propertyCondition('source_type',$source_type,'=');

        // Contains the list of the datasources enabled and ready to be used

        $results = $query->execute();

        $datasources=array();
        if (isset($results['ftep_datasource'])) {
            $nodes = entity_load('ftep_datasource',array_keys($results['ftep_datasource']));
            foreach ($nodes as $nid => $node) {
                // Do something with the node object
                $datasources[  ] = array(
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
     * Returns an array containing only defined query parameters
     *
     * @param ServerRequestInterface $data
     * @return mixed
     */
    protected function parseRequest(ServerRequestInterface $data){
        $params=array();
        foreach($data->getQueryParams() as $k=>$v){
            /**
             * Don't need to parse 'q' as this is managed (here) by drupal
             */
            if($k==='q'){ continue; }
            // no need to add empty params
            if(  null!==$v ){ $params[$k]=$v; }
        }
        return $params;
    }
    /**
     * Submit a search to all registered and active datasources.
     * Parameters are aliased as declared by the datasource plugin itself.
     * Results are parsed by the plugin itself.
     * @throws InvalidArgumentException for invalid file values
     */
    public function search(){
        $request = Zend\Diactoros\ServerRequestFactory::fromGlobals();
        $params = $this->parseRequest($request);

        $this->_log->debug(__METHOD__.' - '.__LINE__. ' - Search request with parameters: '.var_export($params,true));

        // Select the datasource type
        // internal     means products created by TEP
        // exernal      means search external catalogues
        // reference    means search internal catalogue for reference data
        $ds_params=array(
            'internal' => array_key_exists('tep', $params) ? $params['tep'] : false ,
            'reference'=> array_key_exists('ref', $params) ? $params['ref'] : false ,
            'external' => array_key_exists('sat', $params) ? $params['sat'] : false
        );

        $datasources = $this->getDatasources($ds_params);
        $results=array(
            'data'=>array()
        );




        $client_conf = [
            // http://docs.guzzlephp.org/en/latest/request-options.html
            'allow_redirects' => [ // set to false to disable
                'max'             => 5,
                'strict'          => false,
                'referer'         => false,
                'protocols'       => ['http', 'https'],
                //'on_redirect'     => $onRedirect,
                'track_redirects' => true
            ],
            'timeout' => 10,
//          // 'debug' => true,
            // 'handler' => $stack,
            'verify' => false
        ];
        if($this->_conf['debug']){
            $client_conf['debug'] = true;
        }
        $client = new Client($client_conf);

        $logger = $this->_log;
        $requests = function () use ($datasources, $logger, $params  ) {
            $data = $params;
            foreach($datasources as $n=>$datasource){

                $url=$datasource['endpoint']."".(array_key_exists('format',$datasource) ? $datasource['format']:"");

                $plugin = $this->getDataSourcePlugin($datasource['name'],$datasource);

                if( method_exists($plugin, 'aliasSearchParameters') ) {
                    $logger->debug(__METHOD__.' - '.__LINE__. ' - calling aliasSearchParameters ');
                    $data = $plugin->aliasSearchParameters($data);
                }
                if( method_exists($plugin, 'appendQueryParams') ) {
                    $logger->debug(__METHOD__.' - '.__LINE__. ' - calling appendQueryParams ');
                    $data = $plugin->appendQueryParams($data);
                }
                if( method_exists($plugin, 'appendURL') ) {
                    $logger->debug(__METHOD__.' - '.__LINE__. ' - calling appendURL ');
                    $url.= $plugin->appendURL($data);
                }
                $this->_log->notice(__METHOD__.' - '.__LINE__. ' - Submitting search to : '.$datasource['name']. ' - '.var_export($data,true)  );
                if($datasource['name']==='CEDA2'){
                    $data['dataOnline']='true';
                }

                $url.='?'.http_build_query($data);
                $logger->debug(__METHOD__.' - '.__LINE__. ' - calling : '.$url);

                yield new Request('GET', $url ) ;
            }
        };

        $pool = new Pool($client, $requests(), [
            'concurrency' => 5,
            'fulfilled' => function (GuzzleHttp\Psr7\Response $response, $index) use ($datasources,$logger,&$results) {
                // this is delivered each successful response
                //$code = $response->getStatusCode(); // 200
                //$reason = $response->getReasonPhrase(); // OK

                $plugin = $this->getDataSourcePlugin($datasources[$index]['name'],$datasources[$index]);

                $res = $plugin->parse($response);

                $results['data'][]=array('datasource'=>$datasources[$index]['name'],'results'=>$res);

                if($res['totalResults']===0) {
                    $logger->notice(__METHOD__ . ' - ' . __LINE__ . ' - no results from ' . $datasources[$index]['name']);
                }
            },
            'rejected' => function($reason, $index) use ($datasources, $logger) {
                // this is delivered each failed request
                $logger->error(__METHOD__.' - '.__LINE__. ' - error calling  '.$datasources[$index]['name'].' : '.$reason);
            },
        ]);
        $promise = $pool->promise();
        $promise->wait();
        return $results;

    }

    /**
     * Instantiate the class specified in the database for this datasource
     * @param $n string name of the resource
     * @param $datasource string name of the calss
     * @throws FtepClassNotFoundException
     * @return object
     */
    protected function getDataSourcePlugin($n, $datasource){
        //require_once dirname(__FILE__)."/ftep_opensearch_parser_".strtolower($n).".php";
        if(array_key_exists('parserClass', $datasource)) {

            if(! class_exists($datasource['parserClass']) ){
                $this->_log->error(__METHOD__.' - '.__LINE__. ' - parser class : '.$datasource['parserClass']. ' does not exists.' );
                http_response_code(500);
                throw new FtepClassNotFoundException(sprintf("Class '%s' not found when instantiating parser plugin", $datasource['parserClass']));
            }

            $this->_log->debug(__METHOD__.' - '.__LINE__. ' - Launching datasource parser class : '.$datasource['parserClass'] );
            $parser=new $datasource['parserClass']();
        }
        return $parser;
    }
}
