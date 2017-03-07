define ftep::db::flyway_migration (
  $location,
  $placeholders,
  $db_username,
  $db_password,
  $jdbc_url,
) {

  require ::ftep::db::flyway

  $placeholders_args = join($placeholders.map |$items| { "-placeholders.${items[0]}=${items[1]}" }, ' ')

  $flyway_command =
    "flyway -user='${db_username}' -password='${db_password}' -url='${jdbc_url}' -locations='filesystem:${location}' ${placeholders_args}"

  $flyway_path = $ftep::db::flyway::path;

  exec { "Flyway migration: ${title}":
    cwd     => $flyway_path,
    path    => "${flyway_path}:/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin",
    unless  => "${flyway_command} validate",
    command => "${flyway_command} migrate",
  }

}