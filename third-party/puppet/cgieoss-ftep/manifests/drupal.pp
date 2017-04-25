class ftep::drupal (
  $drupal_site      = 'forestry-tep.eo.esa.int',
  $drupal_version   = '7.54',
  $www_path         = '/var/www/html/drupal',
  $www_user         = 'apache',

  $db_host          = undef,
  $db_name          = undef,
  $db_user          = undef,
  $db_pass          = undef,
  $db_port          = '5432',
  $db_driver        = 'pgsql',
  $db_prefix        = 'drupal_',

  $ftep_module_name = 'ftep-backend',

  $init_db          = true,
  $enable_cron      = true,
) {

  require ::ftep::globals
  require ::epel

  contain ::ftep::common::php

  class { '::postgresql::client': }

  $real_db_host = pick($db_host, $::ftep::globals::db_hostname)
  $real_db_name = pick($db_name, $::ftep::globals::ftep_db_name)
  $real_db_user = pick($db_user, $::ftep::globals::ftep_db_username)
  $real_db_pass = pick($db_pass, $::ftep::globals::ftep_db_password)

  file { $www_path:
    ensure => directory,
    owner  => 'root',
    group  => 'root',
  }

  package { 'f-tep-drupalmodules':
    ensure => latest,
    notify => Drupal::Site[$drupal_site],
  }

  class { ::drupal:
    www_dir     => $www_path,
    www_process => $www_user,
    require     => Class['::php'],
  }

  ::drupal::site { $drupal_site:
    core_version     => $drupal_version,
    modules          => {
      'backup_migrate'    => '3.1',
      'ctools'            => '1.9',
      'entity'            => '1.7',
      'entityreference'   => '1.1',
      'registry_autoload' => '1.3',
      'shib_auth'         => '4.3',
      'views'             => '3.13',
      'endpoint'          => { 'download' => {
        'type' => 'copy',
        'url'  => 'file:///opt/f-tep-drupalmodules/endpoint/',
      } },
      $ftep_module_name   => { 'download' => {
        'type' => 'copy',
        'url'  => 'file:///opt/f-tep-drupalmodules/ftep-backend/',
      } }
    },
    settings_content => epp('ftep/drupal/settings.php.epp', {
      'db'         => {
        'host'     => $real_db_host,
        'database' => $real_db_name,
        'username' => $real_db_user,
        'password' => $real_db_pass,
        'port'     => $db_port,
        'driver'   => $db_driver,
        'prefix'   => $db_prefix,
      },
      'ftep_proxy' => $::ftep::globals::proxy_hostname,
    }),
    cron_file_ensure => $enable_cron ? {
      true    => 'present',
      default => 'absent'
    },
    require          => [Package['f-tep-drupalmodules'], Class['::drupal']],
  }

  $site_path = "${www_path}/${drupal_site}"

  class { ::ftep::drupal::apache:
    site_path => $site_path
  }

  file { "${site_path}/api.php":
    ensure  => link,
    target  => "${site_path}/sites/all/modules/${ftep_module_name}/ftep_search/api.php",
    require => [Drupal::Site[$drupal_site]],
  }

  # Install the site if the database is not yet initialised
  if $init_db {
    $drush_site_install = "${::drupal::drush_path} -y site-install"
    $drush_si_options = "standard install_configure_form.update_status_module='array(FALSE,FALSE)'"

    $drupal_site_install_requires = defined(Class["::ftep::db"]) ? {
      true    => [Class['::ftep::db'], Drupal::Site[$drupal_site]],
      default => [Drupal::Site[$drupal_site]]
    }

    exec { 'drupal-site-install':
      command => "${drush_site_install} ${drush_si_options} --root=${site_path} 2>&1",
      unless  => [
        "/usr/bin/test -n \"`${::drupal::drush_path} status bootstrap --field-labels=0 --root=${site_path} 2>&1`\""
      ],
      require => $drupal_site_install_requires,
    }
  }

}
