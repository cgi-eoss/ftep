FROM ubuntu:16.04

MAINTAINER Forestry TEP

# Dependencies
RUN apt-get update && apt-get install -y\
 bc\
 curl\
 libfreetype6\
 python3\
 python3-dev\
 software-properties-common
RUN python3 /usr/bin/add-apt-repository -y ppa:ubuntugis/ppa
RUN apt-get update && apt-get install -y\
 gdal-bin\
 libgdal-dev\
 proj-bin\
 && apt-get clean && rm -rf /var/lib/apt/lists/*
 
###########################
# Download and install SNAP
ENV SNAP_MAJ_VER='6'\
 SNAP_MIN_VER='0'\
 SNAP_POINT_VER='0'

ENV SNAP_BASE_URL="http://step.esa.int/downloads/${SNAP_MAJ_VER}.${SNAP_MIN_VER}/installers"\
 SNAP_INSTALLER="esa-snap_sentinel_unix_${SNAP_MAJ_VER}_${SNAP_MIN_VER}.sh"

LABEL snap.version="${SNAP_MAJ_VER}.${SNAP_MIN_VER}.${SNAP_POINT_VER}"

RUN curl -sL "${SNAP_BASE_URL}/${SNAP_INSTALLER}" >/var/tmp/${SNAP_INSTALLER}

# Install SNAP
RUN echo -e "deleteAllSnapEngineDir$Boolean=false\ndeleteOnlySnapDesktopDir$Boolean=false\nexecuteLauncherWithPythonAction$Boolean=false\nforcePython$Boolean=false\npythonExecutable=/usr/bin/python\nsys.adminRights$Boolean=true\nsys.component.RSTB$Boolean=false\nsys.component.S1TBX$Boolean=true\nsys.component.S2TBX$Boolean=true\nsys.component.S3TBX$Boolean=false\nsys.component.SNAP$Boolean=false\nsys.installationDir=/opt/snap\nsys.languageId=en\nsys.programGroupDisabled$Boolean=false\nsys.symlinkDir=/usr/local/bin" >/var/tmp/SNAP_install.varfile
RUN sh /var/tmp/${SNAP_INSTALLER} -q -varfile /var/tmp/SNAP_install.varfile
ENV PATH=/opt/snap/bin:${PATH}

# Prepare processor script
RUN mkdir -p /home/worker/processor
COPY * /home/worker/processor/
ENTRYPOINT ["/home/worker/processor/workflow.sh"]

RUN echo "-Xmx10240m\n-Xms256m" >>/opt/snap/bin/gpt.vmoptions
