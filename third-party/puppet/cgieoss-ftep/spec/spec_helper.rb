require 'puppetlabs_spec_helper/module_spec_helper'

RSpec.configure do |c|
  c.tty = true
  c.hiera_config = File.join('spec', 'fixtures', 'hiera', 'hiera.yaml')
  c.default_facts = {
      :fqdn     => 'testnode.example.com',
      :kernel   => 'Linux',
      :osfamily => 'RedHat',
      :operatingsystem => 'CentOS',
      :operatingsystemrelease => '6.8',
      :operatingsystemmajrelease => '6',

      :concat_basedir => '/tmp/puppet-concat',

      :root_home => '/root',
      :path => '/usr/local/bin:/usr/bin:/bin',

      # Version facts
      :puppetversion  => '4.7.0',
  }
end

