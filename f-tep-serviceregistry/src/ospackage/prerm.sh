# Make application jar mutable so it can be removed
chattr -i /var/f-tep/serviceregistry/f-tep-serviceregistry.jar &>/dev/null || true
