class ftep::portal::apache {

  include ::apache

  include ::apache::mod::proxy
  include ::apache::mod::proxy_http
  include ::apache::mod::proxy_fcgi

  # Module resource must be defined manually until puppetlabs-apache > 1.10.0
  ::apache::mod { 'shib2':
    id   => 'mod_shib',
    path => '/usr/lib64/shibboleth/mod_shib_22.so',
  }

}