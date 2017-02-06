class ftep::db(
  $trust_local_network = false,
) {

  require ::ftep::globals

  contain ::ftep::db::postgresql

}