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
ENV APP_NAME SNAP5-Sentinel-Toolbox

# Default resolution, change if you like
ENV WIDTH=1280
ENV HEIGHT=720

# Use baseimage-docker's init system
CMD ["/sbin/my_init"]

#########################################
##    REPOSITORIES AND DEPENDENCIES    ##
#########################################
#RUN echo 'deb http://archive.ubuntu.com/ubuntu trusty main universe restricted' > /etc/apt/sources.list
#RUN echo 'deb http://archive.ubuntu.com/ubuntu trusty-updates main universe restricted' >> /etc/apt/sources.list

# Install packages needed for app

#########################################
##          GUI APP INSTALL            ##
#########################################

# Install steps for X app
#RUN apt-get update

#RUN apt-get install -y libx11-6 libxext6 libxau6 libxxf86vm1 libxdmcp6 libdrm2 libglu1 libgtk2.0-0

ENV \
  TBX="esa-snap_sentinel_unix_5_0.sh" \
  STB_URL="http://step.esa.int/downloads/5.0/installers" 

RUN wget $STB_URL/$TBX && chmod +x $TBX

RUN echo -e "deleteAllSnapEngineDir\$Boolean=false\ndeleteOnlySnapDesktopDir\$Boolean=false\nexecuteLauncherWithPythonAction\$Boolean=false\nforcePython\$Boolean=false\npythonExecutable=/usr/bin/python\nsys.adminRights\$Boolean=true\nsys.component.RSTB\$Boolean=true\nsys.component.S1TBX\$Boolean=true\nsys.component.S2TBX\$Boolean=true\nsys.component.S3TBX\$Boolean=true\nsys.component.SNAP\$Boolean=true\nsys.installationDir=/opt/snap\nsys.languageId=en\nsys.programGroupDisabled\$Boolean=false\nsys.symlinkDir=/usr/local/bin" >/tmp/snap.varfile

RUN ./$TBX -q -varfile /tmp/snap.varfile

ENV PATH=/opt/snap/bin:$PATH

RUN snap --nosplash --nogui --modules --update-all

# Copy X app start script to right location
COPY startapp.sh /startapp.sh

#########################################
##         EXPORTS AND VOLUMES         ##
#########################################
EXPOSE 3389 8080

# Place whater volumes and ports you want exposed here:
