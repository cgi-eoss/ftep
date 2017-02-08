# check that owner group exists
if ! getent group ftep &>/dev/null ; then
  groupadd ftep
fi

# check that user exists
if ! getent passwd ftep &>/dev/null ; then
  useradd --system --gid ftep ftep
fi

# (optional) check that user belongs to group
if ! id -G -n ftep | grep -qF ftep ; then
  usermod -a -G ftep ftep
fi

# Make application binary mutable if it already exists (i.e. this is a package upgrade)
if test -f /var/f-tep/bin/f-tep-server.jar ; then
    chattr -i /var/f-tep/bin/f-tep-server.jar
fi
