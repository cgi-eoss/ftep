FROM jlesage/baseimage-gui:ubuntu-18.04

ENV APP_NAME="QGIS"
ENV DISPLAY_WIDTH=1920
ENV DISPLAY_HEIGHT=1080

# Set up QGIS repository
RUN apt-get update && apt-get install -y\
 curl\
 software-properties-common\
 && curl -sL https://qgis.org/downloads/qgis-2017.gpg.key | apt-key add -\
 && add-apt-repository 'https://qgis.org/ubuntu'\
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install qgis and supporting packages
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y\
 qgis\
 python-qgis\
 qgis-plugin-grass\
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Disable QGIS settings import prompt
RUN mkdir -p /config/xdg/data/QGIS/QGIS3/profiles/default/QGIS \
 && echo -e "[migration]\nfirstRunVersionFlag=99999" >>/config/xdg/data/QGIS/QGIS3/profiles/default/QGIS/QGIS3.ini

COPY startapp.sh /startapp.sh
