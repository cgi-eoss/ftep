class ftep::common::datadir (
  $data_basedir = '/data'
) {
  require ::ftep::common::user

  # TODO Use nfs server for $data_basedir
  file { $data_basedir:
    ensure  => directory,
    owner   => 'ftep',
    group   => 'ftep',
    mode    => '755',
    recurse => false,
  }
}