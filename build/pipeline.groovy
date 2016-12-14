{ ->
    node('docker') {
        deleteDir()
        unstash 'source'

        def buildImg
        stage('Bake build container') {
            ansiColor('xterm') {
                buildImg = docker.build('eossci/f-tep_build', '--build-arg http_proxy --build-arg https_proxy --build-arg no_proxy build/')
            }
        }

        def dockerArgs = "-e http_proxy -e https_proxy -e no_proxy -e HOME=${WORKSPACE}/.home"
        buildImg.inside(dockerArgs) {
            ansiColor('xterm') {
                // Build F-TEP
                stage('Build F-TEP') {
                    configFileProvider([configFile(fileId: '54dc7a4d-fa38-433c-954a-ced9a332f7c9', variable: 'GRADLEINIT')]) {
                        sh "env GRADLE_OPTS='-Dorg.gradle.daemon=false' GRADLE_USER_HOME=${WORKSPACE}/.home/.gradle ./build/ftep.sh"
                    }
                }

                // Build third-party components
                stage('Build ZOO-Project') {
                    sh "./build/zoo-project.sh"
                }

                // Build full standalone distribution (and archive the result)
                stage('Build Distribution') {
                    sh "./build/standalone-dist.sh"
                    archiveArtifacts artifacts: '.dist/**/*', fingerprint: true, allowEmptyArchive: true
                }

                if (!eossCI.isTriggeredByGerrit()) {
                    stage('SonarQube Analysis') {
                        configFileProvider([configFile(fileId: '54dc7a4d-fa38-433c-954a-ced9a332f7c9', variable: 'GRADLEINIT')]) {
                            eossCI.sonarqubeGradle("", "gradle --no-daemon -i ${GRADLEINIT}")
                        }
                    }
                }
            }
        }
    }
}
