define ftep::logging::log4j2 (
  String $config_file             = $name,
  Boolean $enable_graylog         = false,
  String $graylog_server          = $ftep::globals::monitor_hostname,
  String $graylog_protocol        = 'TCP',
  Integer $graylog_port           = $ftep::globals::graylog_gelf_tcp_port,
  String $graylog_source_hostname = $trusted['certname'],
) {
  file { $config_file:
    ensure  => 'present',
    owner   => 'ftep',
    group   => 'ftep',
    content => epp('ftep/logging/log4j2.xml.epp', {
      'enable_graylog'          => $enable_graylog,
      'graylog_server'          => $graylog_server,
      'graylog_protocol'        => $graylog_protocol,
      'graylog_port'            => $graylog_port,
      'graylog_source_hostname' => $graylog_source_hostname
    })
  }
}