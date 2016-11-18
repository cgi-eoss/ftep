class ftep::portal::apache {

  include ::apache

  include ::apache::mod::proxy_http

  # mod_shib with the upstream shibboleth package must use a custom path
  ::apache::mod { 'shib2':
    id      => 'mod_shib',
    path    => '/usr/lib64/shibboleth/mod_shib_22.so',
    require => Package['shibboleth'],
  }

  # apache::mod::proxy_fcgi does not include the package on CentOS 6
  class { ::apache::mod::proxy:
  } -> ::apache::mod { 'proxy_fcgi':
    package => 'mod_proxy_fcgi'
  }

}