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

        def gid = sh script: 'stat -c %g /var/run/docker.sock', returnStdout: true
        def dockerInDockerArgs = "-v /var/run/docker.sock:/var/run/docker.sock:rw --group-add=${gid}"

        def dockerArgs = "${dockerInDockerArgs} -e http_proxy -e https_proxy -e no_proxy -e HOME=${WORKSPACE}/.home"
        buildImg.inside(dockerArgs) {
            ansiColor('xterm') {
                // Build F-TEP
                stage('Build F-TEP') {
                    try {
                        configFileProvider([configFile(fileId: '54dc7a4d-fa38-433c-954a-ced9a332f7c9', variable: 'GRADLEINIT')]) {
                            sh "env GRADLE_OPTS='-Dorg.gradle.daemon=false' GRADLE_USER_HOME=${WORKSPACE}/.home/.gradle ./build/ftep.sh"
                        }
                    } finally {
                        junit allowEmptyResults: true, testResults: '**/target/test-results/test/TEST-*.xml'
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
                    configFileProvider([configFile(fileId: '54dc7a4d-fa38-433c-954a-ced9a332f7c9', variable: 'GRADLEINIT')]) {
                        stage('SonarQube Analysis') {
                            eossCI.sonarqubeGradle("", "gradle --no-daemon -I ${GRADLEINIT}")
                        }

                        stage('Acceptance Test') {
                            sh "gradle --no-daemon -I ${GRADLEINIT} "
                        }
                    }
                }
            }
        }
    }
}
