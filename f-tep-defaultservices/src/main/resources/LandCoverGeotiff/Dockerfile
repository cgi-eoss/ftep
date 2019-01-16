FROM ubuntu:16.04

MAINTAINER Forestry TEP

# Dependencies
RUN apt-get update && apt-get install -y\
 bc\
 curl\
 libfreetype6\
 gdal-bin\
 libgdal-dev\
 proj-bin\
 python3\
 python3-dev\
 python-gdal\
 && apt-get clean && rm -rf /var/lib/apt/lists/*

##########################
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
RUN cd /opt && sh /var/tmp/${OTB_INSTALLER}

#######################
# Configure environment
RUN echo "/opt/OTB-${OTB_MAJ_VER}.${OTB_MIN_VER}.${OTB_POINT_VER}-Linux64/lib" >>/etc/ld.so.conf.d/otb.conf && ldconfig
ENV PATH=/opt/OTB-${OTB_MAJ_VER}.${OTB_MIN_VER}.${OTB_POINT_VER}-Linux64/bin:/opt/snap/bin:${PATH}

# Prepare processor script
RUN mkdir -p /home/worker/processor
COPY * /home/worker/processor/
ENTRYPOINT ["/home/worker/processor/workflow.sh"]
