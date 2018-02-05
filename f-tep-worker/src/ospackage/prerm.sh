# Make application jar mutable so it can be removed
chattr -i /var/f-tep/worker/f-tep-worker.jar &>/dev/null || true
