# Configure the gateway to the F-TEP services, reverse-proxying to nodes implementing the other classes
class ftep::proxy (
  $enable_ssl             = false,
  $enable_sso             = false,

  $context_path_geoserver = undef,
  $context_path_resto     = undef,
  $context_path_webapp    = undef,
  $context_path_wps       = undef,
  $context_path_api_v2    = undef,
  $context_path_monitor   = undef,
  $context_path_logs      = undef,
  $context_path_eureka    = undef,

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

  $real_context_path_geoserver = pick($context_path_geoserver, $ftep::globals::context_path_geoserver)
  $real_context_path_resto = pick($context_path_resto, $ftep::globals::context_path_resto)
  $real_context_path_webapp = pick($context_path_webapp, $ftep::globals::context_path_webapp)
  $real_context_path_wps = pick($context_path_wps, $ftep::globals::context_path_wps)
  $real_context_path_api_v2 = pick($context_path_api_v2, $ftep::globals::context_path_api_v2)
  $real_context_path_monitor = pick($context_path_monitor, $ftep::globals::context_path_monitor)
  $real_context_path_logs = pick($context_path_logs, $ftep::globals::context_path_logs)
  $real_context_path_eureka = pick($context_path_eureka, $ftep::globals::context_path_eureka)

  # Directory/Location directives - cannot be an empty array...
  $default_directories = [
    {
      'provider' => 'location',
      'path' => $real_context_path_logs,
      'custom_fragment' => "RequestHeader set X-Graylog-Server-URL \"${ftep::globals::base_url}${ftep::globals::graylog_api_path}\""
    }
  ]

  # Reverse proxied paths
  $default_proxy_pass = [
    {
      'path'   => $real_context_path_geoserver,
      'url'    => "http://${ftep::globals::geoserver_hostname}:${ftep::globals::geoserver_port}${real_context_path_geoserver}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $real_context_path_resto,
      'url'  => "http://${ftep::globals::resto_hostname}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $real_context_path_webapp,
      'url'  => "http://${ftep::globals::webapp_hostname}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $real_context_path_wps,
      'url'  => "http://${ftep::globals::wps_hostname}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $real_context_path_api_v2,
      'url'  => "http://${ftep::globals::server_hostname}:${ftep::globals::server_application_port}${real_context_path_api_v2}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $real_context_path_monitor,
      'url'  => "http://${ftep::globals::monitor_hostname}:${ftep::globals::grafana_port}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $real_context_path_logs,
      'url'  => "http://${ftep::globals::monitor_hostname}:${ftep::globals::graylog_port}${ftep::globals::graylog_context_path}",
      'params' => { 'retry' => '0' }
    },
    {
      'path' => $real_context_path_eureka,
      'url'  => "http://${ftep::globals::server_hostname}:${ftep::globals::serviceregistry_application_port}/eureka",
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
        'path'                  => $real_context_path_webapp,
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
