<?php

use DI\Annotation\Inject;


require_once __DIR__.'/DatasourceProvider.php';

class FileDatasourceProvider implements DatasourceProvider
{
    /**
     * @Inject("Psr\Log\LoggerInterface")
     */
    private $_log;

    /**
     * @Inject("configFile")
     */
    protected $_config;

    public function loadDataSource( $datasourceName )
    {
        $datasources = null;

        try {
            if( array_key_exists( $datasourceName, $this->_config['datasources'])){
                $datasources=$this->_config['datasources'][$datasourceName];
            }

        } catch (ParseException $e) {
            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' Unable to parse the configuration file '
                .$this->_config.' : '. $e->getMessage());

        } finally{
            return $datasources;
        }

    }

    /**
     *
     * @inheritdoc
     */
    public function getDatasources(array $datasourceParameters)
    {
        $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' Called with parameters : ' . var_export($datasourceParameters, true));

        $datasources = array();
        try {
            $mode = array_shift($datasourceParameters);

            foreach ( $this->_config['datasources'] as $nid => $node) {
                if( !( ($node['enabled']=='true') && ($node['source_type']===$mode ))){
                    $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' Skipping '.$nid);
                    continue;
                }

                $datasources[] = array(
                    'name' => $nid,
                    'endpoint' => $node['endpoint'],
                    //'template' => $node['template'],
                    'parserClass' => $node['parserClass'],
                    'config'   => array_key_exists('config',$node) ? $node['config'] : null
                );

            }
        } catch (ParseException $e) {
            $this->_log->debug(__METHOD__ . ' - ' . __LINE__ . ' Unable to parse the configuration file '
                .$this->_config.' : '. $e->getMessage());
        } finally{

            return $datasources;
        }
    }
}