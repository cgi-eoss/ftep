def build() {

    def puppetPath = "${WORKSPACE}/distribution/puppet"
    def modulePath = "${puppetPath}/modules"

    stage('Standalone Distribution') {
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
            puppet module install --modulepath=${modulePath} ${puppetFtep}/pkg/puppet-ftep.tar.gz
            cd ${puppetPath} && tar cfz ${env.DISTDIR}/puppet.tar.gz .
        """
    }

    archiveArtifacts artifacts: '.dist/**/*', fingerprint: true, allowEmptyArchive: true
}

return this;

