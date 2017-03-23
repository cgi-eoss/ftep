# Install composer package manager
#
# === Parameters
#
# [*source*]
#   Holds URL to the Composer source file
#
# [*path*]
#   Holds path to the Composer executable
#
# [*proxy_type*]
#    proxy server type (none|http|https|ftp)
#
# [*proxy_server*]
#   specify a proxy server, with port number if needed. ie: https://example.com:8080.
#
# [*auto_update*]
#   Defines if composer should be auto updated
#
# [*max_age*]
#   Defines the time in days after which an auto-update gets executed
#
# [*root_group*]
#   UNIX group of the root user
#
class php::composer (
  $source       = $::php::params::composer_source,
  $path         = $::php::params::composer_path,
  $proxy_type   = undef,
  $proxy_server = undef,
  $auto_update  = true,
  $max_age      = $::php::params::composer_max_age,
  $root_group   = $::php::params::root_group,
) inherits ::php::params {

  if $caller_module_name != $module_name {
    warning('php::composer is private')
  }

  validate_string($source)
  validate_absolute_path($path)
  validate_bool($auto_update)
  validate_re("x${max_age}", '^x\d+$')

  archive { 'download composer':
    path         => $path,
    source       => $source,
    proxy_type   => $proxy_type,
    proxy_server => $proxy_server,
  } ->
  file { $path:
    mode  => '0555',
    owner => root,
    group => $root_group,
  }

  if $auto_update {
    class { '::php::composer::auto_update':
      max_age      => $max_age,
      source       => $source,
      path         => $path,
      proxy_type   => $proxy_type,
      proxy_server => $proxy_server,
    }
  }
}
