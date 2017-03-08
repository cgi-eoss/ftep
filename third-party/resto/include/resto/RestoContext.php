<?php
/*
 * Copyright 2014 Jérôme Gasperi
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/*
 *
 */
class RestoContext {

    /**
     * Base url
     */
    public $baseUrl = '//localhost/';

    /**
     * Database driver
     */
    public $dbDriver;

    /**
     * Dictionary
     */
    public $dictionary;

    /**
     * HTTP method
     */
    public $method = 'GET';

    /**
     * Mail configuration
     */
    public $mail = array();

    /**
     * Available modules list/configuration
     */
    public $modules = array();

    /**
     * Format
     */
    public $outputFormat = 'json';

    /**
     * Path
     */
    public $path = '';

    /**
     * Query
     */
    public $query = array();

    /*
     * JSON Web Token duration (in seconds)
     */
    private $tokenDuration = 3600;

    /*
     * JSON Web Token accepted encryption algorithms
     */
    private $tokenEncryptions = array('HS256');

    /**
     * Constructor
     *
     * @param array $config : configuration extracted from config.php file
     * @throws Exception
     */
    public function __construct($config) {

        /*
         * JSON Web Token is mandatory
         */
        if (!isset($config['general']['passphrase'])) {
            RestoLogUtil::httpError(4000);
        }

        /*
         * Set variables
         */
        $this->configure($config);

        /*
         * Initialize objects
         */
        $this->initialize($config);

    }

    /**
     * Return complete url
     *
     * @param boolean $withparams : true to return url with parameters (i.e. with ?key=value&...) / false otherwise
     */
    public function getUrl($withparams = true) {
        return $this->baseUrl . '/' . $this->path . '.' . $this->outputFormat . (isset($withparams) ? RestoUtil::kvpsToQueryString($this->query) : '');
    }

    /**
     * Create a Json Web Token
     *
     * @param string $identifier
     * @param json $jsonData
     * @return string
     */
    public function createJWT($identifier, $jsonData) {
        return JWT::encode(array(
            'iss' => 'resto:server',
            'sub' => $identifier,
            'iat' => time(),
            'exp' => time() + $this->tokenDuration,
            'data' => $jsonData
        ), $this->passphrase);
    }

    /**
     * Decode and verify signed JSON Web Token
     *
     * @param string $token
     * @return array
     */
    public function decodeJWT($token) {
        return JWT::decode($token, $this->passphrase, $this->tokenEncryptions);
    }

    /**
     * Decode and verify signed JSON Web Token
     *
     * @param string $token
     * @return array
     */
    public function checkJWT($token) {

        if (!isset($token)) {
            return false;
        }

        try {

            $profile = json_decode(json_encode((array) $this->decodeJWT($token)), true);

            /*
             * Token is valid - i.e. signed by server and still in the validity period
             * Check if it is not revoked
             */
            if (isset($profile['data']['email']) && !$this->dbDriver->check(RestoDatabaseDriver::TOKEN_REVOKED, array('token' => $token))) {
                return true;
            }
            else {
                return false;
            }

        } catch (Exception $ex) {
            return false;
        }
    }

    /**
     * Initialize context variable
     *
     * @param array $config configuration
     */
    private function initialize($config) {

        /*
         * Initialize path
         */
        $this->setPath();

        /*
         * Initialize output format
         */
        $this->setOutputFormat();

        /*
         * Initialize database driver
         */
        $this->setDbDriver($config['database']);

        /*
         * Initialize dictionary
         */
        $this->setDictionary();

        /*
         * Initialize server endpoint url
         */
        $this->setBaseURL($config['general']['rootEndpoint'], $config['general']['protocol']);

        /*
         * Initialize query array
         */
        $this->setQuery();

    }

    /**
     * Set configuration variables
     *
     * @param array $config configuration
     */
    private function configure($config) {

        /*
         * Set TimeZone
         */
        date_default_timezone_set(isset($config['timezone']) ? $config['timezone'] : 'Europe/Paris');

        /*
         * HTTP Method is one of GET, POST, PUT or DELETE
         */
        $this->method = strtoupper(filter_input(INPUT_SERVER, 'REQUEST_METHOD', FILTER_SANITIZE_STRING));

        /*
         * Get general configuration
         */
        foreach (array_values(array(
            'htmlSearchUrl',
            'languages',
            'osDescription',
            'passphrase',
            'resetPasswordUrl',
            'storeQuery',
            'streamMethod',
            'title',
            'tokenDuration',
            'tokenEncryptions',
            'uploadDirectory',
            'userAutoValidation'
            )) as $key) {
            if (isset($config['general'][$key])) {
                $this->$key = $config['general'][$key];
            }
        }

        /*
         * Mail configuration
         */
        if (isset($config['mail'])) {
            $this->mail = $config['mail'];
        }

        /*
         * Initialize modules
         */
        $this->setModules($config['modules']);

    }

    /**
     * Get url with no parameters
     * Note that trailing '/' is systematically removed
     *
     * @param string $endPoint
     * @param string $protocol
     *
     * @return string $endPoint
     */
    private function setBaseURL($endPoint, $protocol) {
        if ($protocol === 'auto') {
            $https = filter_input(INPUT_SERVER, 'HTTPS', FILTER_SANITIZE_STRING);
            $protocol = isset($https) && $https === 'on' ? 'https' : 'http';
        }
        $host = filter_input(INPUT_SERVER, 'HTTP_HOST', FILTER_SANITIZE_STRING);
        $this->baseUrl = $protocol . '://' . $host . (substr($endPoint, -1) === '/' ? substr($endPoint, 0, strlen($endPoint) - 1) : $endPoint);
    }

