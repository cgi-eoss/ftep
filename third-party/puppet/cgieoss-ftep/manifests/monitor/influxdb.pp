class ftep::monitor::influxdb(
  $db_name = 'ftep',
  $db_username = 'ftep_user',
  $db_password = 'ftep_pass',
  $monitor_data_port = '8086'
) {

  require ::ftep::globals
  require ::epel

  $real_monitor_data_port = pick($monitor_data_port, $ftep::globals::monitor_data_port)

  class {'influxdb::server':
    http_bind_address => ":$real_monitor_data_port",
  }
}
