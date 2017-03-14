class ftep::common::user (
  $user  = undef,
  $group = undef,
  $home  = '/home/ftep'
) {

  $uid = pick($user, $ftep::globals::user)
  $gid = pick($group, $ftep::globals::group)

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