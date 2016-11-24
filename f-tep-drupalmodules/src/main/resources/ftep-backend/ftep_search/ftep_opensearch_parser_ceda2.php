<?php
require __DIR__ . '/vendor/autoload.php';
use Monolog\Logger;
use Monolog\Handler\StreamHandler;
use \Curl\Curl;

class Ceda2ResponseHandler  {
    protected $_log;

    public function __construct(){
        $this->_log = new Logger( basename(__FILE__) );
        $this->_log->pushHandler(new StreamHandler('/var/log/ftep.log', Logger::DEBUG));
    }
	protected $namespaces=array(
			""=> "http://www.w3.org/2005/Atom",
			"time" 	=> "http://a9.com/-/opensearch/extensions/time/1.0/",
			"geo"	=> "http://a9.com/-/opensearch/extensions/geo/1.0/",
			"os" 	=> "http://a9.com/-/spec/opensearch/1.1/",
			"eo"	=> "http://a9.com/-/opensearch/extensions/eo/1.0/",
			"dc"	=> "http://purl.org/dc/elements/1.1/",
			"georss"=> "http://www.georss.org/georss",
			"gml"	=> "http://www.opengis.net/gml",
			"metalink" => "urn:ietf:params:xml:ns:metalink",
			"xlink"	=> "http://www.w3.org/1999/xlink",
            "media"	=> "http://search.yahoo.com/mrss/",
            'eop20' => "http://www.opengis.net/eop/2.0"
        );


    private function getTagAttribute($xpath, $xml, $attribute){
        $result = null;
        $link_tag =  $xml->xpath($xpath);
        if($link_tag && is_array($link_tag) ){
            $result= (string)$link_tag[0]->attributes()->{$attribute};
        }
        return $result;
    }

	private function setupNamespaces(SimpleXMLElement $x){
		$namespaces = $x->getNamespaces(true);
		if(isset($namespaces[""])){
			// register a prefix for the default namespace:
			$x->registerXPathNamespace("default", $namespaces[""]);
		}

		foreach($this->namespaces as $n=>$ns){
			$x->registerXPathNamespace($n, $ns);
		}
		return $x;
	}

    public function multicall($data, array $opts=array()  ){
        $chs=array();
        $results=array();

        $mh = curl_multi_init();
        $result=array();
        foreach($data as $id=>$d){
            // $chs[$id] = curl_init();
            $chs[$id] = array('handle'=>curl_init(), 'data'=>$d['payload'] );
            $url = ( is_array($d) && !empty($d['url']) ) ? $d['url'] : $d;
            curl_setopt($chs[$id]['handle'], CURLOPT_URL, $url);
            curl_setopt($chs[$id]['handle'], CURLOPT_HEADER,0);
            curl_setopt($chs[$id]['handle'], CURLOPT_RETURNTRANSFER, true );
            // extra options?
            if (!empty($options)) {
                curl_setopt_array($curly[$id], $options);
            }
            curl_multi_add_handle($mh, $chs[$id]['handle']);
        }
        // execute the handles
        $running = null;
        do {
            curl_multi_exec($mh, $running);
        } while($running > 0);

        // get content and remove handles
        foreach($chs as $id => $c) {
            //$result[$id] = curl_multi_getcontent($c['handle']);
            $result[$id] = array( 'data'=>curl_multi_getcontent($c['handle']),'payload'=>$c['data']  );
            //$result[$id."_1"] = array( 'data'=>$result[$id], 'payload'=>$opts);
            curl_multi_remove_handle($mh, $c['handle']);
        }
        // all done
        curl_multi_close($mh);
        return $result;
    }

    public function parse(SimpleXMLElement $xml){
        $this->_log->debug(__METHOD__." - ".__LINE__. " - Parsing response " );
        
        //$xml = new SimpleXMLElement($response);
		$namespaces = $xml->getNamespaces(true);
		if(isset($namespaces[""])){
			// register a prefix for the default namespace:
			$xml->registerXPathNamespace("default", $namespaces[""]);
		}

		foreach($this->namespaces as $n=>$ns){
			$xml->registerXPathNamespace($n, $ns);
        }
        //die("<pre>".var_export($response,true));
		$result=array(
				'totalResults' => (string)$xml->xpath('//os:totalResults')[0],
				'startIndex' =>(string) $xml->xpath('//os:startIndex')[0],
				'itemsPerPage' => (string)$xml->xpath('//os:itemsPerPage')[0],
		);

        $urls=array();
		foreach($xml->xpath('//default:entry') as $entry) {
			$entry = $this->setupNamespaces($entry);
            $ent['title'] = (string)$entry->title;
            /*
			$ent['link'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->href;
			$ent['size'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->length;
            $ent['type'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->type;
            $id=(string)$entry->id;
            $meta = (string)$entry->xpath('default:link[@type="application/json"]')[0]->attributes()->href;
             */

            $ent['link'] = $this->getTagAttribute('default:link[@rel="enclosure"]', $entry, 'href');
            $ent['size'] = $this->getTagAttribute('default:link[@rel="enclosure"]', $entry, 'length');
            $ent['type'] = $this->getTagAttribute('default:link[@rel="enclosure"]', $entry, 'type');
            $id=(string)$entry->id;
            $meta = $this->getTagAttribute('default:link[@type="application/json"]',$entry,'href');
            $ent['meta']=$meta;

            // $urls[$id] = $meta;
            $urls[$id] = array( 'url'=> $meta, 'payload'=>$ent);

            if(array_key_exists('georss', $namespaces)){
                $georss = $entry->children($namespaces['georss']);
                $ent['geo']=(string)$georss->polygon;
            }
		}
        $xxx = $this->multicall( $urls );
        foreach($xxx as $k=>$v){
            $ent=$v['payload'];
            $v = is_array($v) ? $v['data'] : $v;
            $data=json_decode($v);

            $start = $data->temporal->start_time;
            $stop = $data->temporal->end_time;
            $size = $data->file->size;
            $fname=$data->file->filename;
            $entr =  array( 
                'identifier'=> $fname,
                'start'=> $start,
                'stop' => $stop,
                'size'=>$size,
                //'geo'=> $data->spatial->geometries->search
                'geo'=> $data->spatial->geometries->display
            );
            $entr=array_merge($ent,$entr);
            $param_list=array('data_format', 'temporal', 'misc','file');
            $data->file->path="ftp://ftp.ceda.ac.uk".$data->file->path;
            foreach($param_list as $p ){
                $entr['details'][$p] =  $data->$p ;
            }
            $result['entities'][] = $entr;
        }
		return $result;
	}
}

