#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

M2_ARGS="-B"

if [ "" != "${M2SETTINGS}" ]; then
  M2_ARGS="${M2_ARGS} -s ${M2SETTINGS}"
fi

# Use a workspace-local maven repo unless explicitly disabled
if [ "" = "${NO_LOCAL_M2_REPO}" ]; then
    M2_ARGS="${M2_ARGS} -Dmaven.repo.local=${WORKSPACE}/.repository"
fi

mvn $M2_ARGS clean install
cp ftep-config/target/rpm/f-tep-processors/RPMS/noarch/*.rpm "${DISTDIR_NOARCH}"
cp ftep-portal/target/rpm/f-tep-portal/RPMS/noarch/*.rpm "${DISTDIR_NOARCH}"
