# Class for setting cross-class global overrides.
class ftep::globals (
  $manage_package_repo     = true,

  # Base URL (i.e. servername) for ftep::proxy (and ftep::drupal's base_url config)
  $base_url                = $facts['fqdn'],

  # Hostnames and IPs for components
  $db_hostname             = 'ftep-db',
  $drupal_hostname         = 'ftep-drupal',
  $geoserver_hostname      = 'ftep-geoserver',
  $proxy_hostname          = 'ftep-proxy',
  $webapp_hostname         = 'ftep-webapp',
  $wps_hostname            = 'ftep-wps',
  $server_hostname         = 'ftep-server',

  $hosts_override          = { },

  # All classes should share this database config, or override it if necessary
  $ftep_db_name            = 'ftep',
  $ftep_db_v2_name         = 'ftep_v2',
  $ftep_db_username        = 'ftep-user',
  $ftep_db_password        = 'ftep-pass',

  # App server port config
  $server_application_port = 8090,
  $worker_application_port = 8091,
) {

  # Alias reverse-proxy hosts via hosts file
  ensure_resources(host, $hosts_override)

  # Setup of the repo only makes sense globally, so we are doing this here.
  if($manage_package_repo) {
    require ::ftep::repo
  }
}