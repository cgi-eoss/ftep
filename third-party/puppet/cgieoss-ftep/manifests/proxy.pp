# Configure the gateway to the F-TEP services, reverse-proxying to nodes implementing the other classes
class ftep::proxy(
  $enable_ssl = false,
  $enable_sso = false,

  $context_path_geoserver = '/geoserver',
  $context_path_webapp = '/app',
  $context_path_wps = '/wps',

  $tls_cert_path = '/etc/pki/tls/certs/ftep_portal.crt',
  $tls_key_path = '/etc/pki/tls/private/ftep_portal.key',
  $tls_cert = undef,
  $tls_key = undef,
) {

  require ::ftep::globals

  contain ::ftep::common::apache

  include ::apache::mod::proxy

  apache::vhost { 'ftep-proxy':
    port             => '80',
    docroot          => '/var/www/html',
    default_vhost    => true,
    vhost_name       => '_default_',          # The default landing site should always be Drupal
    proxy_dest       => 'http://ftep-drupal', # Drupal is always mounted at the base_url
    proxy_pass       => [
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
    ],
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
  }

  if $enable_sso {
    contain ::ftep::proxy::shibboleth
  }

}