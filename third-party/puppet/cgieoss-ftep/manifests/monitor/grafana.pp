class ftep::monitor::grafana (
  $db_name      = 'grafana',
  $db_username  = 'grafanauser',
  $db_password  = 'grafanapass',
  $context_path = '/monitor',
  $port         = undef,
) {

  require ::epel

  $real_port = pick($port, $ftep::globals::grafana_port)

  class { 'grafana':
    cfg => {
      app_mode => 'production',
      server   => {
        http_port => $real_port,
        root_url  => "%(protocol)s://%(domain)s:%(http_port)s${context_path}"
      },
      database => {
        type     => 'sqlite3',
        name     => $db_name,
      },
      users    => {
        allow_sign_up => false,
      },
    },
  }
}
