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
                def gradle = "gradle -I ${GRADLEINIT}"

                // Build F-TEP
                stage('Build F-TEP') {
                    try {
                        sh "${gradle} build --parallel"
                    } finally {
                        junit allowEmptyResults: true, testResults: '**/target/test-results/test/TEST-*.xml'
                    }
                }

                // Assemble full standalone distribution (and archive the result)
                stage('Build Distribution') {
                    sh "${gradle} buildDist -pdistribution --parallel"
                    archiveArtifacts artifacts: '.dist/**/*', fingerprint: true, allowEmptyArchive: true
                }

                stage('Acceptance Test') {
                    timeout(20) {
                        try {
                            sh "${gradle} test -pf-tep-test -PacceptanceTests"
                        } catch (Exception e) {
                            // Swallow acceptance test failures
                        } finally {
                            step($class: 'CucumberTestResultArchiver', testResults: 'f-tep-test/target/test-results/cucumber.json')
                        }
                    }
                }

                if (!eossCI.isTriggeredByGerrit()) {
                    stage('SonarQube Analysis') {
                        eossCI.sonarqubeGradle("", gradle)
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
