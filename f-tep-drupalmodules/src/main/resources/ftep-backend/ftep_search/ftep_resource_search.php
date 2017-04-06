<?php
require __DIR__ . '/vendor/autoload.php';

use DI\Annotation\Inject;
use GuzzleHttp\Client;
use GuzzleHttp\Pool;
use GuzzleHttp\Psr7;
use GuzzleHttp\Psr7\Request;
use Psr\Http\Message\ServerRequestInterface;

spl_autoload_register(function ($class) {
    $base_dir = __DIR__ . '/parsers/';
    $file = $base_dir . 'ftep_opensearch_parser_' . strtolower(substr($class, 0, strpos($class, 'ResponseHandler'))) . '.php';
    if (file_exists($file)) {
        require $file;
    }
});

class FtepResourceSearch
{
    /**
     * @Inject("Psr\Log\LoggerInterface")
     */
    protected $_log;

    /**
     * @Inject("DatasourceProviderService")
     */
    protected $_datasourceProviderService;

    /**
     * @Inject("CacheService");
     */
    protected $_cache;


    /**
     * @Inject("DI\Container")
     */
    protected $_container;

    public function getQuickLook($dataSource, $quickLook)
    {
        $params=['sat'=>'true' ];
        $datasource = $this->getDatasource($params);
        if(null === $datasource){
            $this->_log->error(__METHOD__ . ' - ' . __LINE__ . ' - No datasource found for '.$dataSource);
            header('HTTP/1.0 404 Not Found', true, 404);
            die;
        }
        foreach($datasource as $k=>$v){
            if($v['name']==$dataSource){
                $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Launching quickloook for  '.$dataSource);
                $plugin = $this->getDataSourcePlugin(23, $v);
                $plugin->quickLook($quickLook);
                return;
            }
        }
        $this->_log->notice(__METHOD__ . ' - ' . __LINE__ . ' - Cannot find quickloook driver for  '.$dataSource);
    }

    /**
     * Submit a search to all registered and active datasources.
     * Parameters are aliased as declared by the datasource plugin itself.
     * Results are parsed by the plugin itself.
     * @throws InvalidArgumentException for invalid file values
     */
    public function search()
    {
        $results = array(
            'data' => array()
        );

        $request = Zend\Diactoros\ServerRequestFactory::fromGlobals();
        $params = $this->parseRequest($request);

        $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Search request with parameters: ' . var_export($params, true));


        $datasources = $this->getDatasource($params);

        if(null === $datasources){
            return $results;
        }

        $client_conf = [
            // http://docs.guzzlephp.org/en/latest/request-options.html
            'allow_redirects' => [ // set to false to disable
                'max' => 5,
                'strict' => false,
                'referer' => false,
                'protocols' => ['http', 'https'],
                'track_redirects' => true
            ],
            'timeout' => 20,
            'debug' => false,
            // 'handler' => $stack,
            'verify' => false
        ];

        $client = new Client($client_conf);

        $logger = $this->_log;

        $requests = function () use ($datasources, $logger, $params) {
            foreach ($datasources as $n => $datasource) {
                $data = $params;

                $url = $datasource['endpoint'];
                $plugin = null;
                try {
                    $plugin = $this->getDataSourcePlugin($datasource['name'], $datasource);
                } catch (FtepClassNotFoundException $e) {
                    $logger->error(__METHOD__ . ' - ' . __LINE__ . ' - Could not instantiate class :' . $datasource['name']
                        . ' - ' . $e->getMessage());
                    continue;
                }

                if (null === $plugin) {
                    $logger->warning(__METHOD__ . ' - ' . __LINE__ . ' - Could not instantiate driver for :' . $datasource['name']);
                    continue;
                }

                $data = $plugin->aliasSearchParameters($data);

                $tmpData = $plugin->appendQueryParams($data);
                $data = array_merge($data, $tmpData);

                $url .= $plugin->appendURL($data);

                $url .= '?' . http_build_query($data);

                $this->_log->notice(__METHOD__ . ' - ' . __LINE__ . ' - Submitting search to : ' . $datasource['name'] . ' - ' . var_export($data, true));

                $logger->debug(__METHOD__ . ' - ' . __LINE__ . ' - calling : ' . $url);

                yield new Request('GET', $url);
            }
        };

        $pool = new Pool($client, $requests(), [
            'concurrency' => 10,
            'fulfilled' => function (GuzzleHttp\Psr7\Response $response, $index) use ($datasources, $logger, &$results) {
                // this is delivered each successful response
                //$code = $response->getStatusCode(); // 200
                //$reason = $response->getReasonPhrase(); // OK
                $logger->notice(__METHOD__ . ' - ' . __LINE__ . ' - got ' . $response->getStatusCode() . ' for ' . $datasources[$index]['name']);

                $plugin = $this->getDataSourcePlugin($datasources[$index]['name'], $datasources[$index]);
                $res = $plugin->parse($response);

                $results['data'][] = array('datasource' => strtoupper($datasources[$index]['name']), 'results' => $res);

                if ($res['totalResults'] === 0) {
                    $logger->notice(__METHOD__ . ' - ' . __LINE__ . ' - no results from ' . $datasources[$index]['name']);
                }
            },
            'rejected' => function ($reason, $index) use ($datasources, $logger) {
                // this is delivered each failed request
                $logger->error(__METHOD__ . ' - ' . __LINE__ . ' - error calling  ' . $datasources[$index]['name'] . ' : ' . $reason);
            },
        ]);
        $promise = $pool->promise();
        $promise->wait();
        return $results;

    }

