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
        'properties.productIdentifier' => 'productIdentifier',
        'properties.jobId'             => 'jobId',
        'properties.intJobId'          => 'intJobId',
        'properties.serviceName'       => 'serviceName',
        'properties.jobOwner'          => 'jobOwner',
        'properties.jobStartTime'      => 'jobStartDate',
        'properties.jobEndTime'        => 'jobEndDate',
        'properties.filename'          => 'filename',
        'properties.ftepUrl'           => 'ftepUrl',
        'properties.resource'          => 'resource',
        'properties.resourceMimeType'  => 'resourceMimeType',
        'properties.resourceSize'      => 'resourceSize',
        'properties.resourceChecksum'  => 'resourceChecksum',
        'properties.extraParams'       => 'ftepparam'
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
        'filename'     => array(
            'name' => 'filename',
            'type' => 'TEXT'
        ),
        'ftepUrl'      => array(
            'name' => 'ftepUrl',
            'type' => 'TEXT'
        ),
        'ftepparam'    => array(
            'name' => 'ftepparam',
            'type' => 'JSONB'
        ),
    );

    public $extendedSearchFilters = array(
        'productIdentifier' => array(
            'name'      => 'productIdentifier',
            'type'      => 'TEXT',
            'osKey'     => 'productIdentifier',
            'key'       => 'productIdentifier',
            'operation' => '=',
            'title'     => 'Identifier of the output product',
        ),
        'jobStartDate'      => array(
            'name'  => 'jobStartDate',
            'type'  => 'TIMESTAMP',
            'index' => array(
                'type'      => 'btree',
                'direction' => 'DESC'
            )
        ),
        'jobEndDate'        => array(
            'name'  => 'jobEndDate',
            'type'  => 'TIMESTAMP',
            'index' => array(
                'type'      => 'btree',
                'direction' => 'DESC'
            )
        ),
        'filename'          => array(
            'name'      => 'filename',
            'type'      => 'TEXT',
            'osKey'     => 'filename',
            'key'       => 'filename',
            'operation' => '=',
            'title'     => 'Output product filename',
        ),
        'ftepparam'         => array(
            'name'      => 'ftepparam',
            'type'      => 'JSONB',
            'osKey'     => 'ftepparam',
            'key'       => 'ftepparam',
            'operation' => '@>',
        ),

        'job:owner'           => array(
            'key'       => 'jobOwner',
            'osKey'     => 'job:owner',
            'operation' => '=',
            'title'     => 'Owner of the job',
        ),
        'job:id'              => array(
            'key'       => 'jobId',
            'osKey'     => 'job:id',
            'operation' => '=',
            'title'     => 'Identifier of the job',
        ),
        'job:integerId'       => array(
            'key'       => 'intJobId',
            'osKey'     => 'job:integerId',
            'operation' => '=',
            'title'     => 'Identifier of the job',
        ),
        'job:serviceName'     => array(
            'key'       => 'serviceName',
            'osKey'     => 'job:serviceName',
            'operation' => '=',
            'title'     => 'Identifier of the serviceName',
        ),
        'job:startDateAfter'  => array(
            'key'       => 'jobStartDate',
            'osKey'     => 'job:startDateAfter',
            'operation' => '>=',
            'title'     => 'Beginning of the time slice of the search query for job start date. Format should follow RFC-3339',
            'pattern'   => '^[0-9]{4}-[0-9]{2}-[0-9]{2}(T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]+)?(|Z|[\+\-][0-9]{2}:[0-9]{2}))?$'
        ),
        'job:startDateBefore' => array(
            'key'       => 'jobStartDate',
            'osKey'     => 'job:startDateBefore',
            'operation' => '<=',
            'title'     => 'End of the time slice of the search query for job start date. Format should follow RFC-3339',
            'pattern'   => '^[0-9]{4}-[0-9]{2}-[0-9]{2}(T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]+)?(|Z|[\+\-][0-9]{2}:[0-9]{2}))?$'
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

}