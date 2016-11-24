<?php

class OpenSearchParser  {
	protected $_count;
	protected $_startPage;
	protected $_startIndex;
	protected $_searchTerms;
	protected $_start;
	protected $_stop;

	protected  $params ;

	protected $log;

	public function __construct(){
		// This is the list of parameters received from the client
		$this->params=array(
				"count"=>null,
				"startPage"=>null,
				"startIndex"=>null,
				"searchTerms"=>null,
		);
	}
    public function parseRequest($data){
        unset($data['q']);
        foreach($data as $k=>$v){
            //if( array_key_exists($k, $this->params) ){
    	//		$this->params = $v;
          //  }
            $this->params[$k]=$v;
        }

        foreach($this->params as $k=>$v){
            if(is_null($v)){ unset($this->params[$k]); }
        }
		return $this->params;
	}
	public function parse( $response){
		// need to convert the atom response back in json
		// @TODO invoke gearman to perform conversion from gml to wkt
		//$entries = simplexml_load_file($data);
		$xml = new SimpleXMLElement($response);

		$namespaces=array(
			""=> "http://www.w3.org/2005/Atom",
			"time" => "http://a9.com/-/opensearch/extensions/time/1.0/",
			"os" => "http://a9.com/-/spec/opensearch/1.1/",
			"purl" => "http://purl.org/dc/elements/1.1/",
			"georss" => "http://www.georss.org/georss",
		    "gml"=>"http://www.opengis.net/gml",
			"geo"=> "http://a9.com/-/opensearch/extensions/geo/1.0/",
		);
		foreach($namespaces as $n=>$ns){
			$xml->registerXPathNamespace($n, $ns);
		}

		foreach($xml->xpath('//os:totalResult') as $event) {
			print_r( $event,true );
		}

	}
}

