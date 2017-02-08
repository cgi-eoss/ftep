class ftep::server (
  $install_path               = '/var/f-tep',
  $config_file                = '/var/f-tep/etc/f-tep-server.properties',

  $service_enable             = true,
  $service_ensure             = 'running',

  # f-tep-server.properties config
  $application_port           = undef,
  $grpc_port                  = undef,
  $api_base_path              = '/secure/api/v2.0',

  $jdbc_url                   = undef,
  $jdbc_driver                = 'org.postgresql.Driver',
  $jdbc_user                  = undef,
  $jdbc_password              = undef,
  $jdbc_datasource_class_name = 'org.postgresql.ds.PGSimpleDataSource',

  $local_worker_hostname      = 'ftep-worker',
  $local_worker_grpc_port     = undef,
) {

  require ::ftep::globals

  contain ::ftep::common::java
  # User and group are set up by the RPM if not included here
  contain ::ftep::common::user

  $real_application_port = pick($application_port, $ftep::globals::server_application_port)
  $real_grpc_port = pick($grpc_port, $ftep::globals::server_grpc_port)

  $default_jdbc_url = "jdbc:postgresql://${::ftep::globals::db_hostname}/${::ftep::globals::ftep_db_v2_name}"
  $real_db_url = pick($jdbc_url, $default_jdbc_url)
  $real_db_user = pick($jdbc_user, $::ftep::globals::ftep_db_username)
  $real_db_pass = pick($jdbc_password, $::ftep::globals::ftep_db_password)

  $real_local_worker_grpc_port = pick($local_worker_grpc_port, $ftep::globals::worker_grpc_port)

  ensure_packages(['f-tep-server'], {
    ensure => 'latest',
    name   => 'f-tep-server',
    tag    => 'ftep',
  })

  file { $config_file:
    ensure  => 'present',
    owner   => 'ftep',
    group   => 'ftep',
    content => epp('ftep/server/f-tep-server.properties.epp', {
      'server_port'                 => $real_application_port,
      'grpc_port'                   => $real_grpc_port,
      'api_base_path'               => $api_base_path,
      'jdbc_driver'                 => $jdbc_driver,
      'jdbc_url'                    => $real_db_url,
      'jdbc_user'                   => $real_db_user,
      'jdbc_password'               => $real_db_pass,
      'jdbc_data_source_class_name' => $jdbc_datasource_class_name,
      'local_worker_hostname'       => $local_worker_hostname,
      'local_worker_grpc_port'      => $real_local_worker_grpc_port,
    }),
    require => Package['f-tep-server'],
    notify  => Service['f-tep-server'],
  }

  service { 'f-tep-server':
    ensure     => $service_ensure,
    enable     => $service_enable,
    hasrestart => true,
    hasstatus  => true,
    require    => [Package['f-tep-server'], File[$config_file]],
  }

}
