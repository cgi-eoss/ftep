#!/usr/bin/env sh

WORKSPACE=$(dirname $(readlink -f "$0"))

ZOO="${WORKSPACE}"
ZOO_WS="${ZOO}/src"
ZOO_DESTDIR="${ZOO}/target/staging"
ZOO_PKGDIR="${ZOO}/target/distributions"

# Build libcgic.a
cd "${ZOO_WS}/thirds/cgic206"
make install

# Build zoo-kernel
mkdir -p "${ZOO_DESTDIR}/usr/lib" "${ZOO_DESTDIR}/var/www/cgi-bin" "${ZOO_DESTDIR}/usr/include"
cd "${ZOO_WS}/zoo-project/zoo-kernel"
autoconf
./configure --prefix=/usr --with-db-backend --with-python --with-java="${JAVA_HOME}" --with-cgi-dir="/var/www/cgi-bin"
make
make install DESTDIR="${ZOO_DESTDIR}"

# Build libZOO.so for JNI, and add to the cgi-bin path
cd "${ZOO_WS}/zoo-project/zoo-api/java"
env LIBRARY_PATH="${ZOO_DESTDIR}/usr/lib:${LIBRARY_PATH}" make
cp "${ZOO_WS}/zoo-project/zoo-api/java/libZOO.so" "${ZOO_DESTDIR}/var/www/cgi-bin/"
