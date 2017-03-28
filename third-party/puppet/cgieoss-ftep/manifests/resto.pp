class ftep::resto (
  $install_dir   = '/opt/resto',
  $config_file   = 'include/config.php',

  $root_endpoint = '/resto',

  $db_driver     = 'PostgreSQL',
  $db_name       = undef,
  $db_schema     = 'resto',
  $db_host       = undef,
  $db_port       = 5432,
  $db_user       = undef,
  $db_pass       = undef,
) {

  require ::ftep::globals
  require ::epel

  contain ::ftep::common::apache
  contain ::ftep::common::php
  contain ::ftep::db::flyway

  include ::apache::mod::proxy_http
  include ::apache::mod::rewrite
  include ::apache::mod::proxy

  # apache::mod::proxy_fcgi does not include the package on CentOS 6
  ensure_resource('apache::mod', 'proxy_fcgi', { package => 'mod_proxy_fcgi', require => Class['apache::mod::proxy'] })

  ensure_packages(['resto'], {
    ensure => 'latest',
  })

  $real_db_host = pick($db_host, $::ftep::globals::db_hostname)
  $real_db_name = pick($db_name, $::ftep::globals::ftep_db_resto_name)
  $real_db_user = pick($db_user, $::ftep::globals::ftep_db_resto_username)
  $real_db_pass = pick($db_pass, $::ftep::globals::ftep_db_resto_password)

  file { "${install_dir}/${config_file}":
    ensure  => 'present',
    owner   => 'root',
    group   => 'root',
    content => epp('ftep/resto/config.php.epp', {
      'root_endpoint'  => $root_endpoint,
      'db_driver'      => $db_driver,
      'db_name'        => $real_db_name,
      'db_schema_name' => $db_schema,
      'db_host'        => $real_db_host,
      'db_port'        => $db_port,
      'db_username'    => $real_db_user,
      'db_password'    => $real_db_pass,
    }),
    require => Package['resto'],
  }

  $resto_db_migration_requires = defined(Class["::ftep::db"]) ? {
    true    => [Class['::ftep::db'], Package['resto']],
    default => [Package['resto']]
  }

  ftep::db::flyway_migration { 'resto':
    location     => '/opt/resto/_flyway_migration',
    placeholders => {
      'DB'     => $real_db_name,
      'SCHEMA' => $db_schema,
      'USER'   => $real_db_user,
    },
    db_username  => $ftep::globals::ftep_db_resto_su_username,
    db_password  => $ftep::globals::ftep_db_resto_su_password,
    jdbc_url     => "jdbc:postgresql://${::ftep::globals::db_hostname}/${::ftep::globals::ftep_db_resto_name}",
    require      => $resto_db_migration_requires,
  }

  ::apache::vhost { 'ftep-resto':
    port             => '80',
    servername       => 'ftep-resto',
    docroot          => $install_dir,
    override         => ['All'],
    directoryindex   => '/index.php index.php',
    proxy_pass_match => [
      {
        'path' => '^/resto/(.*\.php(/.*)?)$',
        'url'  => "fcgi://127.0.0.1:9000${install_dir}/\$1"
      }
    ],
  }

}
