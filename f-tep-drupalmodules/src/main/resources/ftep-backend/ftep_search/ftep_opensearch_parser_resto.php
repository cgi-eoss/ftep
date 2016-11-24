<?php

class RestoResponseHandler  {
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


		foreach($xml->xpath('//default:entry') as $entry) {
			$entry = $this->setupNamespaces($entry);
			$ent['title'] = (string)$entry->title;
			// $ent['link'] = (string)$entry->link->attributes()->href;

			$ent['link'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->href;
			$ent['size'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->length;
			$ent['type'] =  (string)$entry->xpath('default:link[@rel="enclosure"]')[0]->attributes()->type;


			$georss = $entry->children($namespaces['georss']);
			$ent['geo']=(string)$georss->polygon;

			$result['entities'][]=$ent;
		}
		return $result;
	}
}

