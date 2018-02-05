FROM ubuntu:16.04

MAINTAINER Forestry TEP

# Dependencies
RUN apt-get update && apt-get install -y\
 bc\
 curl\
 libfreetype6\
 python\
 python-dev\
 && apt-get clean && rm -rf /var/lib/apt/lists/*

###########################
# Download and install SNAP
ENV SNAP_MAJ_VER='5'\
 SNAP_MIN_VER='0'\
 SNAP_POINT_VER='0'

ENV SNAP_BASE_URL="http://step.esa.int/downloads/${SNAP_MAJ_VER}.${SNAP_MIN_VER}/installers"\
 SNAP_INSTALLER="esa-snap_sentinel_unix_${SNAP_MAJ_VER}_${SNAP_MIN_VER}.sh"

LABEL snap.version="${SNAP_MAJ_VER}.${SNAP_MIN_VER}.${SNAP_POINT_VER}"

RUN curl -sL "${SNAP_BASE_URL}/${SNAP_INSTALLER}" >/var/tmp/${SNAP_INSTALLER}

# Install SNAP
RUN echo -e "deleteAllSnapEngineDir$Boolean=false\ndeleteOnlySnapDesktopDir$Boolean=false\nexecuteLauncherWithPythonAction$Boolean=false\nforcePython$Boolean=false\npythonExecutable=/usr/bin/python\nsys.adminRights$Boolean=true\nsys.component.RSTB$Boolean=false\nsys.component.S1TBX$Boolean=true\nsys.component.S2TBX$Boolean=true\nsys.component.S3TBX$Boolean=false\nsys.component.SNAP$Boolean=false\nsys.installationDir=/opt/snap\nsys.languageId=en\nsys.programGroupDisabled$Boolean=false\nsys.symlinkDir=/usr/local/bin" >/var/tmp/SNAP_install.varfile
RUN sh /var/tmp/${SNAP_INSTALLER} -q -varfile /var/tmp/SNAP_install.varfile

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
