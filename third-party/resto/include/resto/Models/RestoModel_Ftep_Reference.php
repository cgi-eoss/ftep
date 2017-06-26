<?php

class RestoModel_Ftep_Reference extends RestoModel
{

    /*
     * Properties mapping between RESTo model and input GeoJSON Feature file
     * 'propertyNameInInputFile' => 'restoPropertyName'
     */
    public $inputMapping = array(
        'properties.productIdentifier' => 'productIdentifier',
        'properties.owner'             => 'owner',
        'properties.filename'          => 'filename',
        'properties.ftepUrl'           => 'ftepUrl',
        'properties.resource'          => 'resource',
        'properties.resourceMimeType'  => 'resourceMimeType',
        'properties.resourceSize'      => 'resourceSize',
        'properties.resourceChecksum'  => 'resourceChecksum',
        'properties.extraParams'       => 'ftepparam'
    );

    public $extendedProperties = array(
        'owner'     => array(
            'name' => 'owner',
            'type' => 'TEXT'
        ),
        'filename'  => array(
            'name' => 'filename',
            'type' => 'TEXT'
        ),
        'ftepUrl'   => array(
            'name' => 'ftepurl',
            'type' => 'TEXT'
        ),
        'ftepparam' => array(
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
            'title'     => 'Identifier of the reference data',
        ),
        'owner'             => array(
            'name'      => 'owner',
            'type'      => 'TEXT',
            'osKey'     => 'owner',
            'key'       => 'owner',
            'operation' => '=',
            'title'     => 'Owner of the reference data',
        ),
        'filename'          => array(
            'name'      => 'filename',
            'type'      => 'TEXT',
            'osKey'     => 'filename',
            'key'       => 'filename',
            'operation' => '=',
            'title'     => 'Reference data filename',
        ),
        'ftepparam'         => array(
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

}
