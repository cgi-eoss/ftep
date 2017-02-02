class ftep::worker {

  require ::ftep::globals

  contain ::ftep::common::java
  # User and group are set up by the RPM if not included here
  contain ::ftep::common::user
  contain ::docker

  # TODO Install f-tep-worker service
  # TODO Configure worker with f-tep-server location for self-registration

}
