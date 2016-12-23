#!/bin/sh
#
# This script is used to update the uid of the ftep user account so the
# mounted volume is writable by the container user.
#
# The uid to be used must be passed as the first argument. The remaining
# arguments are invoked as-is, allowing e.g. sshd to be started.

/usr/sbin/usermod -u $1 ftep
/usr/sbin/groupmod -g $2 ftep
shift 2

$@
