#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

GRADLE_ARGS=""

if [ "" != "${GRADLEINIT}" ]; then
  GRADLE_ARGS="${GRADLE_ARGS} -I ${GRADLEINIT}"
fi

gradle $GRADLE_ARGS build --parallel --configure-on-demand
cp f-tep-processors/target/distributions/*.rpm "${DISTDIR_NOARCH}"
cp f-tep-portal/target/distributions/*.rpm "${DISTDIR_NOARCH}"
