{ ->
    node('docker') {
        deleteDir()
        unstash 'source'

        // Prep the distribution assembly directories
        env.DISTDIR = "${WORKSPACE}/.dist"
        env.DISTDIR_NOARCH = "${env.DISTDIR}/repo/6/local/noarch/RPMS"
        env.DISTDIR_OS = "${env.DISTDIR}/repo/6/local/x86_64/RPMS"
        sh "mkdir -p ${env.DISTDIR_NOARCH} ${env.DISTDIR_OS}"

        def buildImg
        stage('Bake build container') {
            buildImg = docker.build('eossci/f-tep_build', '--build-arg http_proxy --build-arg https_proxy build/')
        }

        def dockerArgs = "-v /etc/pki/tls/certs:/etc/pki/tls/certs -e http_proxy -e https_proxy -e HOME=${WORKSPACE}/.home"
        buildImg.inside(dockerArgs) {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                // Build F-TEP
                stage('Backend') {
                    configFileProvider([configFile(fileId: '2c353f8f-d8c7-41e8-b26f-8f76dfa4a000', variable: 'M2SETTINGS')]) {
                        def m2args = "-B -s ${M2SETTINGS} -Dmaven.repo.local=${WORKSPACE}/.repository"
                        sh """
                        mvn ${m2args} clean install
                        cp ftep-config/target/rpm/f-tep-processors/RPMS/noarch/*.rpm ${env.DISTDIR_NOARCH}/
                    """
                    }
                }

                // Build third-party components
                load('build/zoo-project.pipeline.groovy').build()

                // Build full standalone distribution (and archive the result)
                load('build/standalone-dist.pipeline.groovy').build()
            }
        }
    }
}
