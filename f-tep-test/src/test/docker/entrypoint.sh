#!/bin/sh

# Execute puppet apply to start any services, then block with cat
cd /var/tmp/puppet \
 && /opt/puppetlabs/bin/puppet apply \
 --hiera_config=/var/tmp/puppet/hiera-global.yaml \
 --environmentpath=/var/tmp \
 --environment=puppet \
 /var/tmp/puppet/manifest.pp \
 && while :; do sleep 100; done