#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

PUPPET_WS="${WORKSPACE}/distribution/puppet"
MODULE_PATH="${PUPPET_WS}/modules"
PUPPET_FTEP="${WORKSPACE}/third-party/cots/puppet-ftep"

# Download missing third-party packages for our local repo
wget -O "${DISTDIR_OS}/armadillo-3.800.2-1.el6.x86_64.rpm" http://proj.badc.rl.ac.uk/cedaservices/raw-attachment/ticket/670/armadillo-3.800.2-1.el6.x86_64.rpm

# Create yum repository from RPM packages
createrepo "${DISTDIR}/repo"

# Build F-TEP Puppet module from github revision
# TODO Use a tag when stable, or install from forge
PUPPET_FTEP_REV="540913d98758c60b1f1cdae71801b30e1a01d618"
rm -rf "${PUPPET_FTEP}" && mkdir -p "${PUPPET_FTEP}" && cd "${PUPPET_FTEP}" &&\
 curl -sL https://github.com/cgi-eoss/puppet-ftep/archive/${PUPPET_FTEP_REV}.tar.gz | tar xz --strip-components=1
puppet module build $PUPPET_FTEP

# Collect required puppet modules
puppet module install --modulepath="${MODULE_PATH}" ${PUPPET_FTEP}/pkg/cgieoss-ftep-*.tar.gz
cd "${PUPPET_WS}" && tar cfz "${DISTDIR}/puppet.tar.gz" .
