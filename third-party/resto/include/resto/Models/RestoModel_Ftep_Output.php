<?php

class RestoModel_Ftep_Output extends RestoModel {
    /*
     * Properties mapping between RESTo model and input GeoJSON Feature file
     */
    public $inputMapping = array(
        'properties.jobid'          => 'jobid',
        'properties.userid'         => 'userid',
        //'properties.job_start_time' => 'resource',
        'properties.servicename'    => 'servicename',
    );
    public $extendedSearchFilters = array(
//        'eo:description' => array (
//            'key'           => 'description',
//            'osKey'         => 'description',
//            'operation'     => '=',
//            'htmlFilter'    => false
//        ),
//        'ftepparam' => array(
//            'name'          => 'ftepparam',
//            'type'          => 'JSONB',
//            'osKey'         => 'ftepparam',
//            'key'           => 'ftepparam'
//        ,'operation'    => '@>',
//        ),
//        'fname' => array (
//            'key'           => 'productIdentifier',
//            'osKey'         => 'productIdentifier',
//            'operation'     => '='
//        ),
        'jobid' => array(
            'name'          => 'jobid',
            'type'          => 'TEXT',
            'osKey'         => 'jobid',
            'key'           => 'jobid',
            'operation'     => '=',
            'title'         => 'Identifier of the job',
            //'pattern'       => '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[8-9a-bA-B][0-9a-fA-F]{3}-[0-9a-fA-F]{12}',
        ),
        'userid' => array(
            'name'          => 'userid',
            'type'          => 'TEXT',
            'osKey'         => 'userid',
            'key'           => 'userid',
            'operation'    => '=',
            'title'         => 'Identifier of the user',
        ),
        'servicename' => array(
            'name'          => 'servicename',
            'type'          => 'TEXT',
            'osKey'         => 'servicename',
            'key'           => 'servicename',
            'operation'    => '=',
            'title'         => 'Identifier of the servicename',
        ),
    );

    public $extendedProperties = array (
        'jobid' => array(
            'name'      => 'jobid',
            'type'      => 'TEXT',
            'operation' => '='
        ),
        'userid' => array(
            'name'      => 'userid',
            'type'      => 'INTEGER',
            'operation' => '='
        ),
        'servicename' => array(
            'name'      => 'servicename',
            'type'      => 'TEXT',
            'operation' => '='
        )
    );
    /*
    * Return property database column name
    *
    * @param string $modelKey : RESTo model key
    * @return array
    */
    public function getDbKey($modelKey) {
        if (!isset($modelKey,$this->properties[$modelKey]) || !is_array($this->properties[$modelKey])) {
            return null;
        }
        return $this->properties[$modelKey]['name'];
    }

    /**
     * Generate the absolute path for RO products used for download feature
     *
     * @param $properties
     * @return string
     */
    public function generateResourcePath($properties) {
        return "generateResourcePath";
    }
    /**
     * Generate the dynamic relative path for RO quicklooks
     *
     * @param $properties
     * @return string relative
     */
    public function generateQuicklookPath($properties) {
        return "generateQuicklookPath";
    }
    /**
     * Generate the dynamic relative path for RO thumbnails
     *
     * @param $properties
     * @return string relative path in the form of YYYYMMdd/thumbnail_filename with YYYYMMdd is the formated startDate parameter
     */
    public function generateThumbnailPath($properties) {
        return "generateThumbnailPath";
    }

    /**
     * Constructor
     */
    public function __construct() {
        parent::__construct();
        $this->searchFilters = array_merge($this->searchFilters, $this->extendedSearchFilters);
    }

    /**
     * Add feature to the {collection}.features table following the class model
     *
     * @param array $data : array (MUST BE GeoJSON in abstract Model)
     * @param string $collectionName : collection name
     * @return RestoFeature  feature
     */
    public function storeFeature($data, $collectionName) {
        return parent::storeFeature( $data, $collectionName);
    }

    /**
     * Generate WMS url
     *
     * @param $properties
     * @return string
     */
    public function generateWMSUrl($properties) {
        return "http://generateWMSUrl";
    }
    /**
     * Generate landsat download url
     *
     * @param $properties
     * @return string
     */
    public function generateDownloadUrl($properties) {
        return "http://ftep/".$properties['productIdentifier'];
    }


}