if (!eossCI.isTriggeredByGerrit()) {
    properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '30', daysToKeepStr: '', numToKeepStr: '']]])
}

node {
    dir('.fetch') {
        deleteDir()
        eossCI.checkoutScmOrGerrit()
        stash name: 'source', useDefaultExcludes: false
    }

    load '.fetch/pipeline.groovy'
}()

// vi: syntax=groovy

