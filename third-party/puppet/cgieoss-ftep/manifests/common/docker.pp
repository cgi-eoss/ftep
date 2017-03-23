class ftep::common::docker (
  $docker_group = 'dockerroot'
) {

  require ::ftep::common::user

  # garethr-docker does not use the correct group ID for CentOS 6/EPEL docker-io package...
  class { ::docker:
    socket_group => $docker_group,
  }

  # ... and does not allow overriding the docker_group param for ::docker::system_user, so we do that here
  exec { "docker-system-user-${ftep::globals::user}":
    command => "/usr/sbin/usermod -aG ${docker_group} ${ftep::globals::user}",
    unless  => "/bin/cat /etc/group | grep '^${docker_group}:' | grep -qw ${ftep::globals::user}",
    require => [Class['::ftep::common::user'], Class['::docker']],
  }

}