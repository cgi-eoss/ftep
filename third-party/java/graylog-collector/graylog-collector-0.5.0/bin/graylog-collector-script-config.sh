#!/bin/bash

COLLECTOR_JAVA_DEFAULT_OPTS="-Xms12m -Xmx64m -Djava.library.path=/usr/share/graylog-collector/lib/sigar"
COLLECTOR_DEFAULT_JAR="/usr/share/graylog-collector/graylog-collector.jar"

# For Debian/Ubuntu based systems.
if [ -f "/etc/default/graylog-collector" ]; then
    . "/etc/default/graylog-collector"
fi

# For RedHat/Fedora based systems.
if [ -f "/etc/sysconfig/graylog-collector" ]; then
    . "/etc/sysconfig/graylog-collector"
fi
