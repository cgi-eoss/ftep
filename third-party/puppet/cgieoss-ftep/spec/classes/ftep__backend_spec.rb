require 'spec_helper'

#Puppet::Util::Log.level = :debug
#Puppet::Util::Log.newdestination(:console)

describe 'ftep::backend', :type => 'class' do
  it { should compile }
  it { should contain_class('ftep::backend') }
  it { should contain_class('ftep::backend::java') }
  it { should contain_class('ftep::backend::zoo_kernel') }

  # Java config
  it { should contain_file('/etc/ld.so.conf.d/java.conf') }
  it { should contain_exec('java_ldconfig').with_command('/sbin/ldconfig') }

  # Third-party yum repos for dependencies
  it { should contain_class('epel') }
  it { should contain_yumrepo('elgis').with_baseurl('http://elgis.argeo.org/repos/6/elgis/$basearch') }

  it { should contain_package('zoo-kernel').with_name('zoo-kernel') }
  it { should contain_file('/usr/lib/cgi-bin/main.cfg')
                  .with_content(/^serverAddress = https:\/\/forestry-tep.eo.esa.int\/zoo$/)
                  .with_content(/^dataPath = \/var\/www\/temp/)
  }
end
