class ftep::worker (
  $component_name           = 'f-tep-worker',

  $install_path             = '/var/f-tep/worker',
  $config_file              = '/var/f-tep/worker/f-tep-worker.conf',
  $logging_config_file      = '/var/f-tep/worker/log4j2.xml',
  $properties_file          = '/var/f-tep/worker/application.properties',

  $service_enable           = true,
  $service_ensure           = 'running',

  # f-tep-worker application.properties config
  $application_port         = undef,
  $grpc_port                = undef,

  $serviceregistry_user     = undef,
  $serviceregistry_pass     = undef,
  $serviceregistry_host     = undef,
  $serviceregistry_port     = undef,
  $serviceregistry_url      = undef,

  $worker_environment       = 'LOCAL',

  $cache_concurrency        = 4,
  $cache_maxweight          = 1024,
  $cache_dir                = 'dl',
  $jobs_dir                 = 'jobs',

  $ipt_auth_endpoint        = 'https://finder.eocloud.eu/resto/api/authidentity',
  # These are not undef so they're not mandatory parameters, but must be set correctly if IPT downloads are required
  $ipt_auth_domain          = '__secret__',
  $ipt_download_base_url    = '__secret__',

  $custom_config_properties = { },
) {

  require ::ftep::globals

  contain ::ftep::common::datadir
  contain ::ftep::common::java
  # User and group are set up by the RPM if not included here
  contain ::ftep::common::user
  contain ::ftep::common::docker

  $real_application_port = pick($application_port, $ftep::globals::worker_application_port)
  $real_grpc_port = pick($grpc_port, $ftep::globals::worker_grpc_port)

  $real_serviceregistry_user = pick($serviceregistry_user, $ftep::globals::serviceregistry_user)
  $real_serviceregistry_pass = pick($serviceregistry_pass, $ftep::globals::serviceregistry_pass)
  $real_serviceregistry_host = pick($serviceregistry_host, $ftep::globals::server_hostname)
  $real_serviceregistry_port = pick($serviceregistry_port, $ftep::globals::serviceregistry_application_port)
  $serviceregistry_creds = "${real_serviceregistry_user}:${real_serviceregistry_pass}"
  $serviceregistry_server = "${real_serviceregistry_host}:${real_serviceregistry_port}"
  $real_serviceregistry_url = pick($serviceregistry_url,
    "http://${serviceregistry_creds}@${serviceregistry_server}/eureka/")

  ensure_packages(['f-tep-worker'], {
    ensure => 'latest',
    name   => 'f-tep-worker',
    tag    => 'ftep',
    notify => Service['f-tep-worker'],
  })

  file { ["${ftep::common::datadir::data_basedir}/${cache_dir}", "${ftep::common::datadir::data_basedir}/${jobs_dir}"]:
    ensure  => directory,
    owner   => $ftep::globals::user,
    group   => $ftep::globals::group,
    mode    => '755',
    recurse => false,
    require => File[$ftep::common::datadir::data_basedir],
  }

  file { $config_file:
    ensure  => 'present',
    owner   => $ftep::globals::user,
    group   => $ftep::globals::group,
    content =>
      'JAVA_OPTS="-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"'
    ,
    require => Package['f-tep-worker'],
    notify  => Service['f-tep-worker'],
  }

  ::ftep::logging::log4j2 { $logging_config_file:
    ftep_component => $component_name,
    require        => Package['f-tep-worker'],
    notify         => Service['f-tep-worker'],
  }

  file { $properties_file:
    ensure  => 'present',
    owner   => $ftep::globals::user,
    group   => $ftep::globals::group,
    content => epp('ftep/worker/application.properties.epp', {
      'logging_config_file'   => $logging_config_file,
      'server_port'           => $real_application_port,
      'grpc_port'             => $real_grpc_port,
      'serviceregistry_url'   => $real_serviceregistry_url,
      'worker_environment'    => $worker_environment,
      'cache_basedir'         => "${ftep::common::datadir::data_basedir}/${cache_dir}",
      'cache_concurrency'     => $cache_concurrency,
      'cache_maxweight'       => $cache_maxweight,
      'jobs_basedir'          => "${ftep::common::datadir::data_basedir}/${jobs_dir}",
      'ipt_auth_endpoint'     => $ipt_auth_endpoint,
      'ipt_auth_domain'       => $ipt_auth_domain,
      'ipt_download_base_url' => $ipt_download_base_url,
      'custom_properties'     => $custom_config_properties,
    }),
    require => Package['f-tep-worker'],
    notify  => Service['f-tep-worker'],
  }

  service { 'f-tep-worker':
    ensure     => $service_ensure,
    enable     => $service_enable,
    hasrestart => true,
    hasstatus  => true,
    require    => [Package['f-tep-worker'], File[$properties_file]],
  }

}
