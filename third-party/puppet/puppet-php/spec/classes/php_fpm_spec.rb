require 'spec_helper'

describe 'php::fpm', type: :class do
  on_supported_os.each do |os, facts|
    context "on #{os}" do
      let :facts do
        facts
      end
      let(:pre_condition) { 'class {"php": fpm => false}' }
      describe 'when called with no parameters' do
        # rubocop:disable RSpec/RepeatedExample
        case facts[:osfamily]
        when 'Debian'
          it { is_expected.to contain_package('php5-fpm').with_ensure('present') }
          it { is_expected.to contain_service('php5-fpm').with_ensure('running') }
        when 'Suse'
          it { is_expected.to contain_package('php5-fpm').with_ensure('present') }
          it { is_expected.to contain_service('php-fpm').with_ensure('running') }
        when 'FreeBSD'
          it { is_expected.not_to contain_package('php56-') }
          it { is_expected.not_to contain_package('php5-fpm') }
          it { is_expected.not_to contain_package('php-fpm') }
          it { is_expected.to contain_service('php-fpm').with_ensure('running') }
        else
          it { is_expected.to contain_package('php-fpm').with_ensure('present') }
          it { is_expected.to contain_service('php-fpm').with_ensure('running') }
        end
        # rubocop:enable RSpec/RepeatedExample
        it { is_expected.to contain_class('php::fpm::config').that_notifies('Class[php::fpm::service]') }
        it { is_expected.to contain_class('php::fpm::service') }
      end
    end
  end
end
