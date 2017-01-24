class ftep::common::user (
  $uid  = 'ftep',
  $gid  = 'ftep',
  $home = '/home/ftep'
) {

  group { $gid:
    ensure => present,
  }

  user { $uid:
    ensure     => present,
    gid        => $gid,
    managehome => true,
    home       => $home,
    shell      => '/bin/bash',
    system     => true,
    require    => Group[$gid],
  }

}