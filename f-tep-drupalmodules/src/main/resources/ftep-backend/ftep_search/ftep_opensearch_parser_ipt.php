2<?php

class IPTResponseHandler
{
    static protected $alias = array(
        'bbox' => 'box',
        'endDate' => 'completionDate',
        'name' => 'productIdentifier',
        'startPage' => 'page',
        'maximumRecords' => 'maxRecords',
        'maxCloudCoverPercentage' => 'cloudCover',
        //""=>"sortParam",
        //""=>"sortOrder"
        'tep' => '',
        'ref' => '',
        'sat' => ''
    );
    static protected $likeFields = array(
        'productIdentifier',
        'description',
    );
    static protected $rangeFields = array(
        'cloudCover',
    );
    /**
     * @Inject("Psr\Log\LoggerInterface")
     */
    protected $_log;

    /**
     * @Inject("CacheService");
     */
    protected $_cache;

    protected $_config;

    /**
     * @Inject("api.version")
     */
    protected $_apiVersion;

    /**
     * @param mixed $config
     */
    public function setConfig($config)
    {
        $this->_config = $config;
    }

    /**
     * This function translates parameters received from the F-TEP client into something
     * accepted (compliant with) the specific datasoruce (view the "/describe.xml")
     * moreover for some fields it might be necessary to apply and extra 'tewak' e.g. productIdentifier is enclosed in "%"
     * @param array $params
     * @return array
     */
    public function aliasSearchParameters(array $params)
    {
        $this->_log->debug(__METHOD__ . ' - ' . __LINE__);

        foreach (IPTResponseHandler::$alias as $k => $v) {
            if (array_key_exists($k, $params)) {
                $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Aliasing ' . $k . ' as ' . $v);
                $old = $params[$k];
                unset($params[$k]);
                if (null !== $v) {
                    if (in_array($v, IPTResponseHandler::$likeFields, true)) {
                        $params[$v] = '%' . $old . '%';
                    } else if (in_array($v, IPTResponseHandler::$rangeFields, true)) {
                        $params[$v] = '[0,' . $old .']';
                    } else {
                        $params[$v] = $old;
                    }
                }
            }
        }
        return $params;
    }

    /**
     * Appebnd a parameter to the url. For IPT it converts 'mission' into a collection
     * Thus:
     * @code
     * <url>?mission=sentinel-1&bbox=xxx  becomes <url>/sentinel1?bbox=xxx
     * @code
     *
     * @param array $data
     * @return string
     */
    public function appendURL(array $data)
    {
        $this->_log->debug(__METHOD__ . ' - ' . __LINE__);

        $result = '';
        if (array_key_exists('mission', $data)) {
            // Transform "mission" query parameter into a collection for Resto
            $result = '/' . str_replace('-', '', $data['mission']) . '/search.json';
        }
        return $result;
    }

    /**
     * @param array $data
     * @return array
     */
    public function appendQueryParams(array $data)
    {
        return array();
    }

    /**
     * @param \GuzzleHttp\Psr7\Response $data
     * @return array
     * @throws Exception
     */
    public function parse(GuzzleHttp\Psr7\Response $data)
    {
        $this->_log->debug(__METHOD__ . ' - ' . __LINE__);

        $result = array(
            'datasource' => 'IPT',
            'totalResults' => 0,
            'startIndex' => 0,
            'itemsPerPage' => 0,
            'entities' => array()
        );

        if ($this->_config['mode'] !== 'json') {
            // Only json query (i.e. RESTO json response) implemented for IPT driver
            throw new Exception("Not implemented");
        }

        $response = $data->getBody()->getContents();
        $response = json_decode($response);

        $result['totalResults'] = $response->properties->totalResults;
        $result['startIndex'] = $response->properties->startIndex;
        $result['itemsPerPage'] = $response->properties->itemsPerPage;

        // Array indexed with the url that will be called in parallel
        foreach ($response->features as $feat) {
            $item = array(
                'title' => basename($feat->properties->productIdentifier),
                'identifier' => basename($feat->properties->productIdentifier),
                'link' => 'httpipt://' . str_replace('/eodata/', '', $feat->properties->productIdentifier) . '.zip',
                'size' => $feat->properties->services->download->size,
                'meta' => $feat->properties->links[0]->href,
                'type' => 'application/unknown',
                'start' => $feat->properties->startDate,
                'stop' => $feat->properties->completionDate,
                'geo' => $feat->geometry,
                'ql' => '/secure/api/v'.$this->_apiVersion.'/ql/ipt/'.parse_url($feat->properties->thumbnail)['path'],
                'details' => array(
                    'misc' => '', //$feat
                    'data_format' => array('format' => 'SAFE'),
                    'temporal' => (object)array(
                        'end_time' => $feat->properties->startDate,
                        'start_time' => $feat->properties->startDate,
                    ),
                ),

            );
            $result['entities'][] = $item;
        }
        return $result;
    }

    public function quicklook( $quickLook ){
        $ql = $this->_cache->getItem($quickLook);
        if (!$ql->isHit()) {
            $fname = $this->_config['qlEndpoint'] . '/' . $quickLook;
            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' - Cache miss .. fetching ' . $quickLook);
            try {
                $client = new GuzzleHttp\Client();
                $res = $client->request('GET', $fname);
                echo $res->getBody();

                $stream = fopen('php://temp', 'w+');
                if (!$stream) {
                    throw new Exception('Could not open php://temp for writing.');
                }
                fwrite($stream, $res->getBody());
                rewind($stream);
                $ql->set(stream_get_contents($stream));
                $this->_cache->save($ql);
            } catch (Exception $e) {
                $this->_log->error(__METHOD__ . ' - ' . __LINE__ . ' - ' . $e->getMessage());
                header('HTTP/1.0 404 Not Found', true, 404);
                die;
            } finally {
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
