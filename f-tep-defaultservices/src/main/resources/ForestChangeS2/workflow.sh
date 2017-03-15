#!/usr/bin/env bash

set -x -e

# F-TEP service environment
WORKFLOW=$(dirname $(readlink -f "$0"))
WORKER_DIR="/home/worker"
IN_DIR="${WORKER_DIR}/workDir/inDir"
OUT_DIR="${WORKER_DIR}/workDir/outDir"
WPS_PROPS="${WORKER_DIR}/workDir/FTEP-WPS-INPUT.properties"
PROC_DIR="${WORKER_DIR}/workDir/outDir/tmp"
TIMESTAMP=$(date --utc +%Y%m%d_%H%M%SZ)
EXTRACT_CLOUD_MASK="python ${WORKFLOW}/extractCloudMask.py"

mkdir -p ${PROC_DIR}
mkdir -p ${OUT_DIR}/result

# Input params
source ${WPS_PROPS}
EPSG="${crs}"
AOI="${aoi}"
TARGET_RESOLUTION="${targetResolution}"

# Calculated input params
SCALING_FACTOR=$(echo "scale=2;${TARGET_RESOLUTION}/10" | bc)

# Internal params
S2_PREPROCESS="${WORKFLOW}/S2_preprocess.xml"
PREPROCESSED_PREFIX="${PROC_DIR}/preprocessed"
S2_CHANGE_MAG="${WORKFLOW}/S2_changeMag.xml"
OUTPUT_FILE="${OUT_DIR}/result/FTEP_FORESTCHANGES2_${TIMESTAMP}.tif"

START_PRODUCT=$(ls -1 ${IN_DIR}/startproduct/*.xml | grep -v 'INSPIRE.xml' | head -1)
END_PRODUCT=$(ls -1 ${IN_DIR}/endproduct/*.xml | grep -v 'INSPIRE.xml' | head -1)

START_PRODUCT_CLOUD_MASK="${PROC_DIR}/start_product_cloud_mask.tif"
END_PRODUCT_CLOUD_MASK="${PROC_DIR}/end_product_cloud_mask.tif"

# Extract cloud masks from start and end products, using gdal tools
time ${EXTRACT_CLOUD_MASK} $(dirname ${START_PRODUCT})/GRANULE/*/IMG_DATA/*_B02.jp2 $(dirname ${START_PRODUCT})/GRANULE/*/QI_DATA/MSK_CLOUDS_B00.gml ${START_PRODUCT_CLOUD_MASK}
time ${EXTRACT_CLOUD_MASK} $(dirname ${END_PRODUCT})/GRANULE/*/IMG_DATA/*_B02.jp2 $(dirname ${END_PRODUCT})/GRANULE/*/QI_DATA/MSK_CLOUDS_B00.gml ${END_PRODUCT_CLOUD_MASK}

time gpt ${S2_PREPROCESS} -Pifile=${START_PRODUCT} -Pmsk=${START_PRODUCT_CLOUD_MASK} -Pofile="${PREPROCESSED_PREFIX}-start.tif"
time gpt ${S2_PREPROCESS} -Pifile=${END_PRODUCT} -Pmsk=${END_PRODUCT_CLOUD_MASK} -Pofile="${PREPROCESSED_PREFIX}-end.tif"

time gpt ${S2_CHANGE_MAG} -Pstart="${PREPROCESSED_PREFIX}-start.tif" -Pend="${PREPROCESSED_PREFIX}-end.tif" -Pofile=${OUTPUT_FILE}
