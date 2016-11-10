# == Class: ftep::portal
#
# Install and manage the F-TEP portal front-end components, including
# Drupal and the F-TEP web application.
#
class ftep::portal() {

  require ::ftep::globals

  contain ::ftep::portal::webapp

}
