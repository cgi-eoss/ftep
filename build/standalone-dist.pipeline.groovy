def build() {

    stage('Standalone Distribution') {
        sh """
            createrepo ${env.DISTDIR}/repo
            cd ${WORKSPACE}/distribution/puppet
            ./install_modules.sh
            cd tar cfz ${env.DISTDIR}/puppet.tar.gz .
        """
    }
}

return this;

