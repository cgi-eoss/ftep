#!/usr/bin/env bash
#
# Download and unpack the tip of the master branch, or the specific commit, of
# the ZOO-Project git repository. The unpacked code should be committed locally
# to allow totally offline builds.

set -ex

ZOO_WS=$(dirname $(readlink -f "$0"))/src

# Download trunk revision (using github mirror of svn trunk) by default
ZOO_REV="${1:-master}"
rm -rf "${ZOO_WS}" && mkdir -p "${ZOO_WS}" && cd "${ZOO_WS}"
curl -sL https://github.com/OSGeo/zoo-project/archive/${ZOO_REV}.tar.gz | tar xz --strip-components=1

# Prune large unnecessary non-source dir...
rm -rf "${ZOO_WS}/workshop"