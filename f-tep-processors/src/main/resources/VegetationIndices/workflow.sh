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

mkdir -p ${PROC_DIR}

# Input params
source ${WPS_PROPS}
FORMAT_NAME="SENTINEL-2-MSI-MultiRes-UTM${utmZone}"
TARGET_RESOLUTION="${targetResolution:-10}"
VEG_INDEX="${vegIndex}"

SCALING_FACTOR=$(echo "scale=2;${TARGET_RESOLUTION}/10" | bc)

# Internal params
S2_PREPROCESS="${WORKFLOW}/S2_preprocess.xml"
S2_MOSAIC="${WORKFLOW}/S2_mosaic.xml"
PREPROCESSED_INPUT="${PROC_DIR}/preprocessed.tif"
VI_OUTPUT="${PROC_DIR}/processed.tif"
OUTPUT_FILE="${OUT_DIR}/FTEP_VEGETATION_INDICES_${VEG_INDEX}_${TIMESTAMP}.tif"

# Preprocess S2 input(s): extract correct bands and resample
# TODO Loop over IN_DIR contents
# Convert the S2 input product name to the XML metadata filename
INPUT=$(ls ${IN_DIR} | head -1)
IN_PROD=${INPUT/_MSIL1C_/_SAFL1C_}
IN_PROD=${IN_PROD/_PRD_/_MTD_}
IN_PROD=${IN_PROD/.SAFE/.xml}
INPUT_FILE="${IN_DIR}/${INPUT}/${IN_PROD}"
gpt ${S2_PREPROCESS} -Pifile=${INPUT_FILE} -PformatName=${FORMAT_NAME} -Pofile=${PREPROCESSED_INPUT}

# Preprocess S2 input(s): mosaic multiple inputs
# TODO

# Execute otb to generate radiometric index
otbcli_RadiometricIndices -in ${PREPROCESSED_INPUT} -channels.blue 1 -channels.green 2 -channels.red 3 -channels.nir 4 -list Vegetation:${VEG_INDEX} -out ${VI_OUTPUT}

# Resample & scale the product to the desired resolution, if necessary
DO_SCALE=$(echo "${SCALING_FACTOR}!=1" | bc)
if [ "${DO_SCALE}" == "1" ]; then
    otbcli_RigidTransformResample -in ${VI_OUTPUT} -out ${OUTPUT_FILE} -interpolator linear -transform.type id -transform.type.id.scalex ${SCALING_FACTOR} -transform.type.id.scaley ${SCALING_FACTOR}
else
    mv ${VI_OUTPUT} ${OUTPUT_FILE}
fi
