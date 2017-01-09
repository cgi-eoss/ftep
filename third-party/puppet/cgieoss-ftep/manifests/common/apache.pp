class ftep::common::apache {

  class { ::apache:
    default_vhost => false,
  }

  ::apache::namevirtualhost {'*:80':}

  if $facts['selinux'] {
    ::selinux::boolean{ 'httpd_can_network_connect_db':
      ensure => true,
    }
  }

}
