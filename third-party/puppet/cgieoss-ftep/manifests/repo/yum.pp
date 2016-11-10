# F-TEP package repository
class ftep::repo::yum {
  ensure_resource(yumrepo, 'ftep', {
    ensure   => 'present',
    descr    => 'F-TEP',
    baseurl  => $ftep::repo::location,
    enabled  => 1,
    gpgcheck => 0,
  })

  # Ensure this (and all other) yumrepos are available before packages
  Yumrepo <| |> -> Package <| provider != 'rpm' |>
}