#!/bin/sh

SCRIPT=$(readlink -f "$0")
LOC=$(dirname $SCRIPT)

for module in puppetlabs-stdlib; do
    /opt/puppetlabs/bin/puppet module install --modulepath=$LOC/modules $module
done

# Manually install our own module from github
git clone --branch master https://github.com/cgi-eoss/puppet-ftep $LOC/modules/ftep
