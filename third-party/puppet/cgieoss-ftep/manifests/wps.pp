# == Class: ftep::wps
#
# Install and manage ZOO-Project's zoo-kernel WPS server and the associated
# f-tep-zoomanager class
#
class ftep::wps (
  $manage_zoo_kernel          = true,
  $manage_package             = true,
  $package_ensure             = 'present',
  $package_name               = 'zoo-kernel',
  $cgi_path                   = '/var/www/cgi-bin',
  $jar_path                   = 'jars',
  $main_config_file           = 'main.cfg',
  $script_alias               = '/zoo_loader.cgi',
  $cgi_file                   = 'zoo_loader.cgi',

  # zoo-kernel config
  $wps_version                = '1.0.0',
  $lang                       = 'en-US',
  $server_address             = 'https://forestry-tep.eo.esa.int/wps',
  $data_basedir               = '/data',
  $data_path                  = 'wps',
  $tmp_path                   = 'wps_tmp',
  $tmp_url                    = '/secure/wps/ftep-output',
  $cache_dir                  = '/tmp',

  $provider_name              = 'CGI IT UK Ltd.',
  $provider_site              = 'https://forestry-tep.eo.esa.int/',

  $db_host                    = undef,
  $db_name                    = undef,
  $db_user                    = undef,
  $db_pass                    = undef,
  $db_port                    = '5432',
  $db_type                    = 'PG',
  $db_schema                  = 'public',

  $classpath_jar_files        = ['/var/www/cgi-bin/jars/f-tep-zoolib.jar'],
  $services_stub_jar_filename = 'f-tep-services.jar',

  $env_config                 = { },
  $ftep_config                = { },
  $java_config                = { },
  $other_config               = { },
) {

  require ::ftep::globals

  contain ::ftep::common::apache
  contain ::ftep::common::datadir
  contain ::ftep::common::java
  contain ::ftep::common::user
  require ::apache::mod::cgi

  # Extra repos
  require ::epel
  require ::ftep::repo::elgis

  $real_db_host = pick($db_host, $::ftep::globals::db_hostname)
  $real_db_name = pick($db_name, $::ftep::globals::ftep_db_zoo_name)
  $real_db_user = pick($db_user, $::ftep::globals::ftep_db_zoo_username)
  $real_db_pass = pick($db_pass, $::ftep::globals::ftep_db_zoo_password)

  $services_stub_jar = "${cgi_path}/${jar_path}/${services_stub_jar_filename}"

  class { ::ftep::zoomanager:
    zcfg_path           => $cgi_path,
    classpath_jar_files => $classpath_jar_files,
    services_stub_jar   => "${cgi_path}/${jar_path}/${services_stub_jar_filename}"
  }

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

    $jar_files = concat($classpath_jar_files, [$services_stub_jar])
    $default_classpath = join($jar_files, ':')
    $default_env_config = {
      'CLASSPATH'       => $default_classpath,
      'LD_LIBRARY_PATH' => $cgi_path,
    }
    $default_ftep_config = {
      'zooConfKeyJobId'          => 'uusid',
      'zooConfKeyUsernameHeader' => "HTTP_${ftep::globals::username_request_header}",
      'ftepServerGrpcHost'       => $ftep::globals::server_hostname,
      'ftepServerGrpcPort'       => $ftep::globals::server_grpc_port,
    }

    $logging_config_file = "${cgi_path}/log4j2.xml"
    ::ftep::logging::log4j2 { $logging_config_file:
      ftep_component    => 'f-tep-zoolib',
      is_spring_context => false,
    }
    $default_java_config = {
      'log4j.configurationFile' => $logging_config_file,
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
        'data_path'      => "${data_basedir}/${data_path}",
        'tmp_path'       => "${data_basedir}/${tmp_path}",
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

        'env_config'     => merge($default_env_config, $env_config),
        'ftep_config'    => merge($default_ftep_config, $ftep_config),
        'java_config'    => merge($default_java_config, $java_config),
        'other_config'   => $other_config,
      }),
      require => Package['zoo-kernel'],
    }
  }

  file { ["${data_basedir}/${data_path}", "${data_basedir}/${tmp_path}"]:
    ensure  => directory,
    owner   => $ftep::globals::user,
    group   => $ftep::globals::group,
    mode    => '777', # Allow httpd to write to this directory
    recurse => false,
    require => File[$data_basedir],
  }

  $zoo_db_migration_requires = defined(Class["::ftep::db"]) ? {
    true    => [Class['::ftep::db'], Package['zoo-kernel']],
    default => [Package['zoo-kernel']]
  }

  ftep::db::flyway_migration { 'zoo-kernel':
    location    => "${cgi_path}/sql",
    db_username => $ftep::globals::ftep_db_zoo_username,
    db_password => $ftep::globals::ftep_db_zoo_password,
    jdbc_url    => "jdbc:postgresql://${::ftep::globals::db_hostname}/${::ftep::globals::ftep_db_zoo_name}",
    require     => $zoo_db_migration_requires,
  }

  ::apache::vhost { 'ftep-wps':
    port          => '80',
    servername    => 'ftep-wps',
    docroot       => $cgi_path,
    docroot_owner => $ftep::globals::user,
    docroot_group => $ftep::globals::group,
    scriptaliases => [{
      alias => $script_alias,
      path  => "${cgi_path}/${cgi_file}"
    }],
    aliases       => [{
      alias => $tmp_url,
      path  => "${data_basedir}/${tmp_path}"
    }],
    directories   => [{
      path    => "${data_basedir}/${data_path}",
      options => [ '-Indexes' ]
    }],
    options       => ['-Indexes']
  }

  # Ensure zoo_loader.cgi can access NFS shares
  if $facts['selinux'] {
    ::selinux::boolean { 'httpd_use_nfs':
      ensure => true
    }
    selinux::boolean { 'httpd_execmem':
      ensure => true
    }
    selinux::fcontext { 'data-path-rw-context':
      context  => 'httpd_sys_rw_content_t',
      pathname => "${data_basedir}/${data_path}"
    }
    selinux::fcontext { 'tmp-path-rw-context':
      context  => 'httpd_sys_rw_content_t',
      pathname => "${data_basedir}/${tmp_path}"
    }
    selinux::module { 'ftep_wps':
      ensure  => 'present',
      content => "
module ftep_wps 1.0;

require {
    type sysctl_net_t;
    type httpd_sys_script_t;
    type port_t;
    type proc_net_t;
    class tcp_socket name_connect;
    class dir search;
    class file { read getattr open };
}

#============= httpd_sys_script_t ==============
allow httpd_sys_script_t port_t:tcp_socket name_connect;
allow httpd_sys_script_t proc_net_t:file { read getattr open };
allow httpd_sys_script_t sysctl_net_t:dir search;
allow httpd_sys_script_t sysctl_net_t:file read;
",
    }
  }

}
