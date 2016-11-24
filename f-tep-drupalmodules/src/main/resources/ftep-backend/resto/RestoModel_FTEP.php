<?php
/**
 * RESTo FTEP model for FTEP project
 */
class RestoModel_FTEP extends RestoModel {

    public $extendedProperties = array(
        'jobid' => array(
            'name' => 'jobid',
            'type' => 'INTEGER'
        )
    );
    
    /** 
     * Generate download url
     * @param $properties
     * @return string
     */
    /*
    public function generateDownloadUrl($properties){
        //die("<pre>".var_export($properties,true));
        if(isset($properties['identifier'])){
            //https://192.171.139.83/secure/api/v1.0/download?j=507&f=FTEP_S2_NDVI_B4_B8_20161031_150901Z.tif
            return 'http://peps.mapshup.com/resto/collections/S2/' . $properties['identifier'] . '/download';
        }
        return null;
    }
*/
    /*
     * Properties mapping between RESTo model and input
     * GeoJSON Feature file
     */
    public $inputMapping = array(
        'properties.productId'      => 'productIdentifier',
        'properties.satellite'      => 'platform',
        'properties.sensorMode'   => 'sensorMode',
        'properties.quicklook'       => 'quicklook',
        'properties.wms'        => 'wms',
        //'properties.resourceMimeType' => 'resourceMime',
        //'properties.resource' => 'resourceUrl',
        'properties.resourceSize' => 'size',
        'properties.jobid' => 'jobid',

    );

    /**
     * Constructor
     */
    public function __construct() {
        parent::__construct();
    }
}
