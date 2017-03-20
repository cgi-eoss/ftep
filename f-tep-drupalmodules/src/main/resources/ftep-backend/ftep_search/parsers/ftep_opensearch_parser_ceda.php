<?php

class CedaResponseHandler  {
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

    public function parse($response){
		$xml = new SimpleXMLElement($response);
		$namespaces = $xml->getNamespaces(true);
		if(isset($namespaces[""])){
			// register a prefix for the default namespace:
			$xml->registerXPathNamespace("default", $namespaces[""]);
		}

		foreach($this->namespaces as $n=>$ns){
			$xml->registerXPathNamespace($n, $ns);
		}
		$result=array(
				'totalResults' => (string)$xml->xpath('//os:totalResults')[0],
				'startIndex' =>(string) $xml->xpath('//os:startIndex')[0],
				'itemsPerPage' => (string)$xml->xpath('//os:itemsPerPage')[0],
		);

        $urls=array();
		foreach($xml->xpath('//default:entry') as $entry) {
			$entry = $this->setupNamespaces($entry);
			$ent['title'] = (string)$entry->title;
			// $ent['link'] = (string)$entry->link->attributes()->href;

			$ent['link'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->href;
			$ent['size'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->length;
            $ent['type'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->type;
            $id=(string)$entry->id;

            //                 <link href="http://opensearch-test.ceda.ac.uk/resource/gml?uid=ed61de3537b5cac2c4344cac01a77cac420dc786" rel="alternate" type="application/gml+xml"/>
            //
            $meta = (string)$entry->xpath('default:link[@type="application/gml+xml"]')[0]->attributes()->href;
            $ent['meta']=$meta;
            $urls[$id] = $meta;

            if(array_key_exists('georss', $namespaces)){
                $georss = $entry->children($namespaces['georss']);
                $ent['geo']=(string)$georss->polygon;
            }
                $result['entities'][$id]=$ent;
		}

        $xxx = $this->multicall( $urls );
        foreach($xxx as $k=>$v){
  //          die("<pre>".var_export($v,true));
            $v = new SimpleXMLElement($v);
            $v=$this->setupNamespaces($v);
			$v->registerXPathNamespace("default", $this->namespaces['eop20']);
            $identifier = (string)$v->xpath('//default:identifier')[0];
            $start = (string)$v->xpath('//gml:beginPosition')[0];
            $stop = (string)$v->xpath('//gml:endPosition')[0];
            $size = (string)$v->xpath('//default:size')[0];
            // $geom = (string)$v->xpath('//om:featureOfInterests')[0];

            $result['entities'][$k] = array( 'identifier' => $identifier, 'start' => $start, 'stop' => $stop , 'size'=> $size );
        }
		return $result;
	}
}

