{ ->
    node('docker') {
        deleteDir()
        unstash 'source'
    }
}

