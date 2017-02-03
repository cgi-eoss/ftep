class ftep::worker (
  $install_path     = '/var/f-tep',
  $config_file      = '/var/f-tep/etc/f-tep-worker.properties',

  $service_enable   = true,
  $service_ensure   = 'running',

  # f-tep-worker.properties config
  $application_port = undef,
) {

  require ::ftep::globals

  contain ::ftep::common::java
  # User and group are set up by the RPM if not included here
  contain ::ftep::common::user
  contain ::docker

  $real_application_port = pick($application_port, $ftep::globals::worker_application_port)

  # TODO Configure worker with f-tep-worker location for self-registration

  ensure_packages(['f-tep-worker'], {
    ensure => 'latest',
    name   => 'f-tep-worker',
    tag    => 'ftep',
  })

  service { 'f-tep-worker':
    ensure     => $service_ensure,
    enable     => $service_enable,
    hasrestart => true,
    hasstatus  => true,
    require    => [Package['f-tep-worker']],
  }

  file { $config_file:
    ensure  => 'present',
    owner   => 'ftep',
    group   => 'ftep',
    content => epp('ftep/worker/f-tep-worker.properties.epp', {
      'server_port' => $real_application_port,
    }),
    require => Package['f-tep-worker'],
  }

}
