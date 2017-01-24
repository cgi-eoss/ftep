<?php
/**
 * @file
 */

/**
 * @param array $config
 */
function endpoint_route($config) {
  endpoint_debug_time();
  endpoint_debug_memory();

  require_once DRUPAL_ROOT . '/includes/bootstrap.inc';
  require_once DRUPAL_ROOT . '/includes/common.inc';

  drupal_bootstrap(DRUPAL_BOOTSTRAP_CONFIGURATION);

  $result = NULL;
  $route = NULL;
  try {
    // Find a route.
    if (!isset($config['routes'])) {
      throw new Exception('Routes should be defined');
    }
    if (!$route = endpoint_match_route($config['routes'])) {
      throw new Exception('Route not found');
    }
    // Before execute.
    if (isset($config['before execute callback'])) {
      $config['before execute callback']($route, $config);
    }
    // Execute.
    if (isset($config['execute callback'])) {
      $result = $config['execute callback']($route, $config);
    }
    else {
      $result = endpoint_callback_execute($route, $config);
    }
    // After execute.
    if (isset($config['after execute callback'])) {
      $config['after execute callback']($route, $result, $config);
    }
  }
  catch (Exception $e) {
    if (isset($config['error callback'])) {
      $result = $config['error callback']($e, $route, $config);
    }
    else {
      $result = endpoint_callback_error($e, $route, $config);
    }
  }

  if (isset($config['delivery callback'])) {
    print $config['delivery callback']($result, $route, $config);
  }
  else {
    print endpoint_callback_delivery($result, $route, $config);
  }
}

/**
 * @param array $routes
 *
 * @return array
 * @throws \Exception
 */
function endpoint_match_route($routes) {
  $route = array();

  if (!isset($_SERVER['REQUEST_METHOD'])) {
    throw new Exception('Request method not defined');
  }

  foreach ($routes as $pattern => $methods) {
    if (preg_match_all('#^' . $pattern . '$#', endpoint_request_path(), $matches, PREG_OFFSET_CAPTURE)) {
      if (isset($methods[$_SERVER['REQUEST_METHOD']])) {
        $params = array();
        foreach (array_slice($matches, 1) as $match) {
          $params[$match[0][1]] = $match[0][0];
        }

        // Make a matched route array.
        $route = $methods[$_SERVER['REQUEST_METHOD']];
        $route['pattern'] = $pattern;
        $route['method'] = $_SERVER['REQUEST_METHOD'];
        $route['params'] = $params;
      }
      break;
    }
  }

  return $route;
}

/**
 * @return mixed|\stdClass
 * @throws \Exception
 */
function endpoint_request_data() {
  static $static;

  if (!isset($static)) {
    if (isset($_SERVER['CONTENT_TYPE']) && (strpos($_SERVER['CONTENT_TYPE'], 'application/json') !== FALSE)) {
      $static = json_decode(file_get_contents('php://input'), FALSE);
      if (json_last_error()) {
        throw new Exception('Invalid data format');
      }
    }
    elseif (!empty($_POST['data'])) {
      $static = json_decode(urldecode($_POST['data']), FALSE);
      if (json_last_error()) {
        throw new Exception('Invalid data format');
      }
    }
    else {
      $static = new stdClass();
    }
  }

  return $static;
}

/**
 * @param array $route
 *
 * @return mixed
 * @throws \Exception
 */
function endpoint_callback_execute($route, $config) {
  drupal_bootstrap(DRUPAL_BOOTSTRAP_DATABASE);

  // Prevent Devel from hi-jacking our output in any case.
  $GLOBALS['devel_shutdown'] = FALSE;

  // Deactivate Drupal Error and Exception handling.
  restore_error_handler();
  restore_exception_handler();
  // Activate Endpoint error handler.
  set_error_handler('endpoint_error_handler');

  $bootstrap = isset($route['bootstrap']) ? $route['bootstrap'] : DRUPAL_BOOTSTRAP_DATABASE;
  if ($bootstrap < DRUPAL_BOOTSTRAP_FULL) {
    fix_gpc_magic();
  }

  if ($bootstrap > DRUPAL_BOOTSTRAP_DATABASE) {
    drupal_bootstrap($bootstrap);
  }

  if (isset($config['authorize callback'])) {
    $config['authorize callback']($route, $bootstrap, $config);
  }
  else {
    endpoint_authorize($route, $bootstrap, $config);
  }

  if (isset($config['init variables callback'])) {
    $config['init variables callback']($route, $bootstrap, $config);
  }
  else {
    endpoint_init_variables($route, $bootstrap, $config);
  }

  require_once DRUPAL_ROOT . '/includes/unicode.inc';
  unicode_check();

  require_once DRUPAL_ROOT . '/includes/module.inc';
  require_once DRUPAL_ROOT . '/includes/file.inc';
  file_get_stream_wrappers();

  if (isset($route['include'])) {
    require_once $route['include'];
  }

  $params = !empty($route['params']) ? $route['params'] : array();

  // [F-TEP] Disable additional wrapping of response content
  // return array('result' => call_user_func_array($route['callback'], $params));
  return call_user_func_array($route['callback'], $params);
}

