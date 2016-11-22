# == Class: ftep::portal
#
# Install and manage the F-TEP portal front-end components, including
# Drupal and the F-TEP web application.
#
class ftep::portal {

  require ::ftep::globals

  contain ::ftep::common::apache

  contain ::ftep::portal::repos
  contain ::ftep::portal::apache
  contain ::ftep::portal::drupal
  contain ::ftep::portal::webapp

}
