#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

PUPPET_WS="${WORKSPACE}/distribution/puppet"
MODULE_PATH="${PUPPET_WS}/modules"
ARMADILLO="${WORKSPACE}/third-party/cxx/armadillo"
PUPPET_FTEP="${WORKSPACE}/third-party/puppet/cgieoss-ftep"

cp ${ARMADILLO}/*.rpm $DISTDIR_OS

# Create yum repository from RPM packages
createrepo "${DISTDIR}/repo"

# Build F-TEP Puppet module
puppet module build $PUPPET_FTEP

# Collect required puppet modules
puppet module install --modulepath="${MODULE_PATH}" ${PUPPET_FTEP}/pkg/cgieoss-ftep-*.tar.gz
cd "${PUPPET_WS}" && tar cfz "${DISTDIR}/puppet.tar.gz" .
