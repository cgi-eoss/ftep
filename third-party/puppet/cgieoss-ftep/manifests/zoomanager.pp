class ftep::zoomanager (
  $install_path        = '/var/f-tep/zoomanager',
  $config_file         = '/var/f-tep/zoomanager/f-tep-zoomanager.conf',
  $logging_config_file = '/var/f-tep/zoomanager/log4j2.xml',
  $properties_file     = '/var/f-tep/zoomanager/application.properties',

  $service_enable      = true,
  $service_ensure      = 'running',

  # f-tep-zoomanager.properties config
  $application_port    = undef,
  $grpc_port           = undef,

  $zcfg_path           = '/var/www/cgi-bin',
  $classpath_jar_files = [],
  $services_stub_jar   = '/var/www/cgi-bin/jars/f-tep-services.jar',
) {

  require ::ftep::globals

  contain ::ftep::common::java
  # User and group are set up by the RPM if not included here
  contain ::ftep::common::user

  $real_application_port = pick($application_port, $ftep::globals::zoomanager_application_port)
  $real_grpc_port = pick($grpc_port, $ftep::globals::zoomanager_grpc_port)

  # JDK is necessary to compile service stubs
  ensure_packages(['java-1.8.0-openjdk-devel'])

  ensure_packages(['f-tep-zoomanager'], {
    ensure => 'latest',
    name   => 'f-tep-zoomanager',
    tag    => 'ftep',
    notify => Service['f-tep-zoomanager'],
  })

  file { $config_file:
    ensure  => 'present',
    owner   => $ftep::globals::user,
    group   => $ftep::globals::group,
    content => 'JAVA_HOME=/etc/alternatives/java_sdk
JAVA_OPTS=-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector',
    require => Package['f-tep-zoomanager'],
    notify  => Service['f-tep-zoomanager'],
  }

  ::ftep::logging::log4j2 { $logging_config_file:
    require => Package['f-tep-zoomanager'],
    notify  => Service['f-tep-zoomanager'],
  }

  file { $properties_file:
    ensure  => 'present',
    owner   => $ftep::globals::user,
    group   => $ftep::globals::group,
    content => epp('ftep/zoomanager/application.properties.epp', {
      'logging_config_file' => $logging_config_file,
      'server_port'         => $real_application_port,
      'grpc_port'           => $real_grpc_port,
      'zcfg_path'           => $zcfg_path,
      'javac_classpath'     => join($classpath_jar_files, ':'),
      'services_stub_jar'   => $services_stub_jar,
    }),
    require => Package['f-tep-zoomanager'],
    notify  => Service['f-tep-zoomanager'],
  }

  service { 'f-tep-zoomanager':
    ensure     => $service_ensure,
    enable     => $service_enable,
    hasrestart => true,
    hasstatus  => true,
    require    => [Package['f-tep-zoomanager'], File[$properties_file]],
  }

}
