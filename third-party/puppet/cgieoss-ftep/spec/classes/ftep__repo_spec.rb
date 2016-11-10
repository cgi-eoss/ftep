require 'spec_helper'

#Puppet::Util::Log.level = :debug
#Puppet::Util::Log.newdestination(:console)

describe 'ftep::repo', :type => 'class' do
  it { should compile }
  it { should contain_class('ftep::repo') }
  it { should contain_class('ftep::repo::yum') }
  it { should contain_yumrepo('ftep').with_baseurl('file:///path/to/repo') }
end
