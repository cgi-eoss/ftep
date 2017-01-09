class ftep::common::java {
  class { ::java:
    package => 'java-1.8.0-openjdk-headless',
  }

  $ld_java_conf_epp = @(END)
<%= $libjvm_path %>
END

  $libjvm_path = $facts['java_libjvm_path'] ? {
    undef => '/etc/alternatives/jre/lib/amd64/server',
    default => $facts['java_libjvm_path']
  }

  # Add libjvm.so to the dynamic linker config
  file { '/etc/ld.so.conf.d/java.conf':
    ensure  => 'present',
    owner   => 'root',
    group   => 'root',
    content => inline_epp($ld_java_conf_epp, {
      'libjvm_path'    => $libjvm_path,
    }),
  } ~>
  exec { 'java_ldconfig':
    command     => '/sbin/ldconfig',
    refreshonly => true,
  }

}
