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
use Monolog\Formatter\LineFormatter;
use Monolog\Handler\StreamHandler;
use Monolog\Logger;

class Ceda2ResponseHandler
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

//    private     $_mode="json";
    private $_mode = 'xml';

    public function __construct()
    {
        $this->_log = new Logger( basename(__FILE__) );
        $this->_log->pushHandler(new StreamHandler('/var/log/ftep.log', Logger::DEBUG));
    }

    /**
     * @param \GuzzleHttp\Psr7\Response $data
     * @return array
     * @throws \LogicException
     */
    public function parse( $data)
    {
        $response = $data;
        if ($data instanceof GuzzleHttp\Psr7\Response) {
            $response = $data->getBody();
        }

        $result = [
            'datasource' => 'CEDA2',
            'totalResults' => 0,
            'startIndex' => 0,
            'itemsPerPage' => 0,
            'entities' => []
        ];

        if ($this->_mode === 'json') {
            $response = json_decode($response);

            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Parsing JSON response ');

            $result['totalResults'] = $response->totalResults;
            $result['startIndex'] = $response->startIndex;
            $result['itemsPerPage'] = 10;
            $result['entities'] = array_map(
                function ($item) {
                    $fname = basename($item->file->filename, '.manifest');
                    $item = [
                        'title' => $item->file->filename,
                        'link' => $item->file->directory . '/' . $fname . '.zip',
                        'size' => $item->file->data_file_size,
                        'type' => 'application/unknown',
                        'meta' => null,
                        'identifier' => $fname,
                        'start' => $item->temporal->start_time,
                        'stop' => $item->temporal->end_time,
                        'geo' => $item->spatial->geometries->display,
                        'details' => [
                            'data_format' => ['format' => 'SAFE'],
                        ],
                        'temporal' => $item->temporal,
                        'thumbnail' => $item->file->quicklook_file,
                        'location' => $item->file->location,
                        'misc' => $item
                    ];
                    return $item;
                }
                , $response->rows
            );
        }  else {
            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Parsing ATOM response ');

            $xml = $response ;
            if (is_string($response)) {
                $xml = simplexml_load_string($response);
            }

            $result = array(
                'datasource' => 'CEDA2',
                'totalResults' => (string)$xml->xpath('//os:totalResults')[0],
                'startIndex' => (string)$xml->xpath('//os:startIndex')[0],
                'itemsPerPage' => (string)$xml->xpath('//os:itemsPerPage')[0],
                'entities' => []
            );

            $namespaces = $xml->getNamespaces(true);
            // register a prefix for the default namespace:
            isset($namespaces[""]) && $xml->registerXPathNamespace('default', $namespaces['']) ;

            $entitiesArray = $result['entities'];

            $requests = array();

            array_map(
                function (SimpleXMLElement $entity) use (&$entitiesArray, &$requests) {
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

                    $link_node = $entity->xpath('./default:link[@rel="enclosure" and @title="ftp"]/@href');
                    if ($link_node && $link_node[0]) {
                        $ent['link'] = (string)$link_node[0]->href;
                    }

                    $ent['type'] = $this->getTagAttribute('default:link[@rel="enclosure"]', $entity, 'type');
//                    $ent['meta']= str_ireplace("opensearch-test.ceda.ac.uk","forestry-tep.eo.esa.int", $ent['meta']);
                    $requests[] = new Request('GET', $ent['meta']);

                    $entitiesArray[] = $ent;

                }, $xml->xpath('//default:entry')
            );

            $result['entities'] = $entitiesArray;

//            // Create default HandlerStack
//            $stack = HandlerStack::create();
//            $stack->push(
//                new CacheMiddleware(
//                    new PrivateCacheStrategy(
//                        new FlysystemStorage(
//                            new Local('/tmp/ceda2')
//                        ),
//                        180 // seconds to keep cache
//                    )
//                ),
//                'cache'
//            );
            //$stack->push(new CacheMiddleware(), 'cache');

            $client = new GuzzleHttp\Client([
                'timeout' => 10, // seconds
                'connect_timeout' => 3, // seconds
                //'handler' => $stack
            ]);

            $pool = new Pool(
                $client,
                $requests, [
                'concurrency' => 10,
                'fulfilled' => function (GuzzleHttp\Psr7\Response $response, $index) use (&$result) {
                    $details = json_decode($response->getBody());
                    $result['entities'][$index]['details'] = $details;
                    $result['entities'][$index]['size'] = $details->file->data_file_size;
                    $result['entities'][$index]['title'] = $details->misc->product_info->Name;
                    $result['entities'][$index]['identifier']= $details->misc->product_info->Name;
                    $result['entities'][$index]['start']= $details->temporal->start_time;
                    $result['entities'][$index]['stop'] = $details->temporal->end_time;
                    $result['entities'][$index]['geo'] = $details->spatial->geometries->display;

                    $result['entities'][$index]['ql'] = '/ql/'.$details->file->quicklook_file;
                },
                'rejected' => function ($reason, $index) {
                    // this is delivered each failed request
                    $this->_log->err(__METHOD__ . ' - ' . __LINE__ . ' - Failed getting info for item ' . $index . ' . -  ' . $reason);
                },
            ]);
            $promise = $pool->promise();
            $promise->wait();
        }
        return $result;
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

