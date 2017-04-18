# Configure the gateway to the F-TEP services, reverse-proxying to nodes implementing the other classes
class ftep::proxy (
  $enable_ssl             = false,
  $enable_sso             = false,

  $context_path_geoserver = '/geoserver',
  $context_path_resto     = '/resto',
  $context_path_webapp    = '/app',
  $context_path_wps       = '/secure/wps',
  $context_path_api_v2    = '/secure/api/v2.0',
  $context_path_monitor   = '/monitor',
  $context_path_log       = '/logs',

  $tls_cert_path          = '/etc/pki/tls/certs/ftep_portal.crt',
  $tls_key_path           = '/etc/pki/tls/private/ftep_portal.key',
  $tls_cert               = undef,
  $tls_key                = undef,
) {

  require ::ftep::globals

  contain ::ftep::common::apache

  include ::apache::mod::headers
  include ::apache::mod::proxy

  $default_proxy_config = {
    docroot    => '/var/www/html',
    vhost_name => '_default_', # The default landing site should always be Drupal
    proxy_dest => 'http://ftep-drupal', # Drupal is always mounted at the base_url
  }

  # Directory/Location directives - cannot be an empty array...
  $default_directories = [
    {
      'provider' => 'location',
      'path' => $context_path_log,
      'custom_fragment' => "RequestHeader set X-Graylog-Server-URL \"${ftep::globals::base_url}${ftep::globals::graylog_context_path}/api\""
    }
  ]

  # Reverse proxied paths
  $default_proxy_pass = [
    {
      'path'   => $context_path_geoserver,
      'url'    => "http://${ftep::globals::geoserver_hostname}:${ftep::globals::geoserver_port}${context_path_geoserver}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $context_path_resto,
      'url'  => "http://${ftep::globals::resto_hostname}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $context_path_webapp,
      'url'  => "http://${ftep::globals::webapp_hostname}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $context_path_wps,
      'url'  => "http://${ftep::globals::wps_hostname}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $context_path_api_v2,
      'url'  => "http://${ftep::globals::server_hostname}:${ftep::globals::server_application_port}${context_path_api_v2}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => '/download',
      'url'  => "http://${ftep::globals::wps_hostname}/wps/ftep-output",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $context_path_monitor,
      'url'  => "http://${ftep::globals::monitor_hostname}:${ftep::globals::grafana_port}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $context_path_log,
      'url'  => "http://${ftep::globals::monitor_hostname}:${ftep::globals::graylog_port}${ftep::globals::graylog_context_path}",
      'params' => { 'retry' => '0' }
    }
  ]


  if $enable_sso {
    unless ($tls_cert and $tls_key) {
      fail("ftep::proxy requres \$tls_cert and \$tls_key to be set if \$enable_sso is true")
    }
    contain ::ftep::proxy::shibboleth

    # Add the /Shibboleth.sso SP callback location and secured paths
    $directories = concat([
      {
        'provider'   => 'location',
        'path'       => '/Shibboleth.sso',
        'sethandler' => 'shib'
      },
      {
        'provider'              => 'location',
        'path'                  => $context_path_webapp,
        'auth_type'             => 'shibboleth',
        'shib_use_headers'      => 'On',
        'shib_request_settings' => { 'requireSession' => '1' },
        'custom_fragment'       => 'ShibCompatWith24 On',
        'auth_require'          => 'shib-session',
      },
      {
        'provider'              => 'location',
        'path'                  => '/secure',
        'auth_type'             => 'shibboleth',
        'shib_use_headers'      => 'On',
        'shib_request_settings' => { 'requireSession' => '1' },
        'custom_fragment'       => 'ShibCompatWith24 On',
        'auth_require'          => 'shib-session',
      }
    ], $default_directories)

    # Insert the callback location at the start of the reverse proxy list
    $proxy_pass = concat([{
      'path'         => '/Shibboleth.sso',
      'url'          => '!',
      'reverse_urls' => [],
      'params'       => { 'retry' => '0' }
    }], $default_proxy_pass)
  } else {
    $directories = $default_directories
    $proxy_pass = $default_proxy_pass
  }

  if $enable_ssl {
    unless ($tls_cert and $tls_key) {
      fail("ftep::proxy requres \$tls_cert and \$tls_key to be set if \$enable_ssl is true")
    }

    file { $tls_cert_path:
      ensure  => present,
      mode    => '0644',
      owner   => 'root',
      group   => 'root',
      content => $tls_cert,
    }

    file { $tls_key_path:
      ensure  => present,
      mode    => '0600',
      owner   => 'root',
      group   => 'root',
      content => $tls_key,
    }

    apache::vhost { 'ftep-proxy':
      port            => '443',
      ssl             => true,
      ssl_cert        => $tls_cert_path,
      ssl_key         => $tls_key_path,
      default_vhost   => true,
      request_headers => [
        'set X-Forwarded-Proto "https"'
      ],
      directories     => $directories,
      proxy_pass      => $proxy_pass,
      *               => $default_proxy_config
    }
  } else {
    apache::vhost { 'ftep-proxy':
      port          => '80',
      default_vhost => true,
      directories   => $directories,
      proxy_pass    => $proxy_pass,
      *             => $default_proxy_config
    }
  }

}
