{ ->
    node('docker') {
        deleteDir()
        unstash 'source'

        // Prep the distribution assembly directory
        env.DISTDIR = "${WORKSPACE}/.dist"
        sh "mkdir -p ${env.DISTDIR}"

        def buildImg
        stage('Bake build container') {
            buildImg = docker.build('eossci/f-tep_build', '--build-arg http_proxy --build-arg https_proxy build/')
        }

        buildImg.inside {
            // Build F-TEP
            configFileProvider([configFile(fileId: '2c353f8f-d8c7-41e8-b26f-8f76dfa4a000', variable: 'M2SETTINGS')]) {
                def m2args = "-B -s ${M2SETTINGS} -Dmaven.repo.local=${WORKSPACE}/.repository"
                sh "mvn ${m2args} clean install"
            }

            // Build third-party components
            def zp = load 'build/zoo-project.pipeline.groovy'
            zp.build()
        }

        archiveArtifacts artifacts: '.dist/**/*', fingerprint: true, allowEmptyArchive: true
    }
}

