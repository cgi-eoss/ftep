#!/usr/bin/env sh

set -xe

if [ "" = "${WORKSPACE}" ]; then
  # Make sure WORKSPACE is set relative to this file's location
  WORKSPACE=$(dirname $(dirname $(readlink -f "$0")))
fi

DISTDIR="${WORKSPACE}/.dist"
DISTDIR_NOARCH="${DISTDIR}/repo/6/local/noarch/RPMS"
DISTDIR_OS="${DISTDIR}/repo/6/local/x86_64/RPMS"

mkdir -p "${DISTDIR_NOARCH}" "${DISTDIR_OS}"
