require 'spec_helper'

describe 'php::fpm::pool' do
  on_supported_os.each do |os, facts|
    context "on #{os}" do
      let :facts do
        facts
      end
      let(:pre_condition) { 'include php' }
      case facts[:osfamily]
      when 'Debian'
        case facts[:operatingsystem]
        when 'Ubuntu'
          context 'plain config' do
            let(:title) { 'unique-name' }
            let(:params) { {} }

            it { is_expected.to contain_file('/etc/php5/fpm/pool.d/unique-name.conf') }
          end
        when 'Debian'
          context 'plain config' do
            let(:title) { 'unique-name' }
            let(:params) { {} }

            it { is_expected.to contain_file('/etc/php5/fpm/pool.d/unique-name.conf') }
          end
        end
      end
    end
  end
end
