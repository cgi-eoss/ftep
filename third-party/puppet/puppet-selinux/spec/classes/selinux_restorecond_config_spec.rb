require 'spec_helper'

describe 'selinux::restorecond' do
  on_supported_os.each do |os, facts|
    context "on #{os}" do
      let(:facts) do
        facts
      end

      it { is_expected.to contain_concat('/etc/selinux/restorecond.conf') }
      it { is_expected.to contain_concat__fragment('restorecond_config_default') }
    end
  end
end
