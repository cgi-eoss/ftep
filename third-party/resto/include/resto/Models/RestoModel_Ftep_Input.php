<?php

class RestoModel_Ftep_Input extends RestoModel
{

    /*
     * Properties mapping between RESTo model and input GeoJSON Feature file
     * 'propertyNameInInputFile' => 'restoPropertyName'
     */
    public $inputMapping = array(
        'properties.productIdentifier' => 'productIdentifier',
        'properties.productSource'     => 'productSource',
        'properties.ftepUrl'           => 'ftepUrl',
        'properties.originalUrl'       => 'originalUrl',
        'properties.extraParams'       => 'ftepparam'
    );

    public $extendedProperties = array(
        'productIdentifier'     => array(
            'name' => 'productIdentifier',
            'type' => 'TEXT'
        ),
        'productSource'  => array(
            'name' => 'productSource',
            'type' => 'TEXT'
        ),
        'ftepparam' => array(
            'name' => 'ftepparam',
            'type' => 'JSONB'
        ),
    );

    public $extendedSearchFilters = array(
        'productIdentifier'     => array(
            'name'      => 'productIdentifier',
            'type'      => 'TEXT',
            'osKey'     => 'productIdentifier',
            'key'       => 'productIdentifier',
            'operation' => '=',
            'title'     => 'Product identifier',
        ),
        'productSource'  => array(
            'name'      => 'productSource',
            'type'      => 'TEXT',
            'osKey'     => 'productSource',
            'key'       => 'productSource',
            'operation' => '=',
            'title'     => 'Product source',
        ),
        'ftepparam' => array(
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