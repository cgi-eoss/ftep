# Make application jar mutable so it can be removed
chattr -i /var/f-tep/server/f-tep-server.jar &>/dev/null || true
