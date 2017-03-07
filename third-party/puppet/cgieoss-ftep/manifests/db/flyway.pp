class ftep::db::flyway (
  $version            = '4.1.2',
  $repo               = 'https://repo1.maven.org/maven2',
  $group_id           = 'org/flywaydb',
  $artifact_id        = 'flyway-commandline',
  $classifier         = 'linux-x64',
  $extension          = 'tar.gz',
  $source             = undef,
  $source_digest      = 'b61e7895011ed28aa88f5aff12ff306dad28636f',
  $source_digest_type = 'sha1',
  $install_dir        = '/opt',
) {

  require ::ftep::globals

  contain ::ftep::common::java

  $flyway_commandline_url = pick(
    $source,
    "${repo}/${group_id}/${artifact_id}/${version}/${artifact_id}-${version}-${classifier}.${extension}"
  )

  $root_dir = "flyway-${version}"

  # Download and unpack the standalone platform-independent binary distribution
  $archive = "flyway-commandline-${version}"
  ::archive { $archive:
    ensure           => present,
    url              => $flyway_commandline_url,
    follow_redirects => true,
    extension        => $extension,
    digest_string    => $source_digest,
    digest_type      => $source_digest_type,
    user             => 'root',
    target           => $install_dir,
    root_dir         => $root_dir,
  }

  $path = "${install_dir}/${root_dir}"

  Class['ftep::db::flyway'] -> Ftep::Db::Flyway_migration<||>

}