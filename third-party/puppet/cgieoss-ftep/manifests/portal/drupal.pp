class ftep::portal::drupal(
  $drupal_site = 'forestry-tep.eo.esa.int',
  $drupal_version = '7.43',
  $www_path  = '/var/www/html/drupal',
  $www_user = 'apache',

  $db_host           = $ftep::globals::ftep_db_host,
  $db_name           = $ftep::globals::ftep_db_name,
  $db_user           = $ftep::globals::ftep_db_username,
  $db_pass           = $ftep::globals::ftep_db_password,
  $db_port           = '5432',
  $db_driver         = 'pgsql',
  $db_prefix         = 'drupal_',
) {

  class { '::php':
    ensure         => latest,
    manage_repos   => false,
    fpm            => true,
    package_prefix => 'php56w-',
    composer       => true,
    pear           => true,
    extensions     => {
      xml      => { },
      gd       => { },
      pdo      => { },
      mbstring => { },
      pgsql    => { },
    },
    settings       => {
      'PHP/max_execution_time'  => '90',
      'PHP/max_input_time'      => '300',
      'PHP/memory_limit'        => '64M',
      'PHP/post_max_size'       => '32M',
      'PHP/upload_max_filesize' => '32M',
      'Date/date.timezone'      => 'UTC',
    },
    require        => Yumrepo['webtatic']
  }

  file { $www_path:
    ensure => directory,
    owner  => 'root',
    group  => 'root',
  }

  class { drupal:
    www_dir     => $www_path,
    www_process => $www_user,
  }

  ::drupal::site { $drupal_site:
    core_version     => $drupal_version,
    modules          => {
      'ctools'            => '1.9',
      'endpoint'          => '1.4',
      'entity'            => '1.7',
      'entityreference'   => '1.1',
      'registry_autoload' => '1.3',
      'shib_auth'         => '4.3',
      'views'             => '3.13',
    },
    settings_content => epp('ftep/portal/drupal/settings.php.epp', {
      'db' => {
        'database' => $db_name,
        'username' => $db_user,
        'password' => $db_pass,
        'host'     => $db_host,
        'port'     => $db_port,
        'driver'   => $db_driver,
        'prefix'   => $db_prefix,
      }
    }),
  }

  ::apache::vhost { 'ftep-drupal':
    port             => '80',
    servername       => $::fqdn,
    docroot          => "${www_path}/${drupal_site}",
    directoryindex   => '/index.php index.php',
    proxy_pass_match => [
      {
        'path' => '^/(.*\.php(/.*)?)$',
        'url'  => "fcgi://127.0.0.1:9000${www_path}/${drupal_site}/\$1"
      }
    ],
    rewrites => [
      {
        rewrite_rule => ['^/api/(.*) /api.php?q=api/$1 [P,L]']
      }
    ],
    aliases => [
      {
        alias => '/app',
        path => "${www_path}/../f-tep/app"
      },{
        alias => '/bower_components',
        path => "${www_path}/../f-tep/bower_components"
      }
    ]
  }

  file { "${www_path}/${drupal_site}/api.php":
    ensure  => link,
    target  => "${www_path}/${drupal_site}/sites/default/modules/ftep-backend/ftep_search/api.php"
    require => Apache::Vhost['ftep-drupal'],
  }

  package { f-tep-drupalmodules:
    ensure => latest
  }

}

