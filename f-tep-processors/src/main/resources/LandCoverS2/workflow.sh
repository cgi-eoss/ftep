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

# Input params
source ${WPS_PROPS}
EPSG="${crs}"
AOI="${aoi}"
DEM="${dem}"
TRAINING_SHAPEFILE=$(ls -1 ${IN_DIR}/refDataShapefile/*.shp | head -1)
SHAPEFILE_ATTR="${shapefileAttribute}"
TARGET_RESOLUTION="${targetResolution}"

# Calculated input params
UTM_ZONE=$(${EPSG2UTM} ${EPSG#EPSG:})
FORMAT_NAME="SENTINEL-2-MSI-MultiRes-UTM${UTM_ZONE}"
SCALING_FACTOR=$(echo "scale=2;${TARGET_RESOLUTION}/10" | bc)

# Internal params
S2_PREPROCESS="${WORKFLOW}/S2_preprocess.xml"
S2_MOSAIC="${WORKFLOW}/S2_mosaic.xml"
PREPROCESSED_OUTPUT="${PROC_DIR}/preprocessed.tif"
MOSAIC_OUTPUT="${PROC_DIR}/mosaic.tif"
TRAINING_INPUT="${PROC_DIR}/training_input.tif"
TRAINING_OUTPUT_CLASSIFICATION_MODEL="${OUT_DIR}/FTEP_LANDCOVERS2_${TIMESTAMP}_training_model.txt"
TRAINING_OUTPUT_CONFUSION_MATRIX_CSV="${OUT_DIR}/FTEP_LANDCOVERS2_${TIMESTAMP}_confusion_matrix.csv"
OUTPUT_FILE="${OUT_DIR}/FTEP_LANDCOVERS2_${TIMESTAMP}.tif"

# Bounds of given AOI
AOI_EXTENTS=($(${POLYGON2NSEW} "${AOI}"))
NORTH_BOUND=${AOI_EXTENTS[0]}
SOUTH_BOUND=${AOI_EXTENTS[1]}
EAST_BOUND=${AOI_EXTENTS[2]}
WEST_BOUND=${AOI_EXTENTS[3]}

# Convert the S2 input product name to the XML metadata filename
INPUT_ABS=$(ls -d ${IN_DIR}/S2*.SAFE | head -1)
INPUT=${INPUT_ABS#${IN_DIR}/}
IN_PROD=${INPUT/_MSIL1C_/_SAFL1C_}
IN_PROD=${IN_PROD/_PRD_/_MTD_}
IN_PROD=${IN_PROD/.SAFE/.xml}
INPUT_FILE="${IN_DIR}/${INPUT}/${IN_PROD}"

# Preprocess S2 input(s): extract correct bands and resample
# TODO Loop over IN_DIR contents
gpt ${S2_PREPROCESS} -Pifile="${INPUT_FILE}" -PformatName="${FORMAT_NAME}" -Paoi="${AOI}" -PtargetResolution="${TARGET_RESOLUTION}" -Pofile="${PREPROCESSED_OUTPUT}"

# Preprocess S2 input(s): mosaic multiple inputs
AOI_BOUNDS_PARAMETERS="-PnorthBound=${NORTH_BOUND} -PsouthBound=${SOUTH_BOUND} -PeastBound=${EAST_BOUND} -PwestBound=${WEST_BOUND}"
gpt ${S2_MOSAIC} -t ${MOSAIC_OUTPUT} -Pepsg="${EPSG}" -Pdem="${DEM}" -PtargetResolution="${TARGET_RESOLUTION}" ${AOI_BOUNDS_PARAMETERS} ${PREPROCESSED_OUTPUT}
gpt BandSelect -t ${TRAINING_INPUT} -PsourceBands=B2,B3,B4,B8 ${MOSAIC_OUTPUT}

# OTB training with "random forest" model + reference data
otbcli_TrainImagesClassifier \
 -io.il ${TRAINING_INPUT} -io.vd ${TRAINING_SHAPEFILE} \
 -sample.mv -1 -sample.mt -1 -sample.vtr 0.5 -sample.edg false -sample.vfn ${SHAPEFILE_ATTR} \
 -classifier rf -classifier.rf.max 5 -classifier.rf.min 10 -classifier.rf.var 0 -classifier.rf.nbtrees 100 \
 -io.out ${TRAINING_OUTPUT_CLASSIFICATION_MODEL} -io.confmatout ${TRAINING_OUTPUT_CONFUSION_MATRIX_CSV}

# Final calculation using trained model
otbcli_ImageClassifier \
 -in ${TRAINING_INPUT} \
 -model ${TRAINING_OUTPUT_CLASSIFICATION_MODEL} \
 -out ${OUTPUT_FILE}
