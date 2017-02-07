class ftep::monitor::telegraf(
  $influx_host = 'ftep-monitor',
  $influx_port = '8086',
  $influx_db   = 'ftep',
  $influx_user = 'ftep_user',
  $influx_pass = 'ftep_pass'
) {

  require ::ftep::globals
  require ::epel

  $real_influx_host = pick($influx_host, $ftep::globals::monitor_hostname)
  $real_influx_port = pick($influx_port, $ftep::globals::monitor_data_port)

  class { '::telegraf':
    hostname => $::hostname,
    outputs  => {
        'influxdb' => {
            'urls'     => [ "http://${real_influx_host}:${real_influx_port}" ],
            'database' => $influx_db,
            'username' => $influx_user,
            'password' => $influx_pass,
            }
        },
    inputs   => {
        'cpu' => {
            'percpu'   => true,
            'totalcpu' => true,
        },
    }
  }
}
