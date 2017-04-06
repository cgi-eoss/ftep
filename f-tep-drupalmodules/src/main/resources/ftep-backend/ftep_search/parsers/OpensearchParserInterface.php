<?php


interface OpensearchParserInterface
{
    /**
     * @param $config
     * @return array
     */
    public function setConfig($config );

    /**
     * @param \GuzzleHttp\Psr7\Response $response
     * @return mixed
     */
    public function parse(GuzzleHttp\Psr7\Response $response);

    /**
     * @param array $data
     * @return array
     */
    public function appendQueryParams(array $data);

    /**
     * @param array $data
     * @return string
     */
    public function appendURL(array $data);

    /**
     * @return array
     */
    public function aliasSearchParameters(array $data);

}