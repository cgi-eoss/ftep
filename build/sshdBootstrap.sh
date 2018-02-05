#!/bin/sh
#
# This script is used to update the uid of the ftep user account so the
# mounted volume is writable by the container user.
#
# The uid to be used must be passed as the first argument. The remaining
# arguments are invoked as-is, allowing e.g. sshd to be started.

if [ ! -d /var/run/sshd ]; then
  # Create the temp directory required by sshd (may not be present when not started from an init script)
  mkdir /var/run/sshd && chmod 0755 /var/run/sshd
fi

/usr/sbin/usermod -u $1 ftep
/usr/sbin/groupmod -g $2 ftep
if [ ! $(getent group $3) ]; then
   /usr/sbin/groupadd -g $3 docker
fi
/usr/bin/gpasswd -a ftep $(getent group $3 | awk -F: '{print $1}')

shift 3

$@
