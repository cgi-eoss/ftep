class ftep::drupal::apache(
  $site_path
) {

  require ::ftep::globals

  contain ::ftep::common::apache

  include ::apache::mod::proxy_http
  include ::apache::mod::rewrite
  include ::apache::mod::proxy

  # apache::mod::proxy_fcgi does not include the package on CentOS 6
  ensure_resource('apache::mod', 'proxy_fcgi', { package => 'mod_proxy_fcgi', require => Class['apache::mod::proxy'] })

  ::apache::vhost { 'ftep-drupal':
    port             => '80',
    servername       => 'ftep-drupal',
    docroot          => "${site_path}",
    override         => ['All'],
    directoryindex   => '/index.php index.php',
    proxy_pass_match => [
      {
        'path' => '^/(.*\.php(/.*)?)$',
        'url'  => "fcgi://127.0.0.1:9000${site_path}/\$1"
      }
    ],
    rewrites         => [
      { rewrite_rule => [
        '^/api/v1.0/(.*) /api.php?q=api/$1 [L,PT,QSA]',
        '^/secure/api/v1.0/(.*) /api.php?q=api/$1 [L,PT,QSA]'
      ] },
    ]
  }

}