    /**
     * Set query parameters from input
     */
    private function setQuery() {

        /*
         * Aggregate input parameters
         *
         * Note: PUT is handled by RestoUtil::readInputData() function
         */
        $query = array();
        switch ($this->method) {
            case 'GET':
            case 'DELETE':
            case 'POST':
                $query = RestoUtil::sanitize($_GET);
                break;
            default:
                break;
        }

        /*
         * Remove unwanted parameters
         */
        if (isset($query['RESToURL'])) {
            unset($query['RESToURL']);
        }

        /*
         * Trim all values and remove empty values
         */
        foreach ($query as $key => $value) {
            $query[$key] = trim($value);
            if ($query[$key] === '') {
                unset($query[$key]);
            }
        }

        $this->query = $query;

    }

    /**
     * Set dictionary from input language - default is english
     */
    private function setDictionary() {

        $lang = filter_input(INPUT_GET, 'lang', FILTER_SANITIZE_STRING);

        if (!isset($lang) || !in_array($lang, $this->languages) || !class_exists('RestoDictionary_' . $lang)) {
            $lang = 'en';
        }

        $this->dictionary = RestoUtil::instantiate('RestoDictionary_' . $lang, array($this->dbDriver));

    }

    /**
     * Set Database driver
     *
     * @param array $databaseConfig
     */
    private function setDbDriver($databaseConfig) {

        /*
         * Database
         */
        if (!class_exists('RestoDatabaseDriver_' . $databaseConfig['driver'])) {
            RestoLogUtil::httpError(4002);
        }
        try {
            $databaseClass = new ReflectionClass('RestoDatabaseDriver_' . $databaseConfig['driver']);
            if (!$databaseClass->isInstantiable()) {
                throw new Exception();
            }
        } catch (Exception $e) {
            RestoLogUtil::httpError(4003);
        }

        $this->dbDriver = $databaseClass->newInstance($databaseConfig, new RestoCache(isset($databaseConfig['dircache']) ? $databaseConfig['dircache'] : null));
    }

    /**
     * Set REST path
     */
    private function setPath() {
        $restoUrl = filter_input(INPUT_GET, 'RESToURL', FILTER_SANITIZE_STRING);
        if (isset($restoUrl)) {
            $this->path = substr($restoUrl, -1) === '/' ? substr($restoUrl, 0, strlen($restoUrl) - 1) : $restoUrl;
        }
    }

    /**
     * Set output format from suffix or HTTP_ACCEPT
     */
    private function setOutputFormat() {

        $this->outputFormat = $this->getPathSuffix();

        /*
         * Extract outputFormat from HTTP_ACCEPT
         */
        if (!isset($this->outputFormat)) {
            $httpAccept = filter_input(INPUT_SERVER, 'HTTP_ACCEPT', FILTER_SANITIZE_STRING);
            $acceptedFormats = explode(',', strtolower(str_replace(' ', '', $httpAccept)));
            foreach ($acceptedFormats as $format) {
                $weight = 1;
                if (strpos($format, ';q=')) {
                    list($format, $weight) = explode(';q=', $format);
                }
                $AcceptTypes[$format] = $weight;
            }
            foreach (RestoUtil::$contentTypes as $key => $value) {
                if (isset($AcceptTypes[$value]) && $AcceptTypes[$value] !== 0) {
                    $this->outputFormat = $key;
                    break;
                }
            }

            if (!isset($this->outputFormat)) {
                $this->outputFormat = Resto::DEFAULT_GET_OUTPUT_FORMAT;
            }
        }

    }

    /**
     * Return suffix from input url
     * @return string
     */
    private function getPathSuffix() {

        $splitted = explode('.', $this->path);
        $size = count($splitted);
        if ($size > 1) {
            if (array_key_exists($splitted[$size - 1], RestoUtil::$contentTypes)) {
                $suffix = $splitted[$size - 1];
                array_pop($splitted);
                $this->path = join('.', $splitted);
                return $suffix;
            }
            else {
                RestoLogUtil::httpError(404);
            }
        }

        return null;
    }

    /**
     * Set activated modules
     *
     * @param array $modulesConfig
     *
     */
    private function setModules($modulesConfig) {

        $modules = array();

        foreach (array_keys($modulesConfig) as $moduleName) {

            /*
             * Only activated module are registered
             */
            if (isset($modulesConfig[$moduleName]['activate']) && $modulesConfig[$moduleName]['activate'] === true) {

                /*
                 * Check for className
                 */
                $className = isset($modulesConfig[$moduleName]['className']) ? $modulesConfig[$moduleName]['className'] : $moduleName;
                if (class_exists($moduleName)) {

                    $modules[$moduleName] = isset($modulesConfig[$moduleName]['options']) ? array_merge($modulesConfig[$moduleName]['options'], array('className' => $className)) : array('className' => $className);

                    /*
                     * Add route to module
                     */
                    if (isset($modulesConfig[$moduleName]['route'])) {
                        $modules[$moduleName] = array_merge($modules[$moduleName], array('route' => $modulesConfig[$moduleName]['route']));
                    }

                }

            }

        }

        $this->modules = $modules;
        
    }

}
