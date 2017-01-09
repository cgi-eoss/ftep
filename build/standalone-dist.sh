#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

PUPPET_WS="${WORKSPACE}/distribution/puppet"
MODULE_PATH="${PUPPET_WS}/modules"
PKG_PATH="${WORKSPACE}/third-party/pkg"
PUPPET_FTEP="${WORKSPACE}/third-party/puppet/cgieoss-ftep"

# cp ${PKG_PATH}/*/*.noarch.rpm $DISTDIR_NOARCH # No noarch packages (yet)
cp ${PKG_PATH}/*/*.x86_64.rpm $DISTDIR_OS

# Create yum repository from RPM packages
createrepo "${DISTDIR}/repo"

# Package puppet modules + config
cd "${PUPPET_WS}" && tar cfz "${DISTDIR}/puppet.tar.gz" .
