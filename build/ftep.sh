#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

GRADLE_ARGS=""

if [ "" != "${GRADLEINIT}" ]; then
  GRADLE_ARGS="${GRADLE_ARGS} -I ${GRADLEINIT}"
fi

gradle $GRADLE_ARGS build --parallel
cp $(ls -dtr1 f-tep-processors/target/distributions/*.rpm | tail -1) "${DISTDIR_NOARCH}"
cp $(ls -dtr1 f-tep-portal/target/distributions/*.rpm | tail -1) "${DISTDIR_NOARCH}"
cp $(ls -dtr1 f-tep-drupalmodules/target/distributions/*.rpm | tail -1) "${DISTDIR_NOARCH}"
cp $(ls -dtr1 f-tep-server/target/distributions/*.rpm | tail -1) "${DISTDIR_NOARCH}"

mkdir -p distribution/puppet/modules
cp -r third-party/puppet/target/modules/* distribution/puppet/modules/
