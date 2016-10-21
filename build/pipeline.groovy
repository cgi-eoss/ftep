{ ->
    node('docker') {
        deleteDir()
        unstash 'source'

        def buildImg
        stage('Bake build container') {
            buildImg = docker.build('eossci/f-tep_build', '--build-arg http_proxy --build-arg https_proxy build/')
        }

    }
}

