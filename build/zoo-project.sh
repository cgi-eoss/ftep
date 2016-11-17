#!/usr/bin/env sh

. $(dirname $(readlink -f "$0"))/env.sh

PKG="zoo-kernel"
V="1.5.0"
I="1"
ARCH="x86_64"

ZOO="${WORKSPACE}/third-party/cxx/zoo-project"
ZOO_WS="${ZOO}/src"
ZOO_PREP="${ZOO_WS}/.prep"

# Build libcgic.a
cd "${ZOO_WS}/thirds/cgic206"
make install

# Build zoo-kernel
mkdir -p "${ZOO_PREP}/usr/lib" "${ZOO_PREP}/var/www/cgi-bin" "${ZOO_PREP}/usr/include"
cd "${ZOO_WS}/zoo-project/zoo-kernel"
autoconf
./configure --prefix=/usr --with-db-backend --with-python --with-java="${JAVA_HOME}" --with-cgi-dir="/var/www/cgi-bin"
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
