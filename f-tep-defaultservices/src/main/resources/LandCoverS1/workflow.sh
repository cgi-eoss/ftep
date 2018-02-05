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
mkdir -p ${OUT_DIR}/{result,model,confusionMatrix}

# Input params
source ${WPS_PROPS}
AOI="${aoi}"
DEM="${dem}"
TARGET_RESOLUTION="${targetResolution:-10}"
EPSG="${crs}"
TRAINING_SHAPEFILE=$(ls -1 ${IN_DIR}/refDataShapefile/*.shp | head -1)
SHAPEFILE_ATTR="${shapefileAttribute}"

# Internal params
S1_PREPROCESS="${WORKFLOW}/S1_preprocess.xml"
S1_TEMPORAL_AVERAGE="${WORKFLOW}/S1_temporal_average.xml"
PREPROCESSED_PREFIX="preprocessed"
TEMPORAL_AVG_OUTPUT="${PROC_DIR}/stack.tif"
TRAINING_INPUT="${PROC_DIR}/training_input.tif"

TRAINING_OUTPUT_CLASSIFICATION_MODEL="${OUT_DIR}/model/FTEP_LANDCOVERS1_${TIMESTAMP}_training_model.txt"
TRAINING_OUTPUT_CONFUSION_MATRIX_CSV="${OUT_DIR}/confusionMatrix/FTEP_LANDCOVERS1_${TIMESTAMP}_confusion_matrix.csv"
OUTPUT_FILE="${OUT_DIR}/result/FTEP_LANDCOVERS1_${TIMESTAMP}.tif"

# Preprocess S1 input(s)
I=0
for IN in $(ls -1d ${IN_DIR}/inputfiles/S1*); do
    I=$((I+1))
    INPUT_FILE="${IN}/manifest.safe"
    time gpt ${S1_PREPROCESS} -Pifile="${INPUT_FILE}" -Pdem="${DEM}" -PtargetResolution="${TARGET_RESOLUTION}" -Paoi="${AOI}" -Pofile="${PROC_DIR}/${PREPROCESSED_PREFIX}-${I}.tif"
done

# Multi-temporal averaging
if [ $I -gt 1 ]; then
    time gpt ${S1_TEMPORAL_AVERAGE} -Pofile="${TEMPORAL_AVG_OUTPUT}" ${PROC_DIR}/${PREPROCESSED_PREFIX}-*.tif
else
    mv ${PROC_DIR}/${PREPROCESSED_PREFIX}-*.tif "${TEMPORAL_AVG_OUTPUT}"
fi

# Reprojection to user-requested EPSG
time gpt Reproject -t ${TRAINING_INPUT} -f GeoTIFF-BigTIFF -Pcrs="${EPSG}" -Presampling="Bilinear" ${TEMPORAL_AVG_OUTPUT}

# OTB training with "random forest" model + reference data
time otbcli_TrainImagesClassifier \
 -io.il ${TRAINING_INPUT} -io.vd ${TRAINING_SHAPEFILE} \
 -sample.mv -1 -sample.mt -1 -sample.vtr 0.5 -sample.edg false -sample.vfn ${SHAPEFILE_ATTR} \
 -classifier rf -classifier.rf.max 5 -classifier.rf.min 10 -classifier.rf.var 0 -classifier.rf.nbtrees 100 \
 -io.out ${TRAINING_OUTPUT_CLASSIFICATION_MODEL} -io.confmatout ${TRAINING_OUTPUT_CONFUSION_MATRIX_CSV}

# Final calculation using trained model
time otbcli_ImageClassifier \
 -in ${TRAINING_INPUT} \
 -model ${TRAINING_OUTPUT_CLASSIFICATION_MODEL} \
 -out ${OUTPUT_FILE}
