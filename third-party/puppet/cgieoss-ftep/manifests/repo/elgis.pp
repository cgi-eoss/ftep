class ftep::repo::elgis {
  ensure_resource('yumrepo', 'elgis', {
    ensure   => 'present',
    descr    => 'EL GIS 6 - $basearch',
    baseurl  => 'http://elgis.argeo.org/repos/6/elgis/$basearch',
    enabled  => 1,
    gpgcheck => 1,
    gpgkey   => 'http://elgis.argeo.org/RPM-GPG-KEY-ELGIS',
  })
}