<?php
use DI\Annotation\Inject;
use GuzzleHttp\Client;
use GuzzleHttp\HandlerStack;
use GuzzleHttp\Pool;
use GuzzleHttp\Psr7\Request;
use Kevinrob\GuzzleCache\CacheMiddleware;
use Kevinrob\GuzzleCache\Storage\FlysystemStorage;
use Kevinrob\GuzzleCache\Strategy\PrivateCacheStrategy;
use League\Flysystem\Adapter\Local;

require_once  __DIR__.'/OpensearchParserInterface.php';

class Ceda2ResponseHandler implements OpensearchParserInterface
{
    static protected $namespaces = [
        '' => 'http://www.w3.org/2005/Atom',
        'geo' => 'http://a9.com/-/opensearch/extensions/geo/1.0/',
        'os' => 'http://a9.com/-/spec/opensearch/1.1/',
        'eo' => 'http://a9.com/-/opensearch/extensions/eo/1.0/',
        'dc' => 'http://purl.org/dc/elements/1.1/',
        'georss' => 'http://www.georss.org/georss',
        'gml' => 'http://www.opengis.net/gml',
        'metalink' => 'urn:ietf:params:xml:ns:metalink',
        'xlink' => 'http://www.w3.org/1999/xlink',
        'media' => 'http://search.yahoo.com/mrss/',
        'eop20' => 'http://www.opengis.net/eop/2.0'
    ];

    /**
     * @Inject("Psr\Log\LoggerInterface")
     */
    protected $_log;

    /**
     * @Inject("CacheService");
     */
    protected $_cache;

    /**
     * @Inject("CredentialResolverService")
     */
    protected $_credService;

    /**
     * @Inject("api.version")
     */
    protected $_apiVersion;

    protected $_config;

    static protected $alias= [
        'polarisation' => 'polarisationChannels',
    ];

    /**
     * @param mixed $config
     * @return array
     */
    public function setConfig($config)
    {
        $this->_log->debug(__METHOD__.' - '.__LINE__);

        $this->_config = $config;
        return $this->_config;
    }

