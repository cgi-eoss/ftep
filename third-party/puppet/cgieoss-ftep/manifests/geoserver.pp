# Install and manage GeoServer for WMS/WFS
class ftep::geoserver(
  $group = 'geoserver',
  $user = 'geoserver',
  $user_home = '/home/geoserver',
  $geoserver_home = '/opt/geoserver',
  $geoserver_data_dir = '/opt/geoserver-data',
  $config_file = '/etc/default/geoserver',
  $init_script = '/etc/init.d/geoserver',
  $geoserver_version = '2.10.0',
  $geoserver_download_url = 'http://sourceforge.net/projects/geoserver/files/GeoServer/2.10.0/geoserver-2.10.0-bin.zip',
  $geoserver_port = '9080',
  $geoserver_stopport = '9079',
) {

  contain ::ftep::common::java

  group { $group:
    ensure => present,
  }

  user { $user:
    ensure     => present,
    gid        => $group,
    managehome => true,
    home       => $user_home,
    shell      => '/bin/bash',
    system     => true,
    require    => Group[$group],
  }

  $geoserver_path = '/opt/geoserver-2.10.0'

  # Create empty directories to set up initial permissions
  file { $geoserver_path:
    ensure           => directory,
    mode             => '0755',
    owner            => $user,
    require          => User[$user],
  }
  file { $geoserver_home:
    ensure           => link,
    target           => $geoserver_path,
    require          => File[$geoserver_path],
  }
  file { $geoserver_data_dir:
    ensure           => directory,
    mode             => '0755',
    owner            => $user,
    require          => User[$user],
  }

  # Download and unpack the standalone platform-independent binary distribution
  $archive = "geoserver-${geoserver_version}"
  ::archive { $archive:
    ensure           => present,
    url              => $geoserver_download_url,
    follow_redirects => true,
    extension        => 'zip',
    digest_string    => '86d737c88ac60bc30efd65d3113925ee5c7502db',
    digest_type      => 'sha1',
    user             => $user,
    target           => '/opt',
    src_target       => $user_home,
    require          => [User[$user], File[$geoserver_path]],
  }

  $config_file_epp = @(END)
JAVA_HOME="<%= $java_home %>"
GEOSERVER_USER="<%= $geoserver_user %>"
GEOSERVER_HOME="<%= $geoserver_home %>"
GEOSERVER_DATA_DIR="<%= $geoserver_data_dir %>"
PORT="<%= $port %>"
STOPPORT="<%= $stopport %>"
END

  file { $config_file:
    ensure  => present,
    content => inline_epp($config_file_epp, {
      'java_home'          => '/etc/alternatives/jre',
      'geoserver_user'     => $user,
      'geoserver_home'     => $geoserver_home,
      'geoserver_data_dir' => $geoserver_data_dir,
      'port'               => $geoserver_port,
      'stopport'           => $geoserver_stopport,
    })
  }

  file { $init_script:
    ensure  => present,
    mode => '0755',
    content => epp('ftep/geoserver/initscript.sh.epp'), # no parameterisation yet
    require => [User[$user], Archive[$archive], File[$config_file]],
  }

  service { 'geoserver':
    ensure     => running,
    enable     => true,
    hasrestart => true,
    hasstatus  => true,
  }

}