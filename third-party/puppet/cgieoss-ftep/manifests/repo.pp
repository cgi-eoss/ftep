class ftep::repo(
  $location
) {
  case $::osfamily {
    'RedHat', 'Linux': {
      class { 'ftep::repo::ftep': }
    }
    default: {
      fail("Unsupported managed repository for osfamily: ${::osfamily}, operatingsystem: ${::operatingsystem}, module ${module_name} currently only supports managing repos for osfamily RedHat")
    }
  }
}