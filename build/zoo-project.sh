#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

PKG="zoo-kernel"
V="1.5.0"
I="1"
ARCH="x86_64"

ZOO="${WORKSPACE}/third-party/cots/zoo-project"
ZOO_WS="${ZOO}/zoo-project-trunk"
ZOO_PREP="${ZOO_WS}/.prep"

# Build ZOO-Project from trunk revision (using github mirror of svn trunk)
ZOO_REV="437ba4c4891244b14e89d6346a3fbd8d90830186"
rm -rf "${ZOO_WS}" && mkdir -p "${ZOO_WS}" && cd "${ZOO_WS}" &&\
 curl -sL https://github.com/OSGeo/zoo-project/archive/${ZOO_REV}.tar.gz | tar xz --strip-components=1

# Build libcgic.a
cd "${ZOO_WS}/thirds/cgic206"
make install

# Build zoo-kernel
mkdir -p "${ZOO_PREP}/usr/lib" "${ZOO_PREP}/var/www/cgi-bin" "${ZOO_PREP}/usr/include"
cd "${ZOO_WS}/zoo-project/zoo-kernel"
autoconf
./configure --prefix=/usr --with-java="${JAVA_HOME}" --with-cgi-dir="/var/www/cgi-bin"
make
make install DESTDIR="${ZOO_PREP}"

# Package zoo-kernel RPM
fpm -t rpm -p "${DISTDIR_OS}/NAME-VERSION-ITERATION.ARCH.rpm" -s dir \
 -f -n "${PKG}" -v "${V}" --iteration "${I}" --category Applications/TEP \
 --description "ZOO-Kernel WPS server" --url "http://zoo-project.org/" --license "MIT License" --vendor "F-TEP" \
 -d "java-1.8.0-openjdk-headless" \
 -d "gdal" \
 -d "fcgi" \
 --after-install "${ZOO}/postinst.sh" \
 --verbose \
 -C "${ZOO_PREP}" .
