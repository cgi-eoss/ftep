<?php
use Monolog\Logger;
use Monolog\Handler\StreamHandler;

class IPTResponseHandler {
    protected $_log;

    static protected $alias=array(
        'bbox' => 'box',
        'endDate' => 'completionDate',
        //  'platform' => null,
        'name' => 'productIdentifier',
        'startPage' => 'page',
        ''=>'maxRecords',
        //""=>"sortParam",
        //""=>"sortOrder"
        'tep'=>'',
        'ref'=>'',
        'sat'=>''
    );
    protected $likeFields=array(
        'productIdentifier',
        'description',
    );
    // Sentinel-1 => Sentinel1
    // Sentinel-2 => Sentinel2
    // Sentinel-3 => Sentinel3
    // Landsat-5
    // Landsat-7
    // Landsat-8
    // Envisat
    //
    //
    public function __construct(){
        $this->_log = new Logger( basename(__FILE__) );
        $this->_log->pushHandler(new StreamHandler('/var/log/ftep.log', Logger::DEBUG));
    }

    /**
     * This function tranlsates parameters received from the F-TEP client into something
     * accepted (compliant with) the specific datasoruce (view the "/describe.xml")
     * moreover for some fields it might be necessary to apply and extra 'tewak' e.g. productIdentifier is enclosed in "%"
     */
    public function aliasSearchParameters($params){

        foreach(IPTResponseHandler::$alias as $k=>$v){
            if( array_key_exists($k, $params) ){
                $this->_log->debug(__METHOD__.' - '.__LINE__. ' - Aliasing $k as $v' );
                $old=$params[$k];
                unset( $params[$k] );
                if(null !== $v ){
                    if( in_array($v,$this->likeFields) ){
                        $params[$v] = '%'.$old.'%';
                    } else {
                        $params[$v] = $old;
                    }
                }
            }
        }
        return $params;
    }
    public function appendURL($data){
        if( array_key_exists('mission', $data)){
            // Transform "mission" query parameter into a collection for Resto
            return '/'.str_replace('-','',$data['mission']).'/search.json';
        }
        return '';
    }
    public function parse( $response ){
        $result = array(
            'datasource'=>'IPT',
            'totalResults'=>0,
            'startIndex'=>0,
            'itemsPerPage'=>0,
            'entities'=>array()
        );

        $result['totalResults']= $response->properties->totalResults;
        $result['startIndex']= $response->properties->startIndex;
        $result['itemsPerPage']= $response->properties->itemsPerPage;

        // Array indexed with the url that will be called in parallel
        foreach(  $response->features as $feat  ){
            $item=array(
                //"title"=>$feat->properties->title,
                'title'=>basename( $feat->properties->productIdentifier),
                //"link"=>$feat->properties->services->download->url,
                'link'=>'httpipt://'.str_replace('/eodata/','',$feat->properties->productIdentifier).'.zip',
                'size'=>$feat->properties->services->download->size,
                'type'=>'application/unknown',
                'meta'=>$feat->properties->links[0]->href,
                //"identifier"=>$feat->id,
                'identifier'=>basename( $feat->properties->productIdentifier),
                'start'=>$feat->properties->startDate,
                'stop'=>$feat->properties->completionDate,
                'geo'=>$feat->geometry,
                'details'=>array(
                    'data_format'=>array('format'=>'SAFE'),
                ),
                'temporal'=>(object)array(
                    'end_time'=>$feat->properties->startDate,
                    'start_time'=>$feat->properties->startDate,
                ),
                'thumbnail'=>$feat->properties->thumbnail,
                'misc'=>''//$feat
            );
            $result['entities'][] = $item;
        }
        return $result;
    }
}
