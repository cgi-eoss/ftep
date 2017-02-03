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
