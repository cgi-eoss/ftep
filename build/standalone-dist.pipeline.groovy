def build() {

    def puppetPath = "${WORKSPACE}/distribution/puppet"
    def modulePath = "${puppetPath}/modules"

    stage('Standalone Distribution') {
        // Download missing third-party packages for our local repo
        sh """
            cd ${env.DISTDIR_OS}
            wget http://proj.badc.rl.ac.uk/cedaservices/raw-attachment/ticket/670/armadillo-3.800.2-1.el6.x86_64.rpm
        """

        // Create yum repository from RPM packages
        sh "createrepo ${env.DISTDIR}/repo"

        // Build F-TEP Puppet module
        def puppetFtep = "${WORKSPACE}/third-party/cots/puppet-ftep"
        dir(puppetFtep) {
            git url: 'https://github.com/cgi-eoss/puppet-ftep', branch: 'master', changelog: false, poll: false
            sh "cd ${puppetFtep} && puppet module build"
        }

        // Collect required puppet modules
        sh """
            puppet module install --modulepath=${modulePath} ${puppetFtep}/pkg/cgieoss-ftep-*.tar.gz
            cd ${puppetPath} && tar cfz ${env.DISTDIR}/puppet.tar.gz .
        """
    }

    archiveArtifacts artifacts: '.dist/**/*', fingerprint: true, allowEmptyArchive: true
}

return this;

