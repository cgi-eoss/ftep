# Configure the gateway to the F-TEP services, reverse-proxying to nodes implementing the other classes
class ftep::proxy(
  $tls_cert_path = '/etc/pki/tls/certs/ftep_portal.crt',
  $tls_key_path = '/etc/pki/tls/private/ftep_portal.key',
  $tls_cert,
  $tls_key,
) {

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