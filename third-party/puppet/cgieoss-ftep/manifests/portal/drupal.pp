class ftep::portal::drupal(
  $drupal_site = 'forestry-tep.eo.esa.int',
  $drupal_version = '7.43',
  $www_path  = '/var/www/html/drupal',
  $www_user = 'apache',
) {

  class { '::php':
    ensure         => latest,
    manage_repos   => false,
    fpm            => false,
    package_prefix => 'php56w-',
    composer       => true,
    pear           => true,
    extensions     => {
      xml      => { },
      gd       => { },
      pdo      => { },
      mbstring => { },
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
    core_version => $drupal_version,
    modules      => {
      'ctools'            => '1.9',
      'endpoint'          => '1.4',
      'entity'            => '1.7',
      'entityreference'   => '1.1',
      'registry_autoload' => '1.3',
      'shib_auth'         => '4.3',
      'views'             => '3.13',
    }
  }

  ::apache::vhost { 'F-TEP Drupal':
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
  }

}