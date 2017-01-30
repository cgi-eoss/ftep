# Builds a docker gui image
FROM hurricane/dockergui:xvnc

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
RUN apt-get update

RUN apt-get install -y libx11-6 libxext6 libxau6 libxxf86vm1 libxdmcp6 libdrm2 libglu1 libgtk2.0-0

ENV \
  TBX="OTB-5.6.0-Linux64.run" \
  OTB_URL="https://www.orfeo-toolbox.org/packages/OTB-5.6.0-Linux64.run" 

RUN wget $OTB_URL && chmod +x $TBX

RUN sh $TBX

RUN ln -s OTB-5.6.0-Linux64 otb

ENV PATH=$PATH:/otb/bin

# Copy X app start script to right location
COPY startapp.sh /startapp.sh

#########################################
##         EXPORTS AND VOLUMES         ##
#########################################
EXPOSE 3389 8080

# Place whater volumes and ports you want exposed here:
