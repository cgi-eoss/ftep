class ftep::portal::repos {

  ensure_resource(yumrepo, 'shibboleth', {
    ensure   => 'present',
    descr    => 'Shibboleth (CentOS_CentOS-6)',
    baseurl  => 'http://download.opensuse.org/repositories/security:/shibboleth/CentOS_CentOS-6/',
    enabled  => 1,
    gpgcheck => 1,
    gpgkey   => 'http://download.opensuse.org/repositories/security:/shibboleth/CentOS_CentOS-6/repodata/repomd.xml.key',
  })

  ensure_resource(yumrepo, 'webtatic', {
    ensure     => 'present',
    descr      => 'Webtatic Repository EL6 - $basearch',
    mirrorlist => 'https://mirror.webtatic.com/yum/el6/$basearch/mirrorlist',
    enabled    => 1,
    gpgcheck   => 1,
    gpgkey     => 'https://mirror.webtatic.com/yum/RPM-GPG-KEY-webtatic-andy',
  })

}