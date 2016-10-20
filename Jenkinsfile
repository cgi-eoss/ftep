node {
    dir('.fetch') {
        deleteDir()
        eossCI.checkoutScmOrGerrit()
        stash name: 'source', useDefaultExcludes: false
    }

    load '.fetch/build/pipeline.groovy'
}()

// vi: syntax=groovy

