#!/usr/bin/env sh

WORKFLOW=$(dirname $(readlink -f "$0"))
WORKER_DIR="/home/worker"
IN_DIR="${WORKER_DIR}/workDir/inDir"
OUT_DIR="${WORKER_DIR}/workDir/outDir"
WPS_PROPS="${WORKER_DIR}/workDir/FTEP-WPS-INPUT.properties"

#create a file to procDir (should be visible on docker host sudo find / -name temp_file_1)
PROC_DIR="${WORKER_DIR}/procDir"
echo "lorem ipsum" >${PROC_DIR}/temp_file_1
chmod 777 ${PROC_DIR}/temp_file_1

source ${WPS_PROPS}

mkdir -p ${OUT_DIR}/output
chmod 777 ${OUT_DIR}/output

echo "INPUT PARAM: ${inputKey1}" >${OUT_DIR}/output/output_file_1
chmod 666 ${OUT_DIR}/output/output_file_1

#to inspect what is happening
#tail -f /dev/null