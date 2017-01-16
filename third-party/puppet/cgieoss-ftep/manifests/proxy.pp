# Configure the gateway to the F-TEP services, reverse-proxying to nodes implementing the other classes
class ftep::proxy (
  $enable_ssl             = false,
  $enable_sso             = false,

  $context_path_geoserver = '/geoserver',
  $context_path_webapp    = '/app',
  $context_path_wps       = '/wps',

  $tls_cert_path          = '/etc/pki/tls/certs/ftep_portal.crt',
  $tls_key_path           = '/etc/pki/tls/private/ftep_portal.key',
  $tls_cert               = undef,
  $tls_key                = undef,
) {

  require ::ftep::globals

  contain ::ftep::common::apache

  include ::apache::mod::proxy

  $default_proxy_config = {
    docroot    => '/var/www/html',
    vhost_name => '_default_', # The default landing site should always be Drupal
    proxy_dest => 'http://ftep-drupal', # Drupal is always mounted at the base_url
  }

  # Directory/Location directives
  $default_directories = [ ]

  # Reverse proxied paths
  $default_proxy_pass = [
    {
      'path' => $context_path_geoserver,
      'url'  => 'http://ftep-geoserver'
    },
    {
      'path' => $context_path_webapp,
      'url'  => 'http://ftep-webapp'
    },
    {
      'path' => $context_path_wps,
      'url'  => 'http://ftep-wps'
    },
  ]


  if $enable_sso {
    unless ($tls_cert and $tls_key) {
      fail("ftep::proxy requres \$tls_cert and \$tls_key to be set if \$enable_sso is true")
    }
    contain ::ftep::proxy::shibboleth

    # Add the /Shibboleth.sso SP callback location and secured paths
    $directories = concat($default_directories, [
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
    ])

    # Insert the callback location at the start of the reverse proxy list
    $proxy_pass = concat([{
      'path'         => '/Shibboleth.sso',
      'url'          => '!',
      'reverse_urls' => []
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