    /**
     * @param \GuzzleHttp\Psr7\Response $data
     * @return array
     * @throws \LogicException
     */
    public function parse(GuzzleHttp\Psr7\Response $data)
    {
        $this->_log->debug(__METHOD__.' - '.__LINE__);

        $response = $data->getBody()->getContents();

        $result = [
            'datasource' => 'CEDA2',
            'totalResults' => 0,
            'startIndex' => 0,
            'itemsPerPage' => $this->_config['itemsPerPage'],
            'entities' => []
        ];

        if ($this->_config['mode'] === 'json') {
            $response = json_decode($response);

            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Parsing JSON response ');

            $result['totalResults'] = $response->totalResults;
            $result['startIndex'] = $response->startIndex;

            $result['entities'] = array_map(
                function ($item) {
                    $fname = basename($item->file->filename, '.manifest');
                    $item = [
                        'title' => $item->file->filename,
                        'identifier' => $fname,
                        'link' => $item->file->directory . '/' . $fname . '.zip',
                        'size' => $item->file->data_file_size,
                        'meta' => null,
                        'type' => 'application/unknown',
                        'start' => $item->temporal->start_time,
                        'stop' => $item->temporal->end_time,
                        'geo' => $item->spatial->geometries->display,
                        'ql'  => '/secure/api/v'.$this->_apiVersion.'/ql/ceda2' . $item->file->directory.'/'. $item->file->quicklook_file,
                        'details' => [
                            'misc' => $item,
                            'temporal' => $item->temporal,
                            'spatial' => $item->spatial,
                            'file' => $item->file,
                        ]

                    //'thumbnail' => $item->file->quicklook_file,
                        //'location' => $item->file->location,
                    ];
                    return $item;
                }
                , $response->rows
            );
        } else {
            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Parsing ATOM response ');
            $xml = simplexml_load_string($response);

            $result = array(
                'datasource' => 'CEDA2',
                'totalResults' => (string)$xml->xpath('//os:totalResults')[0],
                'startIndex' => (string)$xml->xpath('//os:startIndex')[0],
                'itemsPerPage' => (string)$xml->xpath('//os:itemsPerPage')[0],
                'entities' => []
            );

            $namespaces = $xml->getNamespaces(true);
            // register a prefix for the default namespace:
            isset($namespaces[""]) && $xml->registerXPathNamespace('default', $namespaces['']);

            $entitiesArray = $result['entities'];

            $requests = array();

            $logger = $this->_log;

            array_map(
                function (SimpleXMLElement $entity) use (&$entitiesArray, &$requests, $logger) {
                    $namespaces = $entity->getNamespaces(true);
                    // register a prefix for the default namespace:
                    isset($namespaces[""]) && $entity->registerXPathNamespace('default', $namespaces['']);

                    array_walk(
                        Ceda2ResponseHandler::$namespaces,
                        function ($n, $ns) use (&$entity) {
                            $entity->registerXPathNamespace($n, $ns);
                        });

                    $ent = [
                        'title' => (string)$entity->title,
                        'identifier' => (string)$entity->title,
                        'link' => null,
                        'size' => $this->getTagAttribute('default:link[@rel="enclosure"]', $entity, 'length'),
                        'meta' => $this->getTagAttribute('default:link[@type="application/json"]', $entity, 'href'),
                    ];
                    // $ent['meta']= str_ireplace("opensearch-test.ceda.ac.uk","forestry-tep.eo.esa.int", $ent['meta']);

                    $link_node = $entity->xpath('./default:link[@rel="enclosure" and @title="ftp"]/@href');
                    if ($link_node && $link_node[0]) {
                        $ent['link'] = (string)$link_node[0]->href;
                    } else {
                        $ent['link']='xxx';
                    }
                    $ent['type'] = $this->getTagAttribute('default:link[@rel="enclosure"]', $entity, 'type');

                    $requests[] = new Request('GET', $ent['meta']);
                    $entitiesArray[] = $ent;
                }, $xml->xpath('//default:entry')
            );

            $result['entities'] = $entitiesArray;

            $client = new GuzzleHttp\Client([
                'timeout' => $this->_config['http.timeout'], // seconds
                'connect_timeout' =>  $this->_config['http.connect_timeout'] , // seconds
            ]);
            $pool = new Pool(
                $client,
                $requests, [
                'concurrency' => $this->_config['http.concurrency'],

                'fulfilled' => function (GuzzleHttp\Psr7\Response $response, $index) use (&$result, $logger) {
                    $details = json_decode($response->getBody());
                    $result['entities'][$index]['start'] = $details->temporal->start_time;
                    $result['entities'][$index]['stop'] = $details->temporal->end_time;
                    $result['entities'][$index]['geo'] = $details->spatial->geometries->display;
                    $result['entities'][$index]['ql'] = '/secure/api/v1.0/ql/ceda2' . $details->file->directory.'/'.$details->file->quicklook_file;
                    $result['entities'][$index]['details'] = $details;

                    $result['entities'][$index]['size'] = $details->file->data_file_size;
                    $result['entities'][$index]['title'] = $details->misc->product_info->Name;
                    $result['entities'][$index]['identifier'] = $details->misc->product_info->Name;

                    if(empty($details->file->quicklook_file)){
                        $logger->notice(__METHOD__ . ' - ' . __LINE__ . ' - Missing quicklook file for '.$details->misc->product_info->Name);
                    }
                },
                'rejected' => function ($reason, $index) use ($logger) {
                    // this is delivered each failed request
                    $logger->err(__METHOD__ . ' - ' . __LINE__ . ' - Failed getting info for item ' . $index . ' . -  ' . $reason);
                },
            ]);
            $promise = $pool->promise();
            $promise->wait();
        }
        return $result;
    }


    public function appendQueryParams(array $data){
        $this->_log->debug(__METHOD__.' - '.__LINE__);

        $result=array('dataOnline'=>'true');
        return $result;
    }

    /**
     * @param array $data
     * @return array
     */
    public function appendURL(array $data)
    {
        return '';
    }

    /**
     * @return array
     */
    /**
     * This function translates parameters received from the F-TEP client into something
     * accepted (compliant with) the specific datasoruce (view the "/describe.xml")
     * moreover for some fields it might be necessary to apply and extra 'tewak' e.g. productIdentifier is enclosed in "%"
     * @param array $params
     * @return array
     */
    public function aliasSearchParameters(array $params){
        $this->_log->debug(__METHOD__.' - '.__LINE__);

        foreach( Ceda2ResponseHandler::$alias as $k=>$v){
            if( array_key_exists($k, $params) ){
                $this->_log->debug(__METHOD__.' - '.__LINE__. ' - Aliasing '.$k.' as '.$v );
                $old=$params[$k];
                unset( $params[$k] );
                if(null !== $v ){
                        $params[$v] = $old;
                }
            }
        }
        return $params;
    }



