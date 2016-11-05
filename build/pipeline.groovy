{ ->
    node('docker') {
        deleteDir()
        unstash 'source'

        def buildImg
        stage('Bake build container') {
            buildImg = docker.build('eossci/f-tep_build', '--build-arg http_proxy --build-arg https_proxy build/')
        }

        def dockerArgs = "-e http_proxy -e https_proxy -e HOME=${WORKSPACE}/.home"
        buildImg.inside(dockerArgs) {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                // Build F-TEP
                stage('Backend') {
                    configFileProvider([configFile(fileId: '2c353f8f-d8c7-41e8-b26f-8f76dfa4a000', variable: 'M2SETTINGS')]) {
                        sh "./build/ftep.sh"
                    }
                }

                // Build third-party components
                stage('ZOO-Project') {
                    sh "./build/zoo-project.sh"
                }

                // Build full standalone distribution (and archive the result)
                stage('Standalone Distribution') {
                    sh "./build/standalone-dist.sh"
                    archiveArtifacts artifacts: '.dist/**/*', fingerprint: true, allowEmptyArchive: true
                }
            }
        }
    }
}
