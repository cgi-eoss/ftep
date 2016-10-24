{ ->
    node('docker') {
        deleteDir()
        unstash 'source'

        // prep the distribution assembly directory
        env.DISTDIR = "${WORKSPACE}/.dist"
        sh "mkdir -p ${env.DISTDIR}"

        def buildImg
        stage('Bake build container') {
            buildImg = docker.build('eossci/f-tep_build', '--build-arg http_proxy --build-arg https_proxy build/')
        }

        buildImg.inside {
            def zp = load 'build/zoo-project.pipeline.groovy'
            zp.build()
        }

        archiveArtifacts artifacts: '.dist/**/*', fingerprint: true, allowEmptyArchive: true
    }
}

