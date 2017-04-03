# -*- mode: ruby -*-
# vi: set ft=ruby :

# This Vagrantfile may require the following vagrant plugins:
# * vagrant-vbguest (for shared folders in the centos/6 base image)
# * vagrant-puppet-install (for the Puppet provisioner)
#
# They may be installed with "vagrant plugin install <plugin>"

Vagrant.configure('2') do |config|

  config.vm.define 'build', primary: false, autostart: false do |build|
    build.ssh.username = 'ftep'
    build.ssh.password = 'ftep'
    build.vm.synced_folder '.', '/home/ftep/build'
    build.vm.synced_folder '/home/vanzettenp/.gradle', '/home/ftep/.gradle'

    build.vm.provider 'docker' do |d|
      d.build_dir = './build'
      d.build_args = ['--build-arg=http_proxy', '--build-arg=https_proxy', '--build-arg=no_proxy']
      # Change the internal 'ftep' uid to the current user's uid, and launch sshd
      d.cmd = ['/usr/sbin/sshdBootstrap.sh', `id -u`.chomp, `id -g`.chomp, '/usr/sbin/sshd', '-D', '-e']
      d.has_ssh = true
    end
  end

  # The default box is an integration testing environment, installing the
  # distribution and configuring with the Puppet manifest.
  config.vm.define 'ftep', primary: true do |ftep|
    ftep.vm.box = 'centos/6'

    # Expose the container's web server on 8080
    ftep.vm.network 'forwarded_port', guest: 80, host: 8080 # apache
    ftep.vm.network 'forwarded_port', guest: 6565, host: 6565 # f-tep-server grpc
    ftep.vm.network 'forwarded_port', guest: 8761, host: 8761 # f-tep-serviceregistry http
    ftep.vm.network 'forwarded_port', guest: 5140, host: 5140 # graylog gelf tcp

    # Create a private network, which allows host-only access to the machine
    # using a specific IP.
    # config.vm.network "private_network", ip: "192.168.33.10"

    # Create a public network, which generally matched to bridged network.
    # Bridged networks make the machine appear as another physical device on
    # your network.
    # config.vm.network "public_network"

    # Ensure the virtualbox provider uses shared folders and not rsynced
    # folders (which may be confused by symlinks)
    ftep.vm.provider 'virtualbox' do |vb|
      ftep.vm.synced_folder '.', '/vagrant', type: 'virtualbox'
      vb.memory = 2048
      vb.cpus = 2
    end

    # Puppet provisioning
    #
    # Configure the local environment by editing distribution/puppet/hieradata/standalone.local.yaml
    # For example:
    # ---
    # classes:
    #   - ftep::backend
    # ftep::repo::location: 'file:///vagrant/.dist/repo'
    #
    config.puppet_install.puppet_version = '4.9.3'
    ftep.vm.provision 'puppet' do |puppet|
      puppet.environment_path = '.dist'
      puppet.environment = 'puppet'
      puppet.hiera_config_path = '.dist/puppet/hiera-global.yaml'
      puppet.working_directory = '/tmp/vagrant-puppet/environments/puppet'
      #puppet.options = "--debug"
    end
  end
end
