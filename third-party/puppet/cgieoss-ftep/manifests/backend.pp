# == Class: ftep::backend
#
# Install and manage the F-TEP processing back-end components, including
# ZOO-Project's zoo-kernel WPS server and the F-TEP processors.
#
# === Parameters
#
#
#
class ftep::backend() {

  require ::ftep::globals

  contain ::ftep::common::java
  contain ::ftep::common::apache

  contain ::ftep::backend::repos
  contain ::ftep::backend::apache
  contain ::ftep::backend::zoo_kernel
  contain ::ftep::backend::processors

}
