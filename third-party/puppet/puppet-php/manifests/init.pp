# Base class with global configuration parameters that pulls in all
# enabled components.
#
# === Parameters
#
# [*ensure*]
#   Specify which version of PHP packages to install, defaults to 'present'.
#   Please note that 'absent' to remove packages is not supported!
#
# [*manage_repos*]
#   Include repository (dotdeb, ppa, etc.) to install recent PHP from
#
# [*fpm*]
#   Install and configure php-fpm
#
# [*fpm_service_enable*]
#   Enable/disable FPM service
#
# [*fpm_service_ensure*]
#   Ensure FPM service is either 'running' or 'stopped'
#
# [*fpm_service_name*]
#   This is the name of the php-fpm service. It defaults to reasonable OS
#   defaults but can be different in case of using php7.0/other OS/custom fpm service
#
# [*fpm_service_provider*]
#   This is the name of the service provider, in case there is a non
#   OS default service provider used to start FPM.
#   Defaults to 'undef', pick system defaults.
#
# [*fpm_pools*]
#   Hash of php::fpm::pool resources that will be created. Defaults
#   to a single php::fpm::pool named www with default parameters.
#
# [*fpm_global_pool_settings*]
#   Hash of defaults params php::fpm::pool resources that will be created.
#   Defaults to empty hash.
#
# [*fpm_inifile*]
#   Path to php.ini for fpm
#
# [*fpm_package*]
#   Name of fpm package to install
#
# [*dev*]
#   Install php header files, needed to install pecl modules
#
# [*composer*]
#   Install and auto-update composer
#
# [*pear*]
#   Install PEAR
#
# [*phpunit*]
#   Install phpunit
#
# [*apache_config*]
#   Manage apache's mod_php configuration
#
# [*proxy_type*]
#    proxy server type (none|http|https|ftp)
#
# [*proxy_server*]
#   specify a proxy server, with port number if needed. ie: https://example.com:8080.
#
# [*extensions*]
#   Install PHP extensions, this is overwritten by hiera hash `php::extensions`
#
# [*package_prefix*]
#   This is the prefix for constructing names of php packages. This defaults
#   to a sensible default depending on your operating system, like 'php-' or
#   'php5-'.
#
# [*config_root_ini*]
#   This is the path to the config .ini files of the extensions. This defaults
#   to a sensible default depending on your operating system, like
#   '/etc/php5/mods-available' or '/etc/php5/conf.d'.
#
# [*config_root_inifile*]
#   The path to the global php.ini file. This defaults to a sensible default
#   depending on your operating system.
#
# [*ext_tool_enable*]
#   Absolute path to php tool for enabling extensions in debian/ubuntu systems.
#   This defaults to '/usr/sbin/php5enmod'.
#
# [*ext_tool_query*]
#   Absolute path to php tool for querying information about extensions in
#   debian/ubuntu systems. This defaults to '/usr/sbin/php5query'.
#
# [*ext_tool_enabled*]
#   Enable or disable the use of php tools on debian based systems
#   debian/ubuntu systems. This defaults to 'true'.
#
# [*log_owner*]
#   The php-fpm log owner
#
# [*log_group*]
#   The group owning php-fpm logs
#
# [*embedded*]
#   Enable embedded SAPI
#
# [*pear_ensure*]
#   The package ensure of PHP pear to install and run pear auto_discover
#
# [*settings*]
#
class php (
  $ensure                   = $::php::params::ensure,
  $manage_repos             = $::php::params::manage_repos,
  $fpm                      = true,
  $fpm_service_enable       = $::php::params::fpm_service_enable,
  $fpm_service_ensure       = $::php::params::fpm_service_ensure,
  $fpm_service_name         = $::php::params::fpm_service_name,
  $fpm_service_provider     = undef,
  $fpm_pools                = { 'www' => {} },
  $fpm_global_pool_settings = {},
  $fpm_inifile              = $::php::params::fpm_inifile,
  $fpm_package              = undef,
  $embedded                 = false,
  $dev                      = true,
  $composer                 = true,
  $pear                     = true,
  $pear_ensure              = $::php::params::pear_ensure,
  $phpunit                  = false,
  $apache_config            = false,
  $proxy_type               = undef,
  $proxy_server             = undef,
  $extensions               = {},
  $settings                 = {},
  $package_prefix           = $::php::params::package_prefix,
  $config_root_ini          = $::php::params::config_root_ini,
  $config_root_inifile      = $::php::params::config_root_inifile,
  $ext_tool_enable          = $::php::params::ext_tool_enable,
  $ext_tool_query           = $::php::params::ext_tool_query,
  $ext_tool_enabled         = $::php::params::ext_tool_enabled,
  $log_owner                = $::php::params::fpm_user,
  $log_group                = $::php::params::fpm_group,
) inherits ::php::params {

  validate_string($ensure)
  validate_bool($manage_repos)
  validate_bool($fpm)
  validate_bool($embedded)
  validate_bool($dev)
  validate_bool($composer)
  validate_bool($pear)
  validate_bool($ext_tool_enabled)
  validate_string($pear_ensure)
  validate_bool($phpunit)
  validate_bool($apache_config)
  validate_hash($extensions)
  validate_hash($settings)
  validate_hash($fpm_pools)
  validate_hash($fpm_global_pool_settings)
  validate_string($log_owner)
  validate_string($log_group)
  validate_absolute_path($config_root_ini)
  validate_absolute_path($config_root_inifile)

  if $ext_tool_enable != undef {
    validate_absolute_path($ext_tool_enable)
  }
  if $ext_tool_query != undef {
    validate_absolute_path($ext_tool_query)
  }

  $real_fpm_package = pick($fpm_package, "${package_prefix}${::php::params::fpm_package_suffix}")

  # Deep merge global php settings
  $real_settings = deep_merge($settings, hiera_hash('php::settings', {}))

  # Deep merge global php extensions
  $real_extensions = deep_merge($extensions, hiera_hash('php::extensions', {}))

  # Deep merge fpm_pools
  $real_fpm_pools = deep_merge($fpm_pools, hiera_hash('php::fpm_pools', {}))

  # Deep merge fpm_global_pool_settings
  $real_fpm_global_pool_settings = deep_merge($fpm_global_pool_settings, hiera_hash('php::fpm_global_pool_settings', {}))

  if $manage_repos {
    class { '::php::repo': } ->
    Anchor['php::begin']
  }

  anchor { 'php::begin': } ->
    class { '::php::packages': } ->
    class { '::php::cli':
      settings => $real_settings,
    } ->
  anchor { 'php::end': }

  # Configure global PHP settings in php.ini
  if $::osfamily != 'Debian' {
    Class['php::packages'] ->
    class {'::php::global':
      settings => $real_settings,
    } ->
    Anchor['php::end']
  }

  if $fpm { contain '::php::fpm' }
  if $embedded {
    if $::osfamily == 'RedHat' and $fpm {
      # Both fpm and embeded SAPIs are using same php.ini
      fail('Enabling both cli and embedded sapis is not currently supported')
    }

    Anchor['php::begin'] ->
      class { '::php::embedded':
        settings => $real_settings,
      } ->
    Anchor['php::end']
  }
  if $dev {
    Anchor['php::begin'] ->
      class { '::php::dev': } ->
    Anchor['php::end']
  }
  if $composer {
    Anchor['php::begin'] ->
      class { '::php::composer':
        proxy_type   => $proxy_type,
        proxy_server => $proxy_server,
      } ->
    Anchor['php::end']
  }
  if $pear {
    Anchor['php::begin'] ->
      class { '::php::pear':
        ensure => $pear_ensure,
      } ->
    Anchor['php::end']
  }
  if $phpunit {
    Anchor['php::begin'] ->
      class { '::php::phpunit': } ->
    Anchor['php::end']
  }
  if $apache_config {
    Anchor['php::begin'] ->
      class { '::php::apache_config':
        settings => $real_settings,
      } ->
    Anchor['php::end']
  }

  create_resources('::php::extension', $real_extensions, {
    require => Class['::php::cli'],
    before  => Anchor['php::end']
  })

  # On FreeBSD purge the system-wide extensions.ini. It is going
  # to be replaced with per-module configuration files.
  if $::osfamily == 'FreeBSD' {
    # Purge the system-wide extensions.ini
    file { '/usr/local/etc/php/extensions.ini':
      ensure  => absent,
      require => Class['::php::packages'],
    }
  }
}
