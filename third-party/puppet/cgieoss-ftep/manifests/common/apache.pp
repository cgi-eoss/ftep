class ftep::common::apache {

  class { ::apache:
    default_vhost => false,
  }

  ::apache::namevirtualhost { '*:80': }

  if $facts['selinux'] {
    ::selinux::boolean { 'httpd_can_network_connect_db':
      ensure => true,
    }

    ::selinux::boolean { 'httpd_can_network_connect':
      ensure => true,
    }

    ::selinux::port { 'f-tep-server':
      context  => 'http_port_t',
      port     => $ftep::globals::server_application_port,
      protocol => 'tcp'
    }

    ::selinux::port { 'f-tep-worker':
      context  => 'http_port_t',
      port     => $ftep::globals::worker_application_port,
      protocol => 'tcp'
    }

    ::selinux::port { 'grafana':
      context  => 'http_port_t',
      port     => $ftep::globals::grafana_port,
      protocol => 'tcp'
    }
  }

}
