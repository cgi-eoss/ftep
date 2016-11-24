<?php
// vim: :set ts=4 et sw=4 


use Tobscure\JsonApi\ErrorHandler;
use Tobscure\JsonApi\Document;
use Tobscure\JsonApi\Exception\Handler\FallbackExceptionHandler;
use Tobscure\JsonApi\Exception\Handler\ResponseBag;
require dirname(__FILE__).'/vendor/autoload.php';
/**
 * @file
 */

function log_error( $num, $str, $file, $line, $context = null ) {
     log_exception( new ErrorException( $str, 0, $num, $file, $line ) );
}

/**
 * Uncaught exception handler.
 */
function log_exception( Exception $e ) {
    $message = "Type: " . get_class( $e ) . "; Message: {$e->getMessage()}; File: {$e->getFile()}; Line: {$e->getLine()};";

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
function check_for_fatal() {

    $error = error_get_last();
    if ( $error["type"] == E_ERROR )
        log_error( $error["type"], $error["message"], $error["file"], $error["line"] );
}

/*
register_shutdown_function( "check_for_fatal" );
set_error_handler( "log_error" );
set_exception_handler( "log_exception" );
ini_set( "display_errors", "off" );
error_reporting( E_ALL );
*/

define('DRUPAL_ROOT', getcwd());
require_once DRUPAL_ROOT . '/sites/default/modules/endpoint/includes/router.inc';


require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_search.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_downloadmanager.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_identitymanager.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_job.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_datasource.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_databasket.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_service.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_group.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_project.php';
require_once DRUPAL_ROOT . '/sites/default/modules/ftep-backend/ftep_search/ftep_resource_user.php';


$search=new FtepResourceSearch();
$downloadManager=new FtepResourceDownloadManager();
$job=new FtepResourceJob();
$im=new FtepResourceIdentityManager();
$ds=new FtepResourceDataSource();
$db=new FtepResourceDataBasket();
$s=new FtepResourceService();
$g=new FtepResourceGroup();
$p=new FtepResourceProject();
$u=new FtepResourceUser();

$routes = array(
		'api/v1.0/login' => array(
					'POST' => array(
							'anonymous'  => true,
                            'callback' => array($im,'login'),
					),
		),
		'api/v1.0/search' => array(
					'GET' => array(
							'anonymous'  => false,
                            'callback' => array($search,'search'),
					),
		),
		'api/v1.0/download2/(\d+)/([\w-_\.]+)' => array(
					'GET' => array(
							'anonymous'  => false,
                            'callback' => array($downloadManager,'download2'),
					),
		),
		'api/v1.0/download' => array(
					'GET' => array(
							'anonymous'  => false,
                            'callback' => array($downloadManager,'download'),
					),
		),
        'api/v1.0/jobs/(\d+)/getGUI' => array(
            // SHould handle better the path
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($job,'getJobGui'),
				),
        ),
        'api/v1.0/jobs/(\d+)/getOutputs' => array(
            // SHould handle better the path
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($job,'getJobOutput'),
				),
        ),
		'api/v1.0/jobs/*(\d+)*(?:/*)(\w+)*(?:/)*(\w+)*' => array(
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($job,'read'),
				),
				'POST' => array(
						'anonymous'  => false,
                        'callback' => array($job,'write'),
				),
				'PATCH' => array(
						'anonymous'  => false,
						'callback' => array($job,'update'),
				),
				'DELETE' => array(
						'anonymous'  => false,
						'callback' => array($job,'delete'),
				),
		),
		'api/v1.0/users/*(\w+)*' => array(
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($u,'read'),
				),
				'POST' => array(
						'anonymous'  => false,
                        'callback' => array($u,'write'),
				),
				'PATCH' => array(
						'anonymous'  => false,
						'callback' => array($u,'update'),
				),
		),
		'api/v1.0/services/*(\w+)*' => array(
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($s,'read'),
				),
				'POST' => array(
						'anonymous'  => false,
                        'callback' => array($s,'write'),
				),
				'PATCH' => array(
						'anonymous'  => false,
						'callback' => array($s,'update'),
				),
		),
///		'apix/v1.0/databaskets/*(\w+)*' => array(
//				'GET' => array(
//						'anonymous'  => false,
//                        'callback' => array($db,'read'),
//				),
//        ),
		'api/v1.0/databaskets/*(\d+)*(?:/*)(\w+)*(?:/)*(\w+)*' => array(
		//'api/v1.0/databaskets/*(\w+)*' => array(
				'OPTIONS' => array(
						'anonymous'  => true,
                        'callback' => array($db,'options'),
				),
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($db,'read'),
				),
				'POST' => array(
						'anonymous'  => false,
                        'callback' => array($db,'write'),
				),
				'PATCH' => array(
						'anonymous'  => false,
						'callback' => array($db,'update'),
				),
				'DELETE' => array(
						'anonymous'  => false,
						'callback' => array($db,'delete'),
				),
		),
		'api/v1.0/datasources/getAuthentication/(.*)' => array(
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($ds,'getAuthentication'),
                ),
        ),
		'api/v1.0/datasources/*(\w+)*' => array(
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($ds,'read'),
				),
				'POST' => array(
						'anonymous'  => false,
                        'callback' => array($ds,'write'),
				),
				'PATCH' => array(
						'anonymous'  => false,
						'callback' => array($ds,'update'),
				),
		),
		'api/v1.0/groups/*(\d+)*(?:/*)(\w+)*(?:/)*(\w+)*' => array(
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($g,'read'),
				),
				'POST' => array(
						'anonymous'  => false,
                        'callback' => array($g,'write'),
				),
				'PATCH' => array(
						'anonymous'  => false,
						'callback' => array($g,'update'),
				),
				'DELETE' => array(
						'anonymous'  => false,
						'callback' => array($g,'delete'),
				),
		),
		'api/v1.0/projects/*(\w+)*' => array(
				'GET' => array(
						'anonymous'  => false,
                        'callback' => array($p,'read'),
				),
				'POST' => array(
						'anonymous'  => false,
                        'callback' => array($p,'write'),
				),
				'PATCH' => array(
						'anonymous'  => false,
						'callback' => array($p,'update'),
				),
				'DELETE' => array(
						'anonymous'  => false,
						'callback' => array($p,'delete'),
				),
		),
);

    endpoint_route(array(
        'debug' => false,
        'routes' => $routes,
        'authorize callback' => array($im, 'authorize'),
		 'error callback' => array($im, 'handleError'),
     )); 

