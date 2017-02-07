class ftep::monitor::grafana (
  $db_name = 'grafana',
  $db_username = 'grafanauser',
  $db_password = 'grafanapass'
) {

  require ::epel

  class { 'grafana':
    cfg => {
      app_mode => 'production',
      server   => {
        http_port     => 8080,
      },
      database => {
        type          => 'sqlite3',
        host          => '127.0.0.1:3306',
        name          => $db_name,
        user          => $db_username,
        password      => $db_password,
      },
      users    => {
        allow_sign_up => false,
      },
    },
  }
}
