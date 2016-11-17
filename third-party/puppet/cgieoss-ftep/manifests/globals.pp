# Class for setting cross-class global overrides.
class ftep::globals(
  $manage_package_repo = true,

  $ftep_backend_host = 'localhost',

  $ftep_portal_host = 'localhost',

  # All classes should share this database config, or override it if necessary
  $ftep_db_host = 'localhost',
  $ftep_db_name = 'ftep',
  $ftep_db_username = 'ftep-user',
  $ftep_db_password = 'ftep-pass',
) {
  # Setup of the repo only makes sense globally, so we are doing this here.
  if($manage_package_repo) {
    class { 'ftep::repo': }
  }
}