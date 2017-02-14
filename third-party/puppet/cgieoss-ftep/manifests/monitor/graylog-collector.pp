class ftep::monitor::graylog-collector(
  $log_path = '/var/log',
  $log_pattern = '*.log',
  $graylog_server = undef
) {

  require ::ftep::globals

  $real_graylog_server = pick($graylog_server, $ftep::globals::graylog_server)

  package { 'graylog-collector':
      ensure => 'installed'
  } ->

    file { '/etc/graylog/collector/collector.conf':
        ensure => present,
        mode => 640,
        content => epp('ftep/graylog-collector/collector.conf.epp', {
            'log_path'               => $log_path,
            'log_pattern'            => $log_pattern,
            'graylog_server'         => $real_graylog_server,
            '$graylog_port'          => $graylog_port,
            '$graylog_api_path'      => $graylog_api_path,
            '$graylog_gelf_tcp_port' => $graylog_gelf_tcp_port,
            '$graylog_context_path'  => $graylog_context_path
        })
    } -> 
      service { 'graylog-collector':
          ensure => 'running'
      }
}
