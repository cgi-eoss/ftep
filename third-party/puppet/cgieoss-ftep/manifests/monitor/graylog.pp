class ftep::monitor::graylog (
  $db_secret  = 'bQ999ugSIvHXfWQqrwvAomNxaMsErX6I4UWicpS9ZU8EDmuFnhX9AmcoM43s4VGKixd2f6d6cELbRuPWO5uayHnBrBbNWVth',
  $db_sha256  = 'a7fdfe53e2a13cb602def10146388c65051c67e60ee55c051668a1c709449111', # sha256 of graylogpass
  $db_bind_ip   = '127.0.0.1'
) {

  require ::epel

  $real_db_secret = pick($db_secret, $ftep::globals::graylog_secret)
  $real_db_sha256 = pick($db_sha256, $ftep::globals::graylog_sha256)
  $real_db_bind_ip = pick($db_bind_ip, $ftep::globals::graylog_bind_ip)

  class { 'mongodb::globals':
    manage_package_repo => true,
  }->
    class { 'mongodb::server':
      bind_ip => [$real_db_bind_ip],
    }

  class { 'elasticsearch':
    java_install => true,
    java_package => 'java-1.8.0-openjdk',
    manage_repo  => true,
    repo_version => '5.x',
  } ->
    elasticsearch::instance { 'graylog':
      config => {
        'cluster.name' => 'graylog',
        'network.host' => $real_db_bind_ip,
      }
    }

  class { 'graylog::repository':
    version => '2.2'
  }->
    class { 'graylog::server':
      config          => {
        password_secret => $real_db_secret,    # Fill in your password secret
        root_password_sha2 => $real_db_sha256, # Fill in your root password hash
    }
  }

}
