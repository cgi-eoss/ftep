class ftep::common::php {

  # Repo for updated PHP packages
  ensure_resource(yumrepo, 'webtatic', {
    ensure     => 'present',
    descr      => 'Webtatic Repository EL6 - $basearch',
    mirrorlist => 'https://mirror.webtatic.com/yum/el6/$basearch/mirrorlist',
    enabled    => 1,
    gpgcheck   => 1,
    gpgkey     => 'https://mirror.webtatic.com/yum/RPM-GPG-KEY-webtatic-andy',
  })

  # PHP 5.6
  class { ::php:
    ensure         => latest,
    manage_repos   => false,
    fpm            => true,
    package_prefix => 'php56w-',
    composer       => true,
    pear           => true,
    extensions     => {
      xml      => { },
      gd       => { },
      pdo      => { },
      mbstring => { },
      pgsql    => { },
    },
    settings       => {
      'PHP/max_execution_time'  => '90',
      'PHP/max_input_time'      => '300',
      'PHP/memory_limit'        => '64M',
      'PHP/post_max_size'       => '32M',
      'PHP/upload_max_filesize' => '32M',
      'Date/date.timezone'      => 'UTC',
    },
    require        => Yumrepo['webtatic']
  }

}