    /**
     * Perform an xpath and returns the value
     *
     * @param $xpath
     * @param SimpleXMLElement $xml
     * @param $attribute
     * @return null|string
     */
    private function getTagAttribute($xpath, SimpleXMLElement $xml, $attribute)
    {
        $result = null;
        $link_tag = $xml->xpath($xpath);
        if ($link_tag && is_array($link_tag)) {
            $result = (string)$link_tag[0]->attributes()->{$attribute};
        }
        return $result;
    }
    public function quicklook( $quickLook ){
        $ql = $this->_cache->getItem($quickLook);

        if (!$ql->isHit()) {
            $fname = $this->_config['qlEndpoint'] . '/' . $quickLook;

            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Cache miss .. fetching ' . $fname);
            $curl_handle = null;
            try {
                $curl_handle = curl_init();
                if (!$curl_handle) {
                    throw new Exception('Could not initialize cURL.' . curl_errno($curl_handle) . ' - ' . curl_strerror(curl_errno($curl_handle)));
                }
                $stream = fopen('php://temp', 'w+');
                if (!$stream) {
                    throw new Exception('Could not open php://temp for writing.');
                }
                $url = parse_url($fname);
                $cred = $this->_credService[$url['host']];


                $options = array(
                    CURLOPT_URL => $fname,
                    // CURLOPT_SSL_VERIFYPEER => false, // don't verify SSL
                    // CURLOPT_SSL_VERIFYHOST => false,
                    //CURLOPT_FTP_SSL        => CURLFTPSSL_ALL, // require SSL For both control and data connections
                    //CURLOPT_FTPSSLAUTH     => CURLFTPAUTH_DEFAULT, // let cURL choose the FTP authentication method (either SSL or TLS)
//                CURLOPT_UPLOAD         => true,
                    // CURLOPT_PORT           => $port,
                    CURLOPT_TIMEOUT => 30,
                    //CURLOPT_FTPPORT        => '-',
                    CURLOPT_RETURNTRANSFER => 1,
                    CURLOPT_FILE => $stream
                );
                if($cred){
                    $options[CURLOPT_USERPWD]=$cred['username'] . ($cred['password'] ? ':' . $cred['password'] : '') ;
                }
                foreach ($options as $option_name => $option_value) {
                    if (!curl_setopt($curl_handle, $option_name, $option_value)) {
                        throw new Exception(sprintf('Could not set cURL option: %s', $option_name));
                    }
                }
                if (!curl_exec($curl_handle)) {
                    throw new Exception(sprintf('Could not download file %s - . cURL Error: [%s] - %s', $fname,
                        curl_errno($curl_handle), curl_error($curl_handle)));
                }
                rewind($stream);
                $ql->set(stream_get_contents($stream));
                $this->_cache->save($ql);
            } catch (Exception $e) {
                $this->_log->error(__METHOD__ . ' - ' . __LINE__ . ' - ' . $e->getMessage());
                header('HTTP/1.0 404 Not Found', true, 404);
                die;
            } finally {
                if (null !== $curl_handle) {
                    @curl_close($curl_handle);
                }
                if ($stream) {
                    fclose($stream);
                }
            }
        }
        $fileinfo = (new finfo())->buffer($ql->get(), FILEINFO_MIME);
        header('Content-type:' . $fileinfo, true);
        header('Expires: ' . $ql->getExpirationDate()->format('D, d M Y H:i:s') . ' GMT', true);
        die($ql->get());
    }

}

if (realpath(__FILE__) === realpath($_SERVER['SCRIPT_FILENAME'])) {
    $mode = 'xml';
    array_map(
        function ($item) use (&$data) {
            $t = explode('=', $item);
            //echo var_export($t, true);
            $data[$t[0]] = $t[1];
        },
        array_slice($argv, 1)
    );

    $plugin = new Ceda2ResponseHandler();

    $builder = new DI\ContainerBuilder();
    $builder->addDefinitions('config.php');
    $environment = array_key_exists('environment', $_SERVER) ? $_SERVER['environment'] : '';

    $builder->addDefinitions('config' . ($environment ? ".$environment" : "") . '.php');
    $builder->useAnnotations(true);
    $_container = $builder->build();
    $_container->injectOn($plugin);


    // Create default HandlerStack
    $stack = HandlerStack::create();
    $stack->push(
        new CacheMiddleware(
            new PrivateCacheStrategy(
                new FlysystemStorage(
                    new Local('/tmp/ceda2')
                ),
                180 // seconds to keep cache
            )
        ),
        'cache'
    );
    // Add this middleware to the top with `push`
    $stack->push(new CacheMiddleware(), 'cache');


    $client = new Client([
        // http://docs.guzzlephp.org/en/latest/request-options.html
        'allow_redirects' => [ // set to false to disable
            'max' => 5,
            'strict' => false,
            'referer' => false,
            'protocols' => ['http', 'https'],
            //'on_redirect'     => $onRedirect,
            'track_redirects' => true
        ],
        'timeout' => 10,
//           'debug' => true,
        'handler' => $stack,
        'verify' => true

    ]);

    # bbox=-103,17,-94,20
    # endDate=2017-03-11T00:00:00.000Z
    # maximumRecords=20
    # mission=Sentinel-1
    # startDate=2016-03-10T00:00:00.000Z
    # startPage=1
    $url = 'http://forestry-tep.eo.esa.int/opensearch/atom';
    if ($mode === 'json') {
        $url = 'http://forestry-tep.eo.esa.int/opensearch/json';
    }
    $res = $client->request('GET', $url, [
        'query' => $data
    ]);

    $body = $res->getBody();
    $result = $plugin->parse($res);
    echo json_encode($result);
}

