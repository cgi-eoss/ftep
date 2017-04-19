<?php

/**
 * Class RestoModel_Ftep_Output
 *
 * @property array $extendedProperties
 */
class RestoModel_Ftep_Output extends RestoModel
{


    /*
     * Properties mapping between RESTo model and input GeoJSON Feature file
     * 'propertyNameInInputFile' => 'restoPropertyName'
     */
    public $inputMapping = array(
        'properties.jobId'             => 'jobId',
        'properties.intJobId'          => 'intJobId',
        'properties.serviceName'       => 'serviceName',
        'properties.jobOwner'          => 'jobOwner',
        'properties.jobStartDate'      => 'jobStartDate',
        'properties.jobEndDate'        => 'jobEndDate',
        'properties.filename'          => 'filename',
        'properties.productIdentifier' => 'productIdentifier',
        'properties.ftepUrl'           => 'ftepUrl',
        'properties.wms'               => 'wms',
        'properties.resource'          => 'resource',
        'properties.resourceMimeType'  => 'resourceMimeType',
        'properties.resourceSize'      => 'resourceSize',
        'properties.resourceChecksum'  => 'resourceChecksum',
    );

    public $extendedProperties = array(
        'jobId'        => array(
            'name' => 'jobId',
            'type' => 'TEXT'
        ),
        'intJobId'     => array(
            'name' => 'intJobId',
            'type' => 'INTEGER'
        ),
        'serviceName'  => array(
            'name' => 'serviceName',
            'type' => 'TEXT'
        ),
        'jobOwner'     => array(
            'name' => 'jobOwner',
            'type' => 'TEXT'
        ),
        'jobStartDate' => array(
            'name' => 'jobStartDate',
            'type' => 'TIMESTAMP',
        ),
        'jobEndDate'   => array(
            'name' => 'jobEndDate',
            'type' => 'TIMESTAMP',
        ),
        'ftepparam'    => array(
            'name' => 'ftepparam',
            'type' => 'JSONB'
        ),
    );

    public $extendedSearchFilters = array(
        'jobId'        => array(
            'name'      => 'jobId',
            'type'      => 'TEXT',
            'osKey'     => 'jobId',
            'key'       => 'jobId',
            'operation' => '=',
            'title'     => 'Identifier of the job',
        ),
        'intJobId'     => array(
            'name'      => 'intJobId',
            'type'      => 'INTEGER',
            'osKey'     => 'intJobId',
            'key'       => 'intJobId',
            'operation' => '=',
            'title'     => 'Identifier of the job',
        ),
        'serviceName'  => array(
            'name'      => 'serviceName',
            'type'      => 'TEXT',
            'osKey'     => 'serviceName',
            'key'       => 'serviceName',
            'operation' => '=',
            'title'     => 'Identifier of the serviceName',
        ),
        'jobOwner'     => array(
            'name'      => 'jobOwner',
            'type'      => 'TEXT',
            'osKey'     => 'jobOwner',
            'key'       => 'jobOwner',
            'operation' => '=',
            'title'     => 'Owner of the job',
        ),
        'jobStartDate' => array(
            'name'  => 'jobStartDate',
            'type'  => 'TIMESTAMP',
            'index' => array(
                'type'      => 'btree',
                'direction' => 'DESC'
            )
        ),
        'jobEndDate'   => array(
            'name'  => 'jobEndDate',
            'type'  => 'TIMESTAMP',
            'index' => array(
                'type'      => 'btree',
                'direction' => 'DESC'
            )
        ),
        'ftepparam'    => array(
            'name'      => 'ftepparam',
            'type'      => 'JSONB',
            'osKey'     => 'ftepparam',
            'key'       => 'ftepparam',
            'operation' => '@>',
        ),
    );

    /*
    * Return property database column name
    *
    * @param string $modelKey : RESTo model key
    * @return array
    */
    public function getDbKey($modelKey)
    {
        if (!isset($modelKey, $this->properties[$modelKey]) || !is_array($this->properties[$modelKey])) {
            return null;
        }
        return $this->properties[$modelKey]['name'];
    }

    /**
     * Constructor
     */
    public function __construct()
    {
        parent::__construct();
        $this->searchFilters = array_merge($this->searchFilters, $this->extendedSearchFilters);
    }

    /**
     * Generate WMS url
     *
     * @param $properties
     * @return string
     */
    public function generateWMSUrl($properties)
    {
        return $properties['wms'];
    }
}