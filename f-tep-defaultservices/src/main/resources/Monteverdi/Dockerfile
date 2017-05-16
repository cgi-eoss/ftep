# Builds a docker gui image
FROM hurricane/dockergui:x11rdp1.3

MAINTAINER Rakesh

#########################################
##        ENVIRONMENTAL CONFIG         ##
#########################################

# Set environment variables

# User/Group Id gui app will be executed as default are 99 and 100
ENV USER_ID=99
ENV GROUP_ID=100

# Gui App Name default is "GUI_APPLICATION"
ENV APP_NAME Monteverdi

# Default resolution, change if you like
ENV WIDTH=1280
ENV HEIGHT=720

# Use baseimage-docker's init system
CMD ["/sbin/my_init"]

#########################################
##    REPOSITORIES AND DEPENDENCIES    ##
#########################################
RUN echo 'deb http://archive.ubuntu.com/ubuntu trusty main universe restricted' > /etc/apt/sources.list
RUN echo 'deb http://archive.ubuntu.com/ubuntu trusty-updates main universe restricted' >> /etc/apt/sources.list

# Install packages needed for app

#########################################
##          GUI APP INSTALL            ##
#########################################

# Install steps for X app
RUN apt-get update && apt-get install -y\
 libx11-6\
 libxext6\
 libxau6\
 libxxf86vm1\
 libxdmcp6\
 libdrm2\
 libglu1\
 libgtk2.0-0\
 python-dev\
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Download and install OTB
ENV OTB_MAJ_VER='5'\
 OTB_MIN_VER='8'\
 OTB_POINT_VER='0'

ENV OTB_BASE_URL="https://www.orfeo-toolbox.org/packages"\
 OTB_ALT_URL="https://www.orfeo-toolbox.org/packages/archives/OTB"\
 OTB_INSTALLER="OTB-${OTB_MAJ_VER}.${OTB_MIN_VER}.${OTB_POINT_VER}-Linux64.run"

LABEL otb.version="${OTB_MAJ_VER}.${OTB_MIN_VER}.${OTB_POINT_VER}"

# OTB downloads move around, so first test the current release, then try the archive
RUN if curl -o/dev/null -sLIf "${OTB_BASE_URL}/${OTB_INSTALLER}"; then\
 curl -sL "${OTB_BASE_URL}/${OTB_INSTALLER}" >/var/tmp/${OTB_INSTALLER}; else\
 curl -sL "${OTB_ALT_URL}/${OTB_INSTALLER}" >/var/tmp/${OTB_INSTALLER}; fi

# Install OTB
RUN chmod 777 /opt && cd /opt && sudo -u nobody sh /var/tmp/${OTB_INSTALLER}

#######################
# Configure environment
RUN echo "/opt/OTB-${OTB_MAJ_VER}.${OTB_MIN_VER}.${OTB_POINT_VER}-Linux64/lib" >>/etc/ld.so.conf.d/otb.conf && ldconfig
ENV PATH=/opt/OTB-${OTB_MAJ_VER}.${OTB_MIN_VER}.${OTB_POINT_VER}-Linux64/bin:${PATH}

# Copy X app start script to right location
COPY startapp.sh /startapp.sh

#########################################
##         EXPORTS AND VOLUMES         ##
#########################################
EXPOSE 3389 8080

# Place whater volumes and ports you want exposed here:
