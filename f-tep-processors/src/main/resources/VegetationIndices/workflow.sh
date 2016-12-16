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
EPSG2UTM="python ${WORKFLOW}/epsg2utm.py"

mkdir -p ${PROC_DIR}

# Input params
source ${WPS_PROPS}
EPSG="${epsg}"
TARGET_RESOLUTION="${targetResolution:-10}"
VEG_INDEX="${vegIndex}"

# Calculated input params
UTM_ZONE=$(${EPSG2UTM} ${EPSG})
FORMAT_NAME="SENTINEL-2-MSI-MultiRes-UTM${UTM_ZONE}"
SCALING_FACTOR=$(echo "scale=2;${TARGET_RESOLUTION}/10" | bc)

# Internal params
S2_PREPROCESS="${WORKFLOW}/S2_preprocess.xml"
S2_MOSAIC="${WORKFLOW}/S2_mosaic.xml"
PREPROCESSED_OUTPUT="${PROC_DIR}/preprocessed.tif"
VI_INPUT="${PROC_DIR}/vi_input.tif"
VI_OUTPUT="${PROC_DIR}/vi_output.tif"
OUTPUT_FILE="${OUT_DIR}/FTEP_VEGETATION_INDICES_${VEG_INDEX}_${TIMESTAMP}.tif"

# Convert the S2 input product name to the XML metadata filename
INPUT_ABS=$(ls -d ${IN_DIR}/S2*.SAFE | head -1)
INPUT=${INPUT_ABS#${IN_DIR}/}
IN_PROD=${INPUT/_MSIL1C_/_SAFL1C_}
IN_PROD=${IN_PROD/_PRD_/_MTD_}
IN_PROD=${IN_PROD/.SAFE/.xml}
INPUT_FILE="${IN_DIR}/${INPUT}/${IN_PROD}"

# Preprocess S2 input(s): extract correct bands and resample
# TODO Loop over IN_DIR contents
gpt ${S2_PREPROCESS} -Pifile=${INPUT_FILE} -PformatName=${FORMAT_NAME} -Pofile=${PREPROCESSED_OUTPUT}

# Preprocess S2 input(s): mosaic multiple inputs
# TODO
mv ${PREPROCESSED_OUTPUT} ${VI_INPUT}

# Execute otb to generate radiometric index
otbcli_RadiometricIndices -in ${VI_INPUT} -channels.blue 1 -channels.green 2 -channels.red 3 -channels.nir 4 -list Vegetation:${VEG_INDEX} -out ${VI_OUTPUT}

# Resample & scale the product to the desired resolution, if necessary
DO_SCALE=$(echo "${SCALING_FACTOR}!=1" | bc)
if [ "${DO_SCALE}" == "1" ]; then
    otbcli_RigidTransformResample -in ${VI_OUTPUT} -out ${OUTPUT_FILE} -interpolator linear -transform.type id -transform.type.id.scalex ${SCALING_FACTOR} -transform.type.id.scaley ${SCALING_FACTOR}
else
    mv ${VI_OUTPUT} ${OUTPUT_FILE}
fi
