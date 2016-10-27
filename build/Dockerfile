FROM centos:6.8

RUN sed -i 's/enabled=.*$/enabled=0/' /etc/yum/pluginconf.d/fastestmirror.conf

# Core dependencies and tools
RUN yum install -y \
 autoconf \
 createrepo \
 epel-release \
 git \
 redhat-rpm-config \
 rpm-build \
 ruby \
 ruby-devel \
 rubygems \
 tar \
 unzip \
 wget \
 zip \
 && yum clean all

# devtoolset-2
RUN curl -sL http://people.centos.org/tru/devtools-2/devtools-2.repo > /etc/yum.repos.d/devtools-2.repo
RUN yum install -y \
 devtoolset-2-binutils \
 devtoolset-2-gcc \
 devtoolset-2-gcc-c++ \
 devtoolset-2-libstdc++-devel \
 devtoolset-2-runtime.noarch \
 && yum clean all
ENV DEVTOOLSET_PATH /opt/rh/devtoolset-2

# Add tools to PATH and set env vars
ENV PATH=${DEVTOOLSET_PATH}/root/usr/bin:${PATH} \
 LD_LIBRARY_PATH=${DEVTOOLSET_PATH}/root/usr/lib64:${DEVTOOLSET_PATH}/root/usr/lib \
 PERL5LIB=${DEVTOOLSET_PATH}/root//usr/lib64/perl5/vendor_perl:${DEVTOOLSET_PATH}/root/usr/lib/perl5:${DEVTOOLSET_PATH}/root//usr/share/perl5/vendor_perl \
 PYTHONPATH=${DEVTOOLSET_PATH}/root/usr/lib64/python2.6/site-packages:${DEVTOOLSET_PATH}/root/usr/lib/python2.6/site-packages

# JDK
RUN yum install -y \
 java-1.8.0-openjdk \
 java-1.8.0-openjdk-devel \
 && yum clean all
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0

# Maven
ENV MAVEN_VERSION 3.3.9
RUN cd /opt && curl -s -L https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xz
ENV M2_HOME /opt/apache-maven-${MAVEN_VERSION}
ENV PATH=${M2_HOME}/bin:${PATH}

# ZOO-Project dependencies (order of installation is important)
RUN yum install -y \
 http://elgis.argeo.org/repos/6/elgis-release-6-6_0.noarch.rpm \
 && yum clean all
RUN yum install -y \
 http://proj.badc.rl.ac.uk/cedaservices/raw-attachment/ticket/670/armadillo-3.800.2-1.el6.x86_64.rpm \
 https://kojipkgs.fedoraproject.org/packages/CharLS/1.0/1.el6/x86_64/CharLS-1.0-1.el6.x86_64.rpm \
 https://kojipkgs.fedoraproject.org/packages/CharLS/1.0/1.el6/x86_64/CharLS-devel-1.0-1.el6.x86_64.rpm \
 && yum clean all
RUN yum install -y \
 bison \
 curl-devel \
 fcgi-devel \
 flex \
 flex-devel \
 gdal-devel \
 geos-devel \
 libuuid-devel \
 libxml2-devel \
 libxslt-devel \
 openssl-devel \
 proj-devel \
 subversion \
 && yum clean all

# Install puppet for preparing distribution module
RUN yum install -y https://yum.puppetlabs.com/puppetlabs-release-pc1-el-6.noarch.rpm
RUN yum install -y \
 puppet-agent \
 && yum clean all
ENV PATH=/opt/puppetlabs/bin:${PATH}

# Re-use the puppet-provided ruby for other gems
RUN /opt/puppetlabs/puppet/bin/gem install -n /usr/local/bin fpm

