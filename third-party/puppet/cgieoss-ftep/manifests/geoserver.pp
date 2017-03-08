# Install and manage GeoServer for WMS/WFS
class ftep::geoserver (
  $group                  = 'geoserver',
  $user                   = 'geoserver',
  $user_home              = '/home/geoserver',
  $geoserver_home         = '/opt/geoserver',
  $geoserver_data_dir     = '/opt/geoserver-data',
  $config_file            = '/etc/default/geoserver',
  $init_script            = '/etc/init.d/geoserver',
  $geoserver_version      = '2.10.0',
  $geoserver_download_url = 'http://sourceforge.net/projects/geoserver/files/GeoServer/2.10.0/geoserver-2.10.0-bin.zip',
  $geoserver_extension    = 'zip',
  $geoserver_digest       = '86d737c88ac60bc30efd65d3113925ee5c7502db',
  $geoserver_digest_type  = 'sha1',
  $geoserver_port         = undef,
  $geoserver_stopport     = undef,
) {

  require ::ftep::globals

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

  ensure_packages(['unzip'])

  # This is created by the ::archive resource
  $geoserver_path = "${user_home}/geoserver-2.10.0"

  # Download and unpack the standalone platform-independent binary distribution
  $archive = "geoserver-${geoserver_version}"
  ::archive { $archive:
    ensure           => present,
    url              => $geoserver_download_url,
    follow_redirects => true,
    extension        => $geoserver_extension,
    digest_string    => $geoserver_digest,
    digest_type      => $geoserver_digest_type,
    user             => $user,
    target           => $user_home,
    src_target       => $user_home,
    require          => [User[$user], Package['unzip']],
  }

  file { $geoserver_home:
    ensure  => link,
    target  => $geoserver_path,
    require => Archive[$archive],
  }
  file { $geoserver_data_dir:
    ensure  => directory,
    mode    => '0755',
    owner   => $user,
    require => User[$user],
  }

  $config_file_epp = @(END)
JAVA_HOME="<%= $java_home %>"
GEOSERVER_USER="<%= $geoserver_user %>"
GEOSERVER_HOME="<%= $geoserver_home %>"
GEOSERVER_DATA_DIR="<%= $geoserver_data_dir %>"
PORT="<%= $port %>"
STOPPORT="<%= $stopport %>"
END

  $real_port = pick($geoserver_port, $ftep::globals::geoserver_port)
  $real_stopport = pick($geoserver_stopport, $ftep::globals::geoserver_stopport)

  file { $config_file:
    ensure  => present,
    content => inline_epp($config_file_epp, {
      'java_home'          => '/etc/alternatives/jre',
      'geoserver_user'     => $user,
      'geoserver_home'     => $geoserver_home,
      'geoserver_data_dir' => $geoserver_data_dir,
      'port'               => "${real_port}",
      'stopport'           => "${real_stopport}",
    })
  }

  file { $init_script:
    ensure  => present,
    mode    => '0755',
    content => epp('ftep/geoserver/initscript.sh.epp'), # no parameterisation yet
    require => [User[$user], Archive[$archive], File[$config_file]],
  }

  service { 'geoserver':
    ensure     => running,
    enable     => true,
    hasrestart => true,
    hasstatus  => true,
    require    => [File[$init_script]],
  }

}