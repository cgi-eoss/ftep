FROM centos:6

RUN sed -i 's/enabled=.*$/enabled=0/' /etc/yum/pluginconf.d/fastestmirror.conf

# Install all required repos
RUN yum install -y \
 https://yum.puppetlabs.com/puppetlabs-release-pc1-el-6.noarch.rpm \
 http://elgis.argeo.org/repos/6/elgis-release-6-6_0.noarch.rpm \
 https://download.postgresql.org/pub/repos/yum/9.5/redhat/rhel-6-x86_64/pgdg-centos95-9.5-3.noarch.rpm \
 https://dl.fedoraproject.org/pub/epel/epel-release-latest-6.noarch.rpm \
 https://mirror.webtatic.com/yum/el6/latest.rpm \
 && mv /etc/yum.repos.d/pgdg-95-centos.repo /etc/yum.repos.d/yum.postgresql.org.repo \
 && yum clean all

# Install puppet
RUN yum install -y \
 puppet-agent \
 && yum clean all
ENV PATH=/opt/puppetlabs/bin:${PATH}

ADD entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT /entrypoint.sh

# Speed up provisioning by pre-installing appropriate packages for each node; this should change infrequently and can be
# cached effectively, whereas the F-TEP stuff will change frequently
ARG pkgconf
ADD ${pkgconf} /var/tmp/pkglist
RUN yum install -y $(cat /var/tmp/pkglist) && yum clean all

# F-TEP installation/configuration
ADD puppet /var/tmp/puppet
ADD common.yaml /var/tmp/puppet/hieradata/standalone.yaml
ARG puppetconf
ADD ${puppetconf} /var/tmp/puppet/hieradata/standalone.local.yaml

# This will change on every build, so do it as late as possible
ADD repo /var/tmp/repo

RUN cd /var/tmp/puppet \
 && puppet apply \
 --hiera_config=/var/tmp/puppet/hiera.yaml \
 --environmentpath=/var/tmp \
 --environment=puppet \
 /var/tmp/puppet/manifest.pp
