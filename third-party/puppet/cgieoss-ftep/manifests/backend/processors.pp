class ftep::backend::processors {
  ensure_resource(package, 'f-tep-processors', {
    ensure  => 'latest',
    name    => 'f-tep-processors',
  })
}
