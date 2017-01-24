File: <docroot>/.htaccess
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
# Rewrite API callback URLs of the form api.php?q=x.
RewriteCond %{REQUEST_URI} ^\/([a-z]{2}\/)?api\/.*
RewriteRule ^(.*)$ api.php?q=$1 [L,QSA]
RewriteCond %{QUERY_STRING} (^|&)q=(\/)?(\/)?api\/.*
RewriteRule .* api.php [L]
<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

File: <docroot>/api.php
>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
<?php
/**
 * @file
 */

define('DRUPAL_ROOT', getcwd());
require_once DRUPAL_ROOT . '/sites/all/modules/contrib/endpoint/includes/router.inc';

$routes = array(
  'api/v1/foos' => array(
    'GET' => array(
      'callback' => 'my_module_foo_list',
      'anonymous'  => TRUE,
    ),
    'POST' => array(
      'callback' => 'my_module_foo_create',
    ),
  ),
  'api/v1/foo/(\w+)' => array(
    'GET' => array(
      'callback' => 'my_module_foo_get',
    ),
    'POST' => array(
      'include' => DRUPAL_ROOT . '/sites/all/modules/custom/another_module/includes/some_callback.inc',
      'callback' => 'my_module_foo_update',
    ),
  ),
);

endpoint_route(array(
  'debug' => TRUE,
  'routes' => $routes,
));

function my_module_foo_list() {
  // ...
  return array('foos' => $foo_collection);
}

function my_module_foo_create() {
  $data = endpoint_request_data;
  // ...
  return array('foo' => $foo, 'bar' => $bar);
}

function my_module_foo_get($id) {
  // ...
  return array('foo' => $foo, 'bar' => $bar);
}

function my_module_foo_update() {
  $data = endpoint_request_data;
  // ...
}
<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
