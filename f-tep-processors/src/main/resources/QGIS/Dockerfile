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
ENV APP_NAME QGIS

# Default resolution, change if you like
ENV WIDTH=1280
ENV HEIGHT=720

# Use baseimage-docker's init system
CMD ["/sbin/my_init"]

#########################################
##    REPOSITORIES AND DEPENDENCIES    ##
#########################################
RUN echo 'deb http://qgis.org/debian trusty main' > /etc/apt/sources.list.d/qgis.list
RUN curl -sL http://qgis.org/downloads/qgis-2016.gpg.key | apt-key add -

# Install packages needed for app

#########################################
##          GUI APP INSTALL            ##
#########################################

# Install steps for X app
RUN apt-get update && apt-get install -y\
 qgis\
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy X app start script to right location
COPY startapp.sh /startapp.sh

#########################################
##         EXPORTS AND VOLUMES         ##
#########################################
EXPOSE 3389 8080
