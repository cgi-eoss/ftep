class ftep::zoomanager (
  $component_name       = 'f-tep-zoomanager',

  $install_path         = '/var/f-tep/zoomanager',
  $config_file          = '/var/f-tep/zoomanager/f-tep-zoomanager.conf',
  $logging_config_file  = '/var/f-tep/zoomanager/log4j2.xml',
  $properties_file      = '/var/f-tep/zoomanager/application.properties',

  $service_enable       = true,
  $service_ensure       = 'running',

  # f-tep-zoomanager application.properties config
  $application_port     = undef,
  $grpc_port            = undef,

  $serviceregistry_user = undef,
  $serviceregistry_pass = undef,
  $serviceregistry_host = undef,
  $serviceregistry_port = undef,
  $serviceregistry_url  = undef,

  $zcfg_path            = '/var/www/cgi-bin',
  $classpath_jar_files  = [],
  $services_stub_jar    = '/var/www/cgi-bin/jars/f-tep-services.jar',
) {

  require ::ftep::globals

  contain ::ftep::common::java
  # User and group are set up by the RPM if not included here
  contain ::ftep::common::user

  $real_application_port = pick($application_port, $ftep::globals::zoomanager_application_port)
  $real_grpc_port = pick($grpc_port, $ftep::globals::zoomanager_grpc_port)

  $real_serviceregistry_user = pick($serviceregistry_user, $ftep::globals::serviceregistry_user)
  $real_serviceregistry_pass = pick($serviceregistry_pass, $ftep::globals::serviceregistry_pass)
  $real_serviceregistry_host = pick($serviceregistry_host, $ftep::globals::server_hostname)
  $real_serviceregistry_port = pick($serviceregistry_port, $ftep::globals::serviceregistry_application_port)
  $serviceregistry_creds = "${real_serviceregistry_user}:${real_serviceregistry_pass}"
  $serviceregistry_server = "${real_serviceregistry_host}:${real_serviceregistry_port}"
  $real_serviceregistry_url = pick($serviceregistry_url,
    "http://${serviceregistry_creds}@${serviceregistry_server}/eureka/")

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
JAVA_OPTS="-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"'
    ,
    require => Package['f-tep-zoomanager'],
    notify  => Service['f-tep-zoomanager'],
  }

  ::ftep::logging::log4j2 { $logging_config_file:
    ftep_component => $component_name,
    require        => Package['f-tep-zoomanager'],
    notify         => Service['f-tep-zoomanager'],
  }

  file { $properties_file:
    ensure  => 'present',
    owner   => $ftep::globals::user,
    group   => $ftep::globals::group,
    content => epp('ftep/zoomanager/application.properties.epp', {
      'logging_config_file' => $logging_config_file,
      'server_port'         => $real_application_port,
      'grpc_port'           => $real_grpc_port,
      'serviceregistry_url' => $real_serviceregistry_url,
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
