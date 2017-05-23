class ftep::webapp (
  $app_path        = '/var/www/html/f-tep',
  $app_config_file = 'scripts/ftepConfig.js',

  $ftep_url        = undef,
  $api_url         = undef,
  $api_v2_url      = undef,
  $sso_url         = 'https://eo-sso-idp.evo-pdgs.com',
  $mapbox_token    = 'pk.eyJ1IjoidmFuemV0dGVucCIsImEiOiJjaXZiZTM3Y2owMDdqMnVwa2E1N2VsNGJnIn0.A9BNRSTYajN0fFaVdJIpzQ',
) {

  require ::ftep::globals

  contain ::ftep::common::apache

  ensure_packages(['f-tep-portal'], {
    ensure => 'latest',
    name   => 'f-tep-portal',
    tag    => 'ftep',
  })

  $real_ftep_url = pick($ftep_url, $ftep::globals::base_url)
  $real_api_url = pick($api_url, "${ftep::globals::base_url}/secure/api/v1.0")
  $real_api_v2_url = pick($api_v2_url, "${$ftep::globals::base_url}/secure/api/v2.0")

  file { "${app_path}/${app_config_file}":
    ensure  => 'present',
    owner   => 'root',
    group   => 'root',
    content => epp('ftep/webapp/ftepConfig.js.epp', {
      'ftep_url'     => $real_ftep_url,
      'api_url'      => $real_api_url,
      'api_v2_url'   => $real_api_v2_url,
      'sso_url'      => $sso_url,
      'mapbox_token' => $mapbox_token,
    }),
    require => Package['f-tep-portal'],
  }

  ::apache::vhost { 'ftep-webapp':
    port       => '80',
    servername => 'ftep-webapp',
    docroot    => $app_path,
  }

}
