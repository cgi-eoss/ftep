Puppet::Type.newtype(:elasticsearch_user) do
  desc "Type to model Elasticsearch users."

  feature :manages_encrypted_passwords,
    'The provider can control the password hash without a need
    to explicitly refresh.'

  feature :manages_plaintext_passwords,
    'The provider can control the password in plaintext form.'

  ensurable do
    defaultvalues
    defaultto :present
  end

  newparam(:name, :namevar => true) do
    desc 'User name.'
  end

  newparam(
    :password,
    :required_features => :manages_plaintext_passwords
  ) do
    desc 'Plaintext password for user.'

    validate do |value|
      if value.length < 6
        raise ArgumentError, 'Password must be at least 6 characters long'
      end
    end

    def is_to_s currentvalue
      return '[old password hash redacted]'
    end
    def should_to_s newvalue
      return '[new password hash redacted]'
    end
  end

  newproperty(
    :hashed_password,
    :required_features => :manages_encrypted_passwords
  ) do
    desc 'Hashed password for user.'

    newvalues(/^[$]2a[$].{56}$/)
  end

  def refresh
    if @parameters[:ensure]
      provider.passwd
    else
      debug 'skipping password set'
    end
  end
end
