#!/usr/bin/env bash

set -x -e

# F-TEP service environment
WORKFLOW=$(dirname $(readlink -f "$0"))
WORKER_DIR="/home/worker"
IN_DIR="${WORKER_DIR}/workDir/inDir"
OUT_DIR="${WORKER_DIR}/workDir/outDir"
WPS_PROPS="${WORKER_DIR}/workDir/FTEP-WPS-INPUT.properties"
PROC_DIR="${WORKER_DIR}/procDir"
TIMESTAMP=$(date --utc +%Y%m%d_%H%M%SZ)

# Temporary file storage
mkdir -p ${PROC_DIR}

# Input parameters available as shell variables
source ${WPS_PROPS}

# Input files available under ${IN_DIR}
# Output files to be written to ${OUT_DIR}/<parameter>/<outputfilename>
