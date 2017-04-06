<?php
// vim: :set ts=4 et sw=4

use Tobscure\JsonApi\Document;
use Tobscure\JsonApi\ErrorHandler;
use Tobscure\JsonApi\Exception\Handler\FallbackExceptionHandler;

require __DIR__ . '/vendor/autoload.php';

function log_error($num, $str, $file, $line, $context = null)
{
    log_exception(new ErrorException($str, 0, $num, $file, $line));
}

/**
 * Uncaught exception handler.
 */
function log_exception(Exception $e)
{
    $message = 'Type: ' . get_class($e) . '; Message: {$e->getMessage()}; File: {$e->getFile()}; Line: {$e->getLine()};';

    $errors = new ErrorHandler;
    $errors->registerHandler(new InvalidParameterExceptionHandler);
    $errors->registerHandler(new FallbackExceptionHandler);
    $response = $errors->handle($e);
    $document = new Document;
    $document->setErrors($response->getErrors());
    return new JsonResponse($document, $response->getStatus());

}

/**
 * Checks for a fatal error, work around for set_error_handler not working on fatal errors.
 */
function check_for_fatal()
{

    $error = error_get_last();
    if ($error["type"] == E_ERROR)
        log_error($error["type"], $error["message"], $error["file"], $error["line"]);
}

/*
register_shutdown_function( "check_for_fatal" );
set_error_handler( "log_error" );
set_exception_handler( "log_exception" );
ini_set( "display_errors", "off" );
error_reporting( E_ALL );
*/


$builder = new DI\ContainerBuilder();
$builder->addDefinitions(__DIR__.'/config.php');
$builder->useAnnotations(true);
$container = $builder->build();

define('DRUPAL_ROOT', $container->get('drupal.root'));
define('MODULE_ROOT', $container->get('module.root'));


$search = $container->get('FtepResourceSearch');
$job = $container->get('FtepResourceJob');
$im = $container->get('FtepResourceIdentityManager');
$ds = $container->get('FtepResourceDataSource');
$db = $container->get('FtepResourceDataBasket');
$s = $container->get('FtepResourceService');
$g = $container->get('FtepResourceGroup');
$p = $container->get('FtepResourceProject');
$u = $container->get('FtepResourceUser');
$downloadManager=$container->get('FtepResourceDownloadManager');

$version = $container->get("api.version");

$routes = array(
    'api/v'.$version.'/login' => array(
        'POST' => array(
            'anonymous' => true,
            'callback' => array($im, 'login'),
        ),
    ),
    'api/v'.$version.'/search' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($search, 'search'),
        ),
    ),
    'api/v'.$version.'/ql/(\w+)/(.*)'=> array(
        'GET' => [
            'anonymous' => false,
            'callback' => array($search, 'getQuickLook'),
        ],
    ),




    'api/v'.$version.'/download2/(\d+)/([\w-_\.]+)' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($downloadManager, 'download2'),
        ),
    ),
    'api/v'.$version.'/download' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($downloadManager, 'download'),
        ),
    ),
    'api/v'.$version.'/jobs/(\d+)/getGUI' => array(
        // SHould handle better the path
        'GET' => array(
            'anonymous' => false,
            'callback' => array($job, 'getJobGui'),
        ),
    ),
    'api/v'.$version.'/jobs/(\d+)/getOutputs' => array(
        // SHould handle better the path
        'GET' => array(
            'anonymous' => false,
            'callback' => array($job, 'getJobOutput'),
        ),
    ),
    'api/v'.$version.'/jobs/*(\d+)*(?:/*)(\w+)*(?:/)*(\w+)*' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($job, 'read'),
        ),
        'POST' => array(
            'anonymous' => false,
            'callback' => array($job, 'write'),
        ),
        'PATCH' => array(
            'anonymous' => false,
            'callback' => array($job, 'update'),
        ),
        'DELETE' => array(
            'anonymous' => false,
            'callback' => array($job, 'delete'),
        ),
    ),
    'api/v'.$version.'/users/*(\w+)*' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($u, 'read'),
        ),
        'POST' => array(
            'anonymous' => false,
            'callback' => array($u, 'write'),
        ),
        'PATCH' => array(
            'anonymous' => false,
            'callback' => array($u, 'update'),
        ),
    ),


    'api/v'.$version.'/services/*(\w+)*' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($s, 'read'),
        ),
        'POST' => array(
            'anonymous' => false,
            'callback' => array($s, 'write'),
        ),
        'PATCH' => array(
            'anonymous' => false,
            'callback' => array($s, 'update'),
        ),
    ),



    'api/v'.$version.'/databaskets/*(\d+)*(?:/*)(\w+)*(?:/)*(\w+)*' => array(
        'OPTIONS' => array(
            'anonymous' => true,
            'callback' => array($db, 'options'),
        ),
        'GET' => array(
            'anonymous' => false,
            'callback' => array($db, 'read'),
        ),
        'POST' => array(
            'anonymous' => false,
            'callback' => array($db, 'write'),
        ),
        'PATCH' => array(
            'anonymous' => false,
            'callback' => array($db, 'update'),
        ),
        'DELETE' => array(
            'anonymous' => false,
            'callback' => array($db, 'delete'),
        ),
    ),
    'api/v'.$version.'/datasources/getAuthentication/(.*)' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($ds, 'getAuthentication'),
        ),
    ),
    'api/v'.$version.'/datasources/*(\w+)*' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($ds, 'read'),
        ),
        'POST' => array(
            'anonymous' => false,
            'callback' => array($ds, 'write'),
        ),
        'PATCH' => array(
            'anonymous' => false,
            'callback' => array($ds, 'update'),
        ),
    ),
    'api/v'.$version.'/groups/*(\d+)*(?:/*)(\w+)*(?:/)*(\w+)*' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($g, 'read'),
        ),
        'POST' => array(
            'anonymous' => false,
            'callback' => array($g, 'write'),
        ),
        'PATCH' => array(
            'anonymous' => false,
            'callback' => array($g, 'update'),
        ),
        'DELETE' => array(
            'anonymous' => false,
            'callback' => array($g, 'delete'),
        ),
    ),
    'api/v'.$version.'/projects/*(\w+)*' => array(
        'GET' => array(
            'anonymous' => false,
            'callback' => array($p, 'read'),
        ),
        'POST' => array(
            'anonymous' => false,
            'callback' => array($p, 'write'),
        ),
        'PATCH' => array(
            'anonymous' => false,
            'callback' => array($p, 'update'),
        ),
        'DELETE' => array(
            'anonymous' => false,
            'callback' => array($p, 'delete'),
        ),
    ),

);


//if ((realpath(__FILE__) === realpath($_SERVER['SCRIPT_FILENAME'])) && (array_key_exists('FCGI_ROLE',$_SERVER) ? $_SERVER['FCGI_ROLE']: null != 'RESPONDER') ) {
//    $data = array();
//    array_map(
//        function ($item) use (&$data) {
//            $t = explode('=', $item);
//            //echo var_export($t, true);
//            $data[$t[0]] = $t[1];
//        },
//        array_slice($argv, 1)
//    );
//    parse_str( http_build_query($data), $_GET );
//
//    $resp = $search->search();
//
//    print_r(json_encode($resp));
//
//
//} else {
    require_once MODULE_ROOT.'/endpoint/includes/router.inc';

    endpoint_route(array(
        'debug' => $container->get('debug'),
        'routes' => $routes,
        'authorize callback' => array($im, 'authorize'),
        'error callback' => array($im, 'handleError'),
    ));
//}



