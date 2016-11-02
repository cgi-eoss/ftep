def build() {

    def modulePath = "${WORKSPACE}/distribution/puppet/modules"

    stage('Standalone Distribution') {
        sh """
            createrepo ${env.DISTDIR}/repo
        """

        dir("${modulePath}/ftep") {
            git url: 'https://github.com/cgi-eoss/puppet-ftep', branch: 'master', changelog: false, poll: false
        }
        sh """
            puppet module install --modulepath=${modulePath} puppetlabs-stdlib
            cd tar cfz ${env.DISTDIR}/puppet.tar.gz .
        """
    }
}

return this;

