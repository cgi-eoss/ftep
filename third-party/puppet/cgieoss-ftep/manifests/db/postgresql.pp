class ftep::db::postgresql (
  $db_name     = $ftep::globals::ftep_db_name,
  $db_v2_name  = $ftep::globals::ftep_db_v2_name,
  $db_username = $ftep::globals::ftep_db_username,
  $db_password = $ftep::globals::ftep_db_password,
) {

  # EPEL is required for the postgis extensions
  require ::epel

  $acls = []

  unique([$ftep::globals::wps_hostname, $ftep::globals::drupal_hostname]).each | $host | {
    concat($acls, "host ${db_name} ${db_username} ${host} md5")
  }

  # We have to control the package version
  class { ::postgresql::globals:
    manage_package_repo => true,
    version             => '9.5',
  } ->
    class { ::postgresql::server:
      ipv4acls         => $acls,
      listen_addresses => '*',
    }
  class { ::postgresql::server::postgis: }
  class { ::postgresql::server::contrib: }

  ::postgresql::server::db { 'ftepdb':
    dbname   => $db_name,
    user     => $db_username,
    password => postgresql_password($db_username, $db_password),
    grant    => 'ALL',
  }
  ::postgresql::server::db { 'ftepdb_v2':
    dbname   => $db_v2_name,
    user     => $db_username,
    password => postgresql_password($db_username, $db_password),
    grant    => 'ALL',
  }

  ::postgresql::server::extension { 'ftepdb-postgis':
    database  => $db_name,
    extension => 'postgis',
    require   => Class['postgresql::server::postgis'],
  }
  ::postgresql::server::extension { 'ftepdb-postgis_topology':
    database  => $db_name,
    extension => 'postgis_topology',
    require   => Class['postgresql::server::postgis'],
  }
  ::postgresql::server::extension { 'ftepdb-unaccent':
    database  => $db_name,
    extension => 'unaccent',
    require   => Class['postgresql::server::contrib'],
  }
}