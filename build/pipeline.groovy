{ ->
    node('docker') { ansiColor('xterm') {
        deleteDir()
        unstash 'source'

        def buildImg
        stage('Bake build container') {
            buildImg = docker.build('eossci/f-tep_build', '--build-arg http_proxy --build-arg https_proxy --build-arg no_proxy build/')
        }

        def gid = sh script: 'stat -c %g /var/run/docker.sock', returnStdout: true
        def dockerInDockerArgs = "-v /var/run/docker.sock:/var/run/docker.sock:rw --group-add=${gid}"

        def dockerArgs = "${dockerInDockerArgs} -e http_proxy -e https_proxy -e no_proxy -e HOME=${WORKSPACE}/.home"
        buildImg.inside(dockerArgs) {
            withGradleEnv {
                // Build F-TEP
                stage('Build F-TEP') {
                    try {
                        sh "./build/ftep.sh"
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

// TODO Pending fix for testcontainers docker-in-docker functionality
//                stage('Acceptance Test') {
//                    timeout(20) {
//                        try {
//                            sh "gradle -I ${GRADLEINIT} test -pf-tep-test -PacceptanceTests"
//                        } catch (Exception e) {
//                            // Swallow acceptance test failures
//                        } finally {
//                            step($class: 'CucumberTestResultArchiver', testResults: 'f-tep-test/target/test-results/cucumber.json')
//                        }
//                    }
//                }

                if (!eossCI.isTriggeredByGerrit()) {
                    stage('SonarQube Analysis') {
                        eossCI.sonarqubeGradle("", "gradle -I ${GRADLEINIT}")
                    }
                }
            } // end withGradleEnv
        } // end buildImg.inside
    } } // end ansiColor & node
}

def withGradleEnv(Closure buildSteps) {
    configFileProvider([configFile(fileId: '54dc7a4d-fa38-433c-954a-ced9a332f7c9', variable: 'GRADLEINIT')]) {
        withEnv(["GRADLE_OPTS='-Dorg.gradle.daemon=false'", "GRADLE_USER_HOME=${WORKSPACE}/.home/.gradle"]) {
            buildSteps()
        }
    }
}
