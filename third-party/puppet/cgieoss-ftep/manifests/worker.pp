class ftep::worker (
  $install_path          = '/var/f-tep',
  $config_file           = '/var/f-tep/etc/f-tep-worker.properties',

  $service_enable        = true,
  $service_ensure        = 'running',

  # f-tep-worker.properties config
  $application_port      = undef,
  $grpc_port             = undef,

  $ftep_server_grpc_host = undef,
  $ftep_server_grpc_port = undef,

  $cache_concurrency     = 4,
  $cache_maxweight       = 1024,
  $data_basedir          = '/data',
  $cache_dir             = 'dl',
  $jobs_dir              = 'jobs',
) {

  require ::ftep::globals

  contain ::ftep::common::java
  # User and group are set up by the RPM if not included here
  contain ::ftep::common::user
  contain ::docker

  $real_application_port = pick($application_port, $ftep::globals::worker_application_port)
  $real_grpc_port = pick($grpc_port, $ftep::globals::worker_grpc_port)
  $real_server_grpc_host = pick($ftep_server_grpc_host, $ftep::globals::server_hostname)
  $real_server_grpc_port = pick($ftep_server_grpc_port, $ftep::globals::server_grpc_port)

  ensure_packages(['f-tep-worker'], {
    ensure => 'latest',
    name   => 'f-tep-worker',
    tag    => 'ftep',
    notify => Service['f-tep-worker'],
  })

  # TODO Manage nfs server for $data_basedir

  file { $data_basedir:
    ensure  => directory,
    owner   => 'ftep',
    group   => 'ftep',
    mode    => '755',
    recurse => false,
  }

  file { ["${data_basedir}/$cache_dir", "${data_basedir}/$jobs_dir"]:
    ensure  => directory,
    owner   => 'ftep',
    group   => 'ftep',
    mode    => '755',
    recurse => false,
    require => File[$data_basedir],
  }

  file { $config_file:
    ensure  => 'present',
    owner   => 'ftep',
    group   => 'ftep',
    content => epp('ftep/worker/f-tep-worker.properties.epp', {
      'server_port'           => $real_application_port,
      'grpc_port'             => $real_grpc_port,
      'ftep_server_grpc_host' => $real_server_grpc_host,
      'ftep_server_grpc_port' => $real_server_grpc_port,
      'cache_basedir'         => "${data_basedir}/$cache_dir",
      'cache_concurrency'     => $cache_concurrency,
      'cache_maxweight'       => $cache_maxweight,
      'jobs_basedir'          => "${data_basedir}/$jobs_dir",
    }),
    require => Package['f-tep-worker'],
    notify  => Service['f-tep-worker'],
  }

  service { 'f-tep-worker':
    ensure     => $service_ensure,
    enable     => $service_enable,
    hasrestart => true,
    hasstatus  => true,
    require    => [Package['f-tep-worker'], File[$config_file]],
  }

}
