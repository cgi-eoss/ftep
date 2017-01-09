require 'spec_helper'

#Puppet::Util::Log.level = :debug
#Puppet::Util::Log.newdestination(:console)

describe 'ftep::portal', :type => 'class' do
  it { should compile }
  it { should contain_class('ftep::portal') }
end
