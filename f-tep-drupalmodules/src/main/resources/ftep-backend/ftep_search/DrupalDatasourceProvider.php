<?php

use DI\Annotation\Inject;

require_once __DIR__.'/DatasourceProvider.php';

class DrupalDatasourceProvider implements DatasourceProvider
{
    /**
     * @Inject("Psr\Log\LoggerInterface")
     */
    protected $_log;

    /**
     * @inheritdoc
     */
    public function getDatasources(array $datasourceParameters)
    {
        $this->_log->debug( __METHOD__.' - '.__LINE__.' Called with parameters : '.var_export($datasourceParameters,true) );

        drupal_bootstrap(DRUPAL_BOOTSTRAP_FULL);
        $query = new EntityFieldQuery;

        // Load datasources entities defined and enabled
        $query = $query->entityCondition('entity_type', 'ftep_datasource')->propertyCondition('enabled', 't') ;

        $source_type= array_shift($datasourceParameters);

        $this->_log->info(__METHOD__.' - ' .__LINE__. ' - Searching '.$source_type.' data');

        $query->propertyCondition('source_type',$source_type,'=');

        // Contains the list of the datasources enabled and ready to be used

        $results = $query->execute();

        $datasources=array();

        if (isset($results['ftep_datasource'])) {

            $nodes = entity_load('ftep_datasource',array_keys($results['ftep_datasource']));

            foreach ($nodes as $nid => $node) {

                // Do something with the node object
                $datasources[  ] = array(
                    'name' => $node->name,
                    'endpoint' => $node->endpoint,
                    'template' => $node->template,
                    'parserClass' => $node->parser,
                );
            }
        }
        return $datasources;
    }

}