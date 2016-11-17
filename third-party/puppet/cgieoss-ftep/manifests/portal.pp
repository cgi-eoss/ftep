# == Class: ftep::portal
#
# Install and manage the F-TEP portal front-end components, including
# Drupal and the F-TEP web application.
#
class ftep::portal(
  $tls_cert_path = '/etc/pki/tls/certs/ftep_portal.crt',
  $tls_key_path = '/etc/pki/tls/private/ftep_portal.key',
  $tls_cert,
  $tls_key,
) {

  require ::ftep::globals

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

  contain ::ftep::portal::repos
  contain ::ftep::portal::shibboleth
  contain ::ftep::portal::apache
  contain ::ftep::portal::drupal
  contain ::ftep::portal::webapp

}
