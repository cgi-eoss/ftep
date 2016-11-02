def build() {

    def puppetPath = "${WORKSPACE}/distribution/puppet"
    def modulePath = "${puppetPath}/modules"

    stage('Standalone Distribution') {
        sh """
            createrepo ${env.DISTDIR}/repo
        """

        dir("${modulePath}/ftep") {
            git url: 'https://github.com/cgi-eoss/puppet-ftep', branch: 'master', changelog: false, poll: false
        }
        sh """
            puppet module install --modulepath=${modulePath} puppetlabs-stdlib
            cd ${puppetPath} && tar cfz ${env.DISTDIR}/puppet.tar.gz .
        """
    }
}

return this;

