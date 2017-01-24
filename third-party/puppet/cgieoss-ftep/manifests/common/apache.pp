class ftep::common::apache {

  class { ::apache:
    default_vhost => false,
  }

  ::apache::namevirtualhost { '*:80': }

  if $facts['selinux'] {
    ::selinux::boolean { 'httpd_can_network_connect_db':
      ensure => true,
    }

    ::selinux::port { 'f-tep-server':
      context  => 'http_port_t',
      port     => $ftep::globals::server_application_port,
      protocol => 'tcp'
    }
  }

}
