# == Class: ftep::wps
#
# Install and manage ZOO-Project's zoo-kernel WPS server, and the
# f-tep-processors WPS service package.
#
class ftep::wps (
  $manage_zoo_kernel = true,
  $manage_package    = true,
  $package_ensure    = 'present',
  $package_name      = 'zoo-kernel',
  $cgi_path          = '/var/www/cgi-bin',
  $main_config_file  = 'main.cfg',
  $script_alias      = '/zoo_loader.cgi',
  $cgi_file          = 'zoo_loader.cgi',

  # zoo-kernel config
  $wps_version       = '1.0.0',
  $lang              = 'en-US',
  $server_address    = 'https://forestry-tep.eo.esa.int/wps',
  $data_path         = '/var/www/temp',
  $tmp_path          = '/data/cache',
  $tmp_url           = '../ftep-output',
  $cache_dir         = '/tmp',

  $provider_name     = 'CGI IT UK Ltd.',
  $provider_site     = 'https://forestry-tep.eo.esa.int/',

  $db_host           = undef,
  $db_name           = undef,
  $db_user           = undef,
  $db_pass           = undef,
  $db_port           = '5432',
  $db_type           = 'PG',
  $db_schema         = 'public',

  $env_config        = { },
  $ftep_config       = { },
  $other_config      = { },
) {

  require ::ftep::globals

  contain ::ftep::common::java
  contain ::ftep::common::apache
  require ::apache::mod::cgi

  # Extra repos
  require ::epel
  require ::ftep::repo::elgis

  $real_db_host = pick($db_host, $::ftep::globals::db_hostname)
  $real_db_name = pick($db_name, $::ftep::globals::ftep_db_name)
  $real_db_user = pick($db_user, $::ftep::globals::ftep_db_username)
  $real_db_pass = pick($db_pass, $::ftep::globals::ftep_db_password)

  ensure_packages(['f-tep-processors'], {
    ensure => 'latest',
    name   => 'f-tep-processors',
  })

  if ($manage_zoo_kernel) {
    if ($manage_package) {
      $_package_ensure = $package_ensure ? {
        true     => 'present',
        false    => 'purged',
        'absent' => 'purged',
        default  => $package_ensure,
      }

      ensure_packages(['zoo-kernel'], {
        ensure => $_package_ensure,
        name   => $package_name,
        tag    => 'ftep',
      })
    }

    file { "${cgi_path}/${main_config_file}":
      ensure  => 'present',
      mode    => '0644',
      owner   => 'root',
      group   => 'root',
      content => epp('ftep/zoo_kernel/main.cfg.epp', {
        'wps_version'    => $wps_version,
        'lang'           => $lang,
        'server_address' => $server_address,
        'data_path'      => $data_path,
        'tmp_path'       => $tmp_path,
        'tmp_url'        => $tmp_url,
        'cache_dir'      => $cache_dir,

        'provider_name'  => $provider_name,
        'provider_site'  => $provider_site,

        'db_name'        => $real_db_name,
        'db_host'        => $real_db_host,
        'db_port'        => $db_port,
        'db_type'        => $db_type,
        'db_schema'      => $db_schema,
        'db_user'        => $real_db_user,
        'db_pass'        => $real_db_pass,

        'env_config'     => $env_config,
        'ftep_config'    => $ftep_config,
        'other_config'   => $other_config,
      }),
      require => Package['zoo-kernel'],
    }
  }

  ::apache::vhost { 'ftep-wps':
    port          => '80',
    servername    => 'ftep-wps',
    docroot       => $cgi_path,
    scriptaliases => [{
      alias => $script_alias,
      path  => "${cgi_path}/${cgi_file}"
    }],
    options       => ['-Indexes']
  }

  # Ensure zoo_loader.cgi can access NFS shares
  if $facts['selinux'] {
    ::selinux::boolean { 'httpd_use_nfs':
      ensure => true,
    }
  }

}