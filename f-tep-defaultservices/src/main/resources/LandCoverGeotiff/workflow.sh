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
mkdir -p ${OUT_DIR}/{result,model,confusionMatrix}

# Input params
source ${WPS_PROPS}
TRAINING_SHAPEFILE=$(ls -1 ${IN_DIR}/refDataShapefile/*/*.shp | head -1)
ls -o "${TRAINING_SHAPEFILE}"
SHAPEFILE_ATTR="${shapefileAttribute}"

# Internal params
PREPROCESSED_PREFIX="${PROC_DIR}/preprocessed"
MOSAIC_OUTPUT="${PROC_DIR}/mosaic.tif"
TRAINING_INPUT="${PROC_DIR}/training_input.tif"
TRAINING_OUTPUT_CLASSIFICATION_MODEL="${OUT_DIR}/model/FTEP_LANDCOVERS2_${TIMESTAMP}_training_model.txt"
TRAINING_OUTPUT_CONFUSION_MATRIX_CSV="${OUT_DIR}/confusionMatrix/FTEP_LANDCOVERS2_${TIMESTAMP}_confusion_matrix.csv"
OUTPUT_FILE="${OUT_DIR}/result/FTEP_LANDCOVERS2_${TIMESTAMP}.tif"

TRAINING_INPUT2="$(readlink -f ${IN_DIR}/inputmosaic)"
TRAINING_INPUT3="$(ls -1 ${TRAINING_INPUT2})"
TRAINING_INPUT4="$(readlink -f ${TRAINING_INPUT2}/${TRAINING_INPUT3})"
TRAINING_LEAVE="$(ls -1 ${TRAINING_INPUT4})"
TRAINING_INPUT="${TRAINING_INPUT4}/${TRAINING_LEAVE}"

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
exit 0
