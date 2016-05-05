node {
    stage 'SCM'
    checkout scm

    // Check out a gerrit patchset, if this run was started by Gerrit Trigger
    if (getBinding().hasVariable('GERRIT_REFSPEC')) {
        checkout changelog: false, poll: false, scm: [
            $class: 'GitSCM',
            branches: [[name: GERRIT_BRANCH]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [[
                $class: 'BuildChooserSetting',
                buildChooser: [
                    $class: 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTriggerBuildChooser'
                    ]
                ]],
            submoduleCfg: [],
            userRemoteConfigs: [[
                credentialsId: 'eada7104-c4db-4397-9255-1eabd318facf',
                refspec: GERRIT_REFSPEC,
                url: "ssh://${GERRIT_HOST}:29418/${GERRIT_PROJECT}"
                ]]
        ]
    }

    stash name: 'source', excludes: '.repository/**'
    load 'pipeline.groovy'
}()

// vi: syntax=groovy

