FROM jlesage/baseimage-gui:ubuntu-18.04	

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

COPY startapp.sh /startapp.sh

ENV APP_NAME="QGIS"