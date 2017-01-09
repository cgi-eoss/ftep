#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

GRADLE_ARGS=""

if [ "" != "${GRADLEINIT}" ]; then
  GRADLE_ARGS="${GRADLE_ARGS} -I ${GRADLEINIT}"
fi

gradle $GRADLE_ARGS build --parallel --stacktrace
cp f-tep-processors/target/distributions/*.rpm "${DISTDIR_NOARCH}"
cp f-tep-portal/target/distributions/*.rpm "${DISTDIR_NOARCH}"
cp f-tep-drupalmodules/target/distributions/*.rpm "${DISTDIR_NOARCH}"

mkdir -p distribution/puppet/modules
cp -r third-party/puppet/target/modules/* distribution/puppet/modules/
