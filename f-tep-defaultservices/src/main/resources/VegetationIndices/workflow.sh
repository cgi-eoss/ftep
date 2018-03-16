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
POLYGON2NSEW="python ${WORKFLOW}/polygon2nsewBounds.py"
S2PRODUCTZONES="python ${WORKFLOW}/s2ProductZones.py"

mkdir -p ${PROC_DIR}
mkdir -p ${OUT_DIR}/result
mkdir -p "${PROC_DIR}/preprocessed"

# Input params
source ${WPS_PROPS}
EPSG="${crs}"
AOI="${aoi}"
TARGET_RESOLUTION="${targetResolution:-10}"
VEG_INDEX="${vegIndex}"

# Calculated input params
SCALING_FACTOR=$(echo "scale=2;${TARGET_RESOLUTION}/10" | bc)

# Internal params
S2_PREPROCESS="${WORKFLOW}/F-TEP_S2_preprocessNew.xml"
PREPROCESSED_PREFIX="${PROC_DIR}/preprocessed"
MOSAIC_OUTPUT="${PREPROCESSED_PREFIX}/mosaic.tif"
VI_INPUT="${PREPROCESSED_PREFIX}/vi_input.tif"
VI_OUTPUT="${OUT_DIR}/result/FTEP_VEGETATION_INDICES_${VEG_INDEX}_${TIMESTAMP}.tif"

# Bounds of given AOI
if [ "" != "${AOI}" ]; then
    AOI_EXTENTS=($(${POLYGON2NSEW} "${AOI}"))
    NORTH_BOUND=${AOI_EXTENTS[0]}
    SOUTH_BOUND=${AOI_EXTENTS[1]}
    EAST_BOUND=${AOI_EXTENTS[2]}
    WEST_BOUND=${AOI_EXTENTS[3]}
fi
UL=($(echo "${WEST_BOUND} ${NORTH_BOUND}" | cs2cs +init=epsg:4326 +to +init=epsg:${EPSG#EPSG:}))
LR=($(echo "${EAST_BOUND} ${SOUTH_BOUND}" | cs2cs +init=epsg:4326 +to +init=epsg:${EPSG#EPSG:}))
EMIN=${UL[0]}
NMAX=${UL[1]}
EMAX=${LR[0]}
NMIN=${LR[1]}

# Preprocess S2 input: extract correct bands and resample
I=0
for IN in ${IN_DIR}/inputfile/*; do
    I=$((I+1))
    INPUT_FILE=$(ls -1 ${IN}/*.xml | grep -v 'INSPIRE.xml' | head -1)
    time gpt ${S2_PREPROCESS} -Pifile=${INPUT_FILE} -Paoi="${AOI}" -PtargetResolution="${TARGET_RESOLUTION}" -Pofile="${PREPROCESSED_PREFIX}-${I}.tif"
    time gdalwarp -t_srs ${EPSG} -te $EMIN $NMIN $EMAX $NMAX -tr $TARGET_RESOLUTION $TARGET_RESOLUTION -tap -ot Int16 -dstnodata "0 0 0 0" -r near -of GTiff -overwrite ${PREPROCESSED_PREFIX}-${I}.tif ${PREPROCESSED_PREFIX}-${I}r.tif
done
if [ 1 == ${I} ]; then
    mv ${PREPROCESSED_PREFIX}-${I}r.tif ${VI_INPUT}

# Execute otb to generate radiometric index
    time otbcli_RadiometricIndices -in ${VI_INPUT} -channels.blue 1 -channels.green 2 -channels.red 3 -channels.nir 4 -list "Vegetation:${VEG_INDEX}" -out ${VI_OUTPUT}
else
    echo "Single tile products only, now: ", $I
fi
exit 0