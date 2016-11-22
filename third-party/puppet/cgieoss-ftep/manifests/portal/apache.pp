class ftep::portal::apache {
  include ::apache::mod::proxy_http

  # apache::mod::proxy_fcgi does not include the package on CentOS 6
  class { ::apache::mod::proxy:
  } -> ::apache::mod { 'proxy_fcgi':
    package => 'mod_proxy_fcgi'
  }

}