    //        // Select the datasource type
//        // internal     means products created by TEP
//        // external      means search external catalogues
//        // reference    means search internal catalogue for reference data

    /**
     * @param $params
     * @return mixed
     */
    protected function getDatasource($params){
        $ds_params = array();
        $datasources=null;

        if (array_key_exists('tep', $params) && $params['tep'] === 'true') {
            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Going for internal data ');
            $ds_params[] = 'internal';
        }
        if (array_key_exists('ref', $params) && $params['ref'] === 'true') {
            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Going for reference data ');
            $ds_params[] = 'reference';
        }
        if (array_key_exists('sat', $params) && $params['sat'] === 'true') {
            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Going for external data ');
            $ds_params[] = 'external';
        }

        if (count($ds_params) === 0) {
            $this->_log->notice(__METHOD__ . ' - ' . __LINE__ . ' - No datasource driver selected ');
            return null;
        }

        $datasources = $this->_datasourceProviderService->getDataSources($ds_params);

        return $datasources;
    }
    /**
     * Returns an array containing only defined query parameters
     *
     * @param ServerRequestInterface $data
     * @return mixed
     */
    protected function parseRequest(ServerRequestInterface $data)
    {
        $params = array();
        foreach ($data->getQueryParams() as $k => $v) {
            /**
             * Don't need to parse 'q' as this is managed (here) by drupal
             */
            if ($k === 'q') {
                continue;
            }
            // no need to add empty params
            if (null !== $v) {
                $params[$k] = $v;
            }
        }
        return $params;
    }

    /**
     * Instantiate the class specified in the database for this datasource
     * @param $n string name of the resource
     * @param $datasource string name of the calss
     * @throws FtepClassNotFoundException
     * @return object
     */
    protected function getDataSourcePlugin($n, $datasource)
    {
        $parser = null;

        if (array_key_exists('parserClass', $datasource)) {
            if (!class_exists($datasource['parserClass'])) {
                $this->_log->error(__METHOD__ . ' - ' . __LINE__ . ' - parser class : ' . $datasource['parserClass'] . ' does not exists.');
                http_response_code(500);
                throw new FtepClassNotFoundException(sprintf("Class '%s' not found when instantiating parser plugin", $datasource['parserClass']));
            }

            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Launching datasource parser class : ' . $datasource['parserClass']);

            $parser = new $datasource['parserClass']();

            $this->_container->injectOn($parser);
            $parser->setConfig($datasource['config']);

        }

        return $parser;
    }
}
