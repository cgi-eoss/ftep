FROM centos:6

RUN sed -i 's/enabled=.*$/enabled=0/' /etc/yum/pluginconf.d/fastestmirror.conf

# Core dependencies and tools
RUN yum install -y \
 autoconf \
 createrepo \
 epel-release \
 git \
 openssh-server \
 redhat-rpm-config \
 rpm-build \
 ruby \
 ruby-devel \
 rubygems \
 sudo \
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

# ZOO-Project dependencies (order of installation is important)
RUN yum install -y \
 http://elgis.argeo.org/repos/6/elgis-release-6-6_0.noarch.rpm \
 && yum clean all
RUN yum install -y \
 http://proj.badc.rl.ac.uk/cedaservices/raw-attachment/ticket/670/armadillo-3.800.2-1.el6.x86_64.rpm \
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
 python-devel \
 subversion \
 && yum clean all

# Install puppet for testing distribution module
RUN yum install -y \
 https://yum.puppetlabs.com/puppetlabs-release-pc1-el-6.noarch.rpm \
 && yum clean all
RUN yum install -y \
 puppet-agent \
 && yum clean all
ENV PATH=/opt/puppetlabs/bin:${PATH}

# Gradle
ENV GRADLE_VERSION 4.2.1
RUN cd /opt && curl -sL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip >/tmp/gradle.zip && unzip -q /tmp/gradle.zip
ENV GRADLE_HOME /opt/gradle-${GRADLE_VERSION}
ENV PATH=${GRADLE_HOME}/bin:${PATH}

# Set up user and environment for interactive usage
RUN adduser -d /home/ftep -m ftep && echo "ftep" | passwd ftep --stdin
RUN ssh-keygen -t rsa -b 4096 -P "" -f /etc/ssh/ssh_host_rsa_key
RUN echo -e "export PATH=$PATH\nexport LD_LIBRARY_PATH=$LD_LIBRARY_PATH\nexport PERL5LIB=$PERL5LIB\nexport PYTHONPATH=$PYTHONPATH\nexport M2_HOME=$M2_HOME\nexport JAVA_HOME=$JAVA_HOME" >>/etc/profile.d/dockerenv.sh
RUN echo -e "ftep ALL=(ALL) NOPASSWD: ALL" >>/etc/sudoers.d/01_ftep
# Bypass devtoolset-2 sudo
RUN rm -f /opt/rh/devtoolset-2/root/usr/bin/sudo
# The script allows the uid of the ftep user to be modified prior to launching sshd
ADD sshdBootstrap.sh /usr/sbin/sshdBootstrap.sh
