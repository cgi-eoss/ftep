class ftep::backend::processors {
  ensure_packages(['f-tep-processors'], {
    ensure  => 'latest',
    name    => 'f-tep-processors',
  })
}
