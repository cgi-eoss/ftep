class ftep::proxy::apache {

  # mod_shib with the upstream shibboleth package must use a custom path
  ::apache::mod { 'shib2':
    id      => 'mod_shib',
    path    => '/usr/lib64/shibboleth/mod_shib_22.so',
    require => Package['shibboleth'],
  }

}