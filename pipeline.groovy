{ ->
    node('eossci_master') {
        dir('/opt/eossci/f-tep') {
           stash name: 'm2settings', includes: 'm2settings.xml'
        }
    }

    node('docker') {
        stage 'Docker build'
        unstash 'source'
        def buildImg = docker.build('eossci/build:f-tep', 'src/main/docker/build/')

        buildImg.inside {
            stage 'Clean local repo'
            sh """
                git clean -fdxq -e .repository
                rm -rf .repository/com/cgi/eoss/f-tep
            """

            stage 'Maven build'
        	unstash 'm2settings'
            def mvn = "mvn -B -s ${pwd()}/m2settings.xml -Dmaven.repo.local=${pwd()}/.repository"
            sh "${mvn} clean install"
        }
    }
}

// vi: syntax=groovy
