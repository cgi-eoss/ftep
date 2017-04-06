<?php

interface DatasourceProvider
{
    /**
     * Returns an array of datasources
     *
     * $datasources[  ] = array(
     *   'name' => $node->name,
     *   'endpoint' => $node->endpoint,
     *   'template' => $node->template,
     *   'parserClass' => $node->parser,
     *   );
     *
     * @param array $datasourceParameters
     * @return mixed
     */
    public function getDatasources(array $datasourceParameters);

}
