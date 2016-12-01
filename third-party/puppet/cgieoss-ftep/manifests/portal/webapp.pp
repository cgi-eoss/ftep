class ftep::portal::webapp(
  $app_path  = '/var/www/html/f-tep/app',
  $app_config_file  = 'scripts/ftepConfig.js',

  $url_prefix   = 'http://localhost',
  $api_url      = 'http://localhost/secure/api/v1.0',
  $zoo_url      = 'http://localhost/wps',
  $mapbox_token = 'pk.eyJ1IjoidmFuemV0dGVucCIsImEiOiJjaXZiZTM3Y2owMDdqMnVwa2E1N2VsNGJnIn0.A9BNRSTYajN0fFaVdJIpzQ',
) {
  ensure_packages(['f-tep-portal'], {
    ensure => 'latest',
    name   => 'f-tep-portal',
    tag    => 'ftep',
  })

  file { "${app_path}/${app_config_file}":
    ensure  => 'present',
    owner   => 'root',
    group   => 'root',
    content => epp('ftep/portal/webapp/ftepConfig.js.epp', {
      'url_prefix'   => $url_prefix,
      'api_url'      => $api_url,
      'zoo_url'      => $zoo_url,
      'mapbox_token' => $mapbox_token,
    }),
    require => Package['f-tep-portal'],
  }

#  ::apache::vhost { 'ftep-webapp':
#    port             => '80',
#    servername       => $::fqdn,
#    docroot          => "$app_path",
#  }

}
