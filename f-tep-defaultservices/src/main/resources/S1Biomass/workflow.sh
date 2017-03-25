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

mkdir -p ${PROC_DIR}
mkdir -p ${OUT_DIR}/result

# Input params
source ${WPS_PROPS}
AOI="${aoi}"
DEM=$(ls -1 ${IN_DIR}/refDataDem/*.tif | head -1)
TARGET_RESOLUTION="${targetResolution:-10}"
EPSG="${crs}"
FOREST_MASK=$(ls -1 ${IN_DIR}/refDataForestMask/*.tif | head -1)

# Internal params
S1_PREPROCESS="${WORKFLOW}/S1_biomass_preprocess.xml"
S1_TEMPORAL_AVERAGE="${WORKFLOW}/S1_temporal_average.xml"
S1_FOREST_VOLUME="${WORKFLOW}/S1_forest_volume.xml"
S1_BIOMASS="${WORKFLOW}/S1_biomass.xml"

PREPROCESSED_PREFIX="preprocessed"
TEMPORAL_AVG_OUTPUT="${PROC_DIR}/temporal_average.tif"
REPROJECTED="${PROC_DIR}/reprojected_average.tif"
FOREST_VOLUME="${PROC_DIR}/forest_volume.tif"
OUTPUT_FILE="${OUT_DIR}/result/FTEP_S1BIOMASS_${TIMESTAMP}.tif"

# Preprocess S1 input(s)
I=0
for IN in $(ls -1d ${IN_DIR}/inputfiles/S1*.SAFE); do
    I=$((I+1))
    INPUT_FILE="${IN}/manifest.safe"
    time gpt ${S1_PREPROCESS} -Pifile="${INPUT_FILE}" -PdemFile="${DEM}" -PtargetResolution="${TARGET_RESOLUTION}" -Paoi="${AOI}" -Pofile="${PROC_DIR}/${PREPROCESSED_PREFIX}-${I}.tif"
done

# Multi-temporal averaging
if [ $I -gt 1 ]; then
    time gpt ${S1_TEMPORAL_AVERAGE} -Pofile="${TEMPORAL_AVG_OUTPUT}" ${PROC_DIR}/${PREPROCESSED_PREFIX}-*.tif
else
    mv ${PROC_DIR}/${PREPROCESSED_PREFIX}-*.tif "${TEMPORAL_AVG_OUTPUT}"
fi

## Reprojection to user-requested EPSG
time gpt Reproject -t ${REPROJECTED} -f GeoTIFF-BigTIFF -Pcrs="${EPSG}" -Presampling="Bilinear" ${TEMPORAL_AVG_OUTPUT}

# Calculate forest volume
time gpt ${S1_FOREST_VOLUME} -Pifile=${REPROJECTED} -Pofile=${FOREST_VOLUME}

# Calculate forest biomass
time gpt ${S1_BIOMASS} -Pifile=${FOREST_VOLUME} -PforestMask=${FOREST_MASK} -Pofile=${OUTPUT_FILE}