function endpoint_authorize($route, $bootstrap, $config) {
  if (($bootstrap < DRUPAL_BOOTSTRAP_SESSION)) {
    require_once DRUPAL_ROOT . '/' . variable_get('session_inc', 'includes/session.inc');
    drupal_session_initialize();
  }
  if (!$GLOBALS['user']->uid && empty($route['anonymous'])) {
    throw new Exception('User should be authorized', 401);
  }
}

function endpoint_init_variables($route, $bootstrap, $config) {
  global $conf;

  if (($bootstrap < DRUPAL_BOOTSTRAP_VARIABLES)) {
    // Initialize the lock system.
    require_once DRUPAL_ROOT . '/' . variable_get('lock_inc', 'includes/lock.inc');
    lock_initialize();

    // Load variables from the database, but do not overwrite variables set in settings.php.
    $conf = variable_initialize(isset($conf) ? $conf : array());
  }
}

/**
 * @param $result
 * @param $route
 * @param $config
 *
 * @return string
 */
function endpoint_callback_delivery($result, $route, $config) {
  if (!empty($config['debug'])) {
    $result['timer'] = endpoint_debug_time();
    $result['memory'] = endpoint_debug_memory();
  }
  $data = endpoint_request_data();
  if (isset($data->id) && is_int($data->id)) {
    $result['id'] = $data->id;
  }
  header('Content-Type: application/json');
  return json_encode($result, JSON_HEX_TAG | JSON_HEX_APOS | JSON_HEX_AMP | JSON_HEX_QUOT);
}

/**
 * @param \Exception $e
 * @param            $route
 *
 * @return array
 */
function endpoint_callback_error(Exception $e, $route, $config) {
  $message = NULL;
  if (is_a($e, 'InvalidArgumentException')) {
    $message = 'Invalid argument';
  }
  else {
    $message = $e->getMessage();
  }

  return array(
    'error' => array(
      'message' => $message,
      'code' => $e->getCode(),
    ),
  );
}

/**
 * @param $error_level
 * @param $message
 * @param $filename
 * @param $line
 * @param $context
 *
 * @throws \Exception
 */
function endpoint_error_handler($error_level, $message, $filename, $line, $context) {
  if (defined('E_DEPRECATED') && ($error_level == E_DEPRECATED || $error_level == E_USER_DEPRECATED)) {
    return;
  }
  throw new Exception("Error (level $error_level): $message @ $filename:$line");
}

/**
 * @return float|int
 */
function endpoint_debug_time() {
  static $time = NULL;

  if (!$time) {
    $time = microtime(TRUE);
    return 0;
  }
  else {
    // Time diff in seconds.
    return round(microtime(TRUE) - $time, 3);
  }
}

/**
 * @return float|int
 */
function endpoint_debug_memory() {
  static $memory = NULL;

  if (!$memory) {
    $memory = memory_get_usage();
    return 0;
  }
  else {
    // Memory diff in Mb.
    return round((memory_get_usage() - $memory) / 1024 / 1024, 2);
  }
}

/**
 * Fork of request_path().
 */
function endpoint_request_path() {
  static $cache;

  if (!isset($cache)) {
    if (isset($_GET['q']) && is_string($_GET['q'])) {
      $cache = trim($_GET['q'], '/');
    }
    elseif (isset($_SERVER['REQUEST_URI'])) {
      $request_path = strtok($_SERVER['REQUEST_URI'], '?');
      $base_path_len = strlen(rtrim(dirname($_SERVER['SCRIPT_NAME']), '\/'));
      $cache = trim(substr(urldecode($request_path), $base_path_len + 1), '/');
      if ($cache == basename($_SERVER['PHP_SELF'])) {
        $cache = '';
      }
    }
    else {
      $cache = '';
    }
  }

  return $cache;
}
