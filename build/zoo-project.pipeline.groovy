def build() {
    def pkg = "zoo-kernel"
    def v = "1.5.0"
    def i = "1"
    def arch = "x86_64"

    def path = "${WORKSPACE}/third-party/cots/zoo-project"
    def ws = "${path}/zoo-project-${v}"
    def prep = "${ws}/.prep"

    def outDir = "${env.DISTDIR}/zoo-project"

    stage('ZOO-Project') {
        sh "cd ${path} && tar xf zoo-project-${v}.tar.bz2"

        // Build libcgic.a
        sh """
            cd ${ws}/thirds/cgic206
            make
            make install
        """

        // Build zoo-kernel
        sh """
            mkdir -p ${prep}/usr/{lib,lib/cgi-bin,include}
            cd ${ws}/zoo-project/zoo-kernel
            autoconf
            ./configure --prefix=/usr --with-java=\$JAVA_HOME --with-cgi-dir=${prep}/usr/lib/cgi-bin
            make
            make install DESTDIR=${prep}
        """

        // Package zoo-kernel RPM
        sh """
            mkdir -p ${outDir}
            fpm -t rpm -p ${outDir}/NAME-VERSION-ITERATION.ARCH.rpm -s dir \
             -f -n ${pkg} -v ${v} --iteration ${i} --category Applications/TEP \
             --description "ZOO-Kernel WPS server" --url "http://zoo-project.org/" --license "MIT License" --vendor "F-TEP" \
             --verbose \
             -C ${prep} .
        """
    }
}

